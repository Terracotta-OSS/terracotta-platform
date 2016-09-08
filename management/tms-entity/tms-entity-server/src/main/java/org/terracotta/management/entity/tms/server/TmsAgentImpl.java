/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.management.entity.tms.server;

import org.terracotta.management.entity.tms.TmsAgent;
import org.terracotta.management.entity.tms.TmsAgentConfig;
import org.terracotta.management.model.cluster.Client;
import org.terracotta.management.model.cluster.Cluster;
import org.terracotta.management.model.cluster.Server;
import org.terracotta.management.model.cluster.ServerEntity;
import org.terracotta.management.model.cluster.Stripe;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.message.DefaultMessage;
import org.terracotta.management.model.message.Message;
import org.terracotta.management.model.notification.ContextualNotification;
import org.terracotta.management.sequence.SequenceGenerator;
import org.terracotta.management.service.monitoring.IMonitoringConsumer;
import org.terracotta.management.service.monitoring.PlatformNotification;
import org.terracotta.management.service.monitoring.ReadOnlyBuffer;
import org.terracotta.monitoring.PlatformConnectedClient;
import org.terracotta.monitoring.PlatformEntity;
import org.terracotta.monitoring.PlatformServer;
import org.terracotta.monitoring.ServerState;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.terracotta.management.entity.tms.server.Utils.toClientIdentifier;

/**
 * Consumes:
 * <ul>
 * <li>{@code management/clients/<client-identifier>/tags String[]}</li>
 * <li>{@code management/clients/<client-identifier>/registry}</li>
 * <li>{@code management/clients/<client-identifier>/registry/contextContainer ContextContainer}</li>
 * <li>{@code management/clients/<client-identifier>/registry/capabilities Capability[]}</li>
 * </ul>
 * Buffers:
 * <ul>
 * <li>{@code client-statistics [byte[] sequence, ContextualStatistics[]]>}</li>
 * <li>{@code client-notifications [byte[] sequence, ContextualNotification]>}</li>
 * </ul>
 *
 * @author Mathieu Carbou
 */
class TmsAgentImpl implements TmsAgent {

  private static final Comparator<Message> MESSAGE_COMPARATOR = (o1, o2) -> o1.getSequence().compareTo(o2.getSequence());

  private final TopologyBuilder topologyBuilder;
  private final SequenceGenerator sequenceGenerator;
  private final ReadOnlyBuffer<Message> entityNotifications;
  private final ReadOnlyBuffer<Message> clientNotifications;
  private final ReadOnlyBuffer<Message> clientStatistics;
  private final ReadOnlyBuffer<PlatformNotification> platformNotifications;

  private long nextExpectedIndex = Long.MIN_VALUE;

  TmsAgentImpl(TmsAgentConfig config, IMonitoringConsumer consumer, SequenceGenerator sequenceGenerator) {
    this.sequenceGenerator = Objects.requireNonNull(sequenceGenerator, "SequenceGenerator service is missing");
    String stripeName = config.getStripeName() != null ? config.getStripeName() : "stripe-1";
    this.topologyBuilder = new TopologyBuilder(consumer, stripeName);
    this.entityNotifications = consumer.getOrCreateBestEffortBuffer("entity-notifications", config.getMaximumUnreadNotifications(), Message.class);
    this.clientNotifications = consumer.getOrCreateBestEffortBuffer("client-notifications", config.getMaximumUnreadNotifications(), Message.class);
    this.clientStatistics = consumer.getOrCreateBestEffortBuffer("client-statistics", config.getMaximumUnreadStatistics(), Message.class);
    this.platformNotifications = consumer.getOrCreatePlatformNotificationBuffer(config.getMaximumUnreadMutations());
  }

  @Override
  public Future<Cluster> readTopology() {
    return CompletableFuture.completedFuture(topologyBuilder.buildTopology());
  }

