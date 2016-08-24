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

import org.terracotta.management.model.cluster.Cluster;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.message.DefaultMessage;
import org.terracotta.management.model.message.Message;
import org.terracotta.management.model.notification.ContextualNotification;
import org.terracotta.management.sequence.BoundaryFlakeSequence;
import org.terracotta.management.sequence.SequenceGenerator;
import org.terracotta.management.service.monitoring.IMonitoringConsumer;
import org.terracotta.management.service.monitoring.Mutation;
import org.terracotta.management.service.monitoring.ReadOnlyBuffer;
import org.terracotta.management.entity.tms.TmsAgent;
import org.terracotta.management.entity.tms.TmsAgentConfig;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Consumes:
 * <ul>
 * <li>{@code platform/servers/<id> PlatformServer}</li>
 * <li>{@code platform/servers/<id>/state ServerState}</li>
 * <li>{@code platform/clients/<id> PlatformConnectedClient}</li>
 * <li>{@code platform/fetched/<id> PlatformClientFetchedEntity}</li>
 * <li>{@code platform/entities/<id> PlatformEntity}</li>
 * <li>{@code management/statistics BlockingQueue<[byte[] sequence, ContextualStatistics[]]>}</li>
 * <li>{@code management/notifications BlockingQueue<[byte[] sequence, ContextualNotification]>}</li>
 * <li>{@code management/clients/<client-identifier>/tags String[]}</li>
 * <li>{@code management/clients/<client-identifier>/registry}</li>
 * <li>{@code management/clients/<client-identifier>/registry/contextContainer ContextContainer}</li>
 * <li>{@code management/clients/<client-identifier>/registry/capabilities Capability[]}</li>
 * </ul>
 *
 * @author Mathieu Carbou
 */
class TmsAgentImpl implements TmsAgent {

  private static final Collection<String> IGNORED = new HashSet<>(Arrays.asList(
      PlatformNotificationType.CONNECTION_CLOSED.name(),
      PlatformNotificationType.CONNECTION_OPENED.name(),
      PlatformNotificationType.CLIENT_TAGS_UPDATED.name(),
      PlatformNotificationType.CLIENT_CONTEXT_CONTAINER_UPDATED.name(),
      PlatformNotificationType.CLIENT_CAPABILITIES_UPDATED.name()
  ));

  private final IMonitoringConsumer consumer;
  private final String stripeName;
  private final TopologyBuilder topologyBuilder;
  private final SequenceGenerator sequenceGenerator;
  private final ReadOnlyBuffer<Serializable[]> notificationBuffer;
  private final ReadOnlyBuffer<Serializable[]> statisticBuffer;
  private final ReadOnlyBuffer<Mutation> mutationBuffer;

  private long expectedNextIndex = Long.MIN_VALUE;

  TmsAgentImpl(TmsAgentConfig config, IMonitoringConsumer consumer, SequenceGenerator sequenceGenerator) {
    this.sequenceGenerator = Objects.requireNonNull(sequenceGenerator, "SequenceGenerator service is missing");
    this.consumer = Objects.requireNonNull(consumer, "IMonitoringConsumer service is missing");
    this.stripeName = config.getStripeName() != null ? config.getStripeName() : "stripe-1";
    this.topologyBuilder = new TopologyBuilder(consumer, stripeName);
    this.notificationBuffer = consumer.getOrCreateBestEffortBuffer("client-notifications", config.getMaximumUnreadNotifications(), Serializable[].class);
    this.statisticBuffer = consumer.getOrCreateBestEffortBuffer("client-statistics", config.getMaximumUnreadStatistics(), Serializable[].class);
    this.mutationBuffer = consumer.getOrCreateMutationBuffer(config.getMaximumUnreadMutations());
  }

  @Override
  public Future<Cluster> readTopology() {
    return CompletableFuture.completedFuture(topologyBuilder.buildTopology());
  }

  @Override
  public synchronized Future<List<Message>> readMessages() {
    List<Message> messages = new ArrayList<>();

    // 1st: read mutations on the voltron tree to build notifications about topology changes
    readTopologyMutations().forEach(messages::add);

    // 2nd: read notifications coming client-side if any
    notificationBuffer.stream()
        .map(bucket -> new DefaultMessage(BoundaryFlakeSequence.fromBytes((byte[]) bucket[0]), "NOTIFICATION", bucket[1]))
        .forEach(messages::add);

    // 3rd: read stats coming client-side if any
    statisticBuffer.stream()
        .map(bucket -> new DefaultMessage(BoundaryFlakeSequence.fromBytes((byte[]) bucket[0]), "STATISTICS", bucket[1]))
        .forEach(messages::add);

    return CompletableFuture.completedFuture(messages);
  }

  private Stream<Message> readTopologyMutations() {
    List<Notification> notifications = mutationBuffer.stream()
        .map(Notification::new)
        .collect(Collectors.toList());

    // no unread mutations => no topology changes
    if (notifications.isEmpty()) {
      return Stream.empty();
    }

    // reset the sequence numbers for next read
    long nextSequence = notifications.get(0).getIndex();
    long expectedNextIndex = this.expectedNextIndex == Long.MIN_VALUE ? nextSequence : this.expectedNextIndex;
    this.expectedNextIndex = notifications.get(notifications.size() - 1).getIndex() + 1;

    // if this is not the first time we read AND the next expected sequence number is not the one next
    // it means that the queue was full at one point and some notifications were discarded
    // so just ignore them and send a fresh new topology
    if (expectedNextIndex != nextSequence) {
      Cluster cluster = topologyBuilder.buildTopology();
      return Stream.of(
          clusterMessage(cluster),
          lostNotificationMessage(cluster.getStripe(stripeName).get().getContext())
      );
    }

    // checks whether the mutations on the tree are just about branch mutations
    notifications.removeIf(mutation -> !mutation.isValueChanged());
    if (notifications.isEmpty()) {
      return Stream.empty();
    }

    // check whether the notifications we have are ONLY mutations that change the topology
    // but are not relevant to send back to the tms.
    notifications.removeIf(mutation -> mutation.getPlatformNotificationType() == PlatformNotificationType.OTHER);
    if (notifications.isEmpty()) {
      return Stream.of(clusterMessage(topologyBuilder.buildTopology()));
    }

    // here, we only have notifications that changed the topology AND must be sent back to tms
    Cluster cluster = topologyBuilder.buildTopology();

    MutationReducer mutationReducer = new MutationReducer(consumer, cluster, notifications);
    mutationReducer.reduce();

    return Stream.concat(
        Stream.of(clusterMessage(cluster)),
        mutationReducer.stream()
            // we only keep "CLIENT_DISCONNECTED" and "CLIENT_CONNECTED" events
            .filter(notification -> !IGNORED.contains(notification.getType()))
            .map(Notification::toMessage)
    );
  }

  private Message clusterMessage(Cluster cluster) {
    return new DefaultMessage(sequenceGenerator.next(), "TOPOLOGY", cluster);
  }

  private Message lostNotificationMessage(Context context) {
    return new DefaultMessage(sequenceGenerator.next(), "NOTIFICATION", new ContextualNotification(context, "LOST_NOTIFICATIONS"));
  }

}