  @Override
  public synchronized Future<List<Message>> readMessages() {
    Cluster cluster = topologyBuilder.buildTopology();
    Stripe stripe = cluster.getStripes().values().iterator().next();

    // read entity and client notifications, plus stats
    List<Message> messages = Stream.concat(
        // While streaming the server-side messages, ensure to fix the context.
        // Messages coming from voltron only have a consumerId.
        // So this transform helps setting a better context from the current topology if we find the element into the current topo.
        entityNotifications.stream().peek(message -> {
          if ("NOTIFICATION".equals(message.getType())) {
            ContextualNotification notification = message.unwrap(ContextualNotification.class);
            stripe.getServerEntity(notification.getContext())
                .map(ServerEntity::getContext)
                .ifPresent(notification::setContext);
          }
        }),
        Stream.concat(
            clientNotifications.stream(),
            clientStatistics.stream()))
        .collect(Collectors.toList());

    // reads platform notifications
    try {
      Collection<Notification> platformNotifications = getPlatformNotifications(cluster);
      platformNotifications.stream().map(Notification::toMessage).forEach(messages::add);
    } catch (BadIndexException e) {
      messages.add(new DefaultMessage(sequenceGenerator.next(), "NOTIFICATION", new ContextualNotification(stripe.getContext(), "LOST_NOTIFICATIONS")));
    }

    // in any case, whether there is at least one message, we add the cluster topology
    if (!messages.isEmpty()) {
      messages.add(new DefaultMessage(sequenceGenerator.next(), "TOPOLOGY", cluster));
    }

    messages.sort(MESSAGE_COMPARATOR);

    return CompletableFuture.completedFuture(messages);
  }

  private Collection<Notification> getPlatformNotifications(Cluster cluster) throws BadIndexException {
    List<Notification> notifications = platformNotifications.stream()
        .map(Notification::new)
        .collect(Collectors.toList());

    // no unread mutations => no topology changes
    if (notifications.isEmpty()) {
      return Collections.emptyList();
    }

    // reset the sequence numbers for next read
    long currentIndex = notifications.get(0).getIndex();
    long expectedIndex = this.nextExpectedIndex == Long.MIN_VALUE ? currentIndex : this.nextExpectedIndex;
    this.nextExpectedIndex = notifications.get(notifications.size() - 1).getIndex() + 1;

    // if this is not the first time we read AND the next expected sequence number is not the one next
    // it means that the queue was full at one point and some notifications were discarded
    // so just ignore them and send a fresh new topology
    if (expectedIndex != currentIndex) {
      throw new BadIndexException();
    }

    Stripe stripe = cluster.getStripes().values().iterator().next();
    Server active = stripe.getActiveServer().orElseThrow(() -> new IllegalStateException("Unable to find active server on stripe " + stripe.getName()));

    // parse the notifs
    for (Notification notification : notifications) {
      switch (notification.getType()) {

        case SERVER_ENTITY_CREATED:
        case SERVER_ENTITY_DESTROYED: {
          PlatformEntity platformEntity = notification.getSource(PlatformEntity.class);
          notification.setContext(active.getContext()
              .with(ServerEntity.create(platformEntity.name, platformEntity.typeName, platformEntity.consumerID).getContext()));
          break;
        }

        case SERVER_JOINED:
        case SERVER_LEFT: {
          PlatformServer server = notification.getSource(PlatformServer.class);
          notification.setContext(stripe.getContext()
              .with(Server.KEY, server.getServerName())
              .with(Server.NAME_KEY, server.getServerName()));
          break;
        }

        case SERVER_STATE_CHANGED: {
          Serializable[] bucket = notification.getSource(Serializable[].class);
          PlatformServer platformServer = (PlatformServer) bucket[0];
          ServerState serverState = (ServerState) bucket[1];
          notification.setContext(stripe.getContext()
              .with(Server.KEY, platformServer.getServerName())
              .with(Server.NAME_KEY, platformServer.getServerName()));
          notification.setAttribute("state", serverState.getState());
          notification.setAttribute("activateTime", serverState.getActivate() > 0 ? String.valueOf(serverState.getActivate()) : "0");
          break;
        }

        case SERVER_ENTITY_FETCHED:
        case SERVER_ENTITY_UNFETCHED: {
          Serializable[] bucket = notification.getSource(Serializable[].class);
          PlatformConnectedClient platformClient = (PlatformConnectedClient) bucket[0];
          PlatformEntity platformEntity = (PlatformEntity) bucket[1];
          Context context = active.getContext()
              .with(ServerEntity.create(platformEntity.name, platformEntity.typeName, platformEntity.consumerID).getContext());
          notification.setContext(context);
          notification.setAttribute(Client.KEY, toClientIdentifier(platformClient).getClientId());
          break;
        }

        case CLIENT_CONNECTED:
        case CLIENT_DISCONNECTED: {
          PlatformConnectedClient platformClient = notification.getSource(PlatformConnectedClient.class);
          Context context = active.getContext()
              .with(Client.KEY, toClientIdentifier(platformClient).getClientId());
          notification.setContext(context);
          break;
        }

        default:
          throw new AssertionError(notification.getType());
      }
    }

    return notifications;
  }

  private static final class BadIndexException extends Exception {
    private static final long serialVersionUID = -7243509059801791979L;
  }

}
