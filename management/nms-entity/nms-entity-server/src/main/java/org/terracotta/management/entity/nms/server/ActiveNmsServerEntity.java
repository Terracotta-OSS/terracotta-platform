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
package org.terracotta.management.entity.nms.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.IEntityMessenger;
import org.terracotta.entity.StateDumpCollector;
import org.terracotta.management.entity.nms.Nms;
import org.terracotta.management.entity.nms.NmsConfig;
import org.terracotta.management.model.call.ContextualCall;
import org.terracotta.management.model.call.ContextualReturn;
import org.terracotta.management.model.call.Parameter;
import org.terracotta.management.model.cluster.Client;
import org.terracotta.management.model.cluster.Cluster;
import org.terracotta.management.model.cluster.Connection;
import org.terracotta.management.model.cluster.Server;
import org.terracotta.management.model.cluster.Stripe;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.context.Contextual;
import org.terracotta.management.model.message.Message;
import org.terracotta.management.service.monitoring.EntityManagementRegistry;
import org.terracotta.management.service.monitoring.ManagementExecutor;
import org.terracotta.management.service.monitoring.ManagementService;
import org.terracotta.management.service.monitoring.SharedEntityManagementRegistry;
import org.terracotta.voltron.proxy.ClientId;
import org.terracotta.voltron.proxy.server.ActiveProxiedServerEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Mathieu Carbou
 */
class ActiveNmsServerEntity extends ActiveProxiedServerEntity<Void, Void, NmsCallback> implements Nms, NmsCallback, ManagementExecutor {

  private static final Comparator<ToSend> MESSAGE_COMPARATOR = Comparator.comparing(toSend -> toSend.message.getSequence());

  private static final Logger LOGGER = LoggerFactory.getLogger(ActiveNmsServerEntity.class);

  private final ManagementService managementService;
  private final String stripeName;
  private final EntityManagementRegistry entityManagementRegistry;
  private final SharedEntityManagementRegistry sharedEntityManagementRegistry;
  private final long consumerId;
  // NmsCallback.entityCallbackToSendMessagesToClients() is scheduled to be executed periodically after 500ms.
  //TODO: load test to verify if this queue can leak, otherwise replace by a ring buffer (see git revision fa80424cc4b56826fa0ff184beb605bf1c39ffa4 to have one) 
  private final BlockingQueue<ToSend> messagesToBeSent = new LinkedBlockingQueue<>();

  ActiveNmsServerEntity(NmsConfig config, ManagementService managementService, EntityManagementRegistry entityManagementRegistry, SharedEntityManagementRegistry sharedEntityManagementRegistry) {
    this.entityManagementRegistry = Objects.requireNonNull(entityManagementRegistry);
    this.sharedEntityManagementRegistry = Objects.requireNonNull(sharedEntityManagementRegistry);
    this.managementService = Objects.requireNonNull(managementService);
    this.stripeName = config.getStripeName();
    this.consumerId = entityManagementRegistry.getMonitoringService().getConsumerId();
  }

  // ActiveProxiedServerEntity

  @Override
  public void destroy() {
    entityManagementRegistry.close();
    managementService.close();
    super.destroy();
  }

  @Override
  public void createNew() {
    super.createNew();
    LOGGER.trace("[{}] createNew()", consumerId);
    entityManagementRegistry.refresh();
    // start scheduling of thsi call
    getMessenger().entityCallbackToSendMessagesToClients();
  }

  @Override
  public void loadExisting() {
    super.loadExisting();
    LOGGER.trace("[{}] loadExisting()", consumerId);
    entityManagementRegistry.refresh();
    // start scheduling of this call
    getMessenger().entityCallbackToSendMessagesToClients();
  }

  @Override
  protected void dumpState(StateDumpCollector dump) {
    dump.addState("consumerId", String.valueOf(consumerId));
    dump.addState("stripeName", String.valueOf(stripeName));
    dump.addState("messageQueueSize", String.valueOf(messagesToBeSent.size()));
  }

  // NmsCallback

  @Override
  public void entityCallbackToExecuteManagementCall(String managementCallIdentifier, ContextualCall<?> call) {
    String serverName = call.getContext().get(Server.NAME_KEY);
    if (serverName == null) {
      throw new IllegalArgumentException("Bad context: " + call.getContext());
    }
    if (entityManagementRegistry.getMonitoringService().getServerName().equals(serverName)) {
      LOGGER.trace("[{}] entityCallbackToExecuteManagementCall({}, {}, {}, {})", consumerId, managementCallIdentifier, call.getContext(), call.getCapability(), call.getMethodName());
      ContextualReturn<?> contextualReturn = sharedEntityManagementRegistry.withCapability(call.getCapability())
          .call(call.getMethodName(), call.getReturnType(), call.getParameters())
          .on(call.getContext())
          .build()
          .execute()
          .getSingleResult();
      entityManagementRegistry.getMonitoringService().answerManagementCall(managementCallIdentifier, contextualReturn);
    }
  }

  @Override
  public IEntityMessenger.ScheduledToken entityCallbackToSendMessagesToClients() {
    List<ToSend> toSends = new ArrayList<>(messagesToBeSent.size());
    messagesToBeSent.drainTo(toSends);
    int size = toSends.size();
    if (size > 0) {
      LOGGER.trace("[{}] entityCallbackToSendMessagesToClients({})", consumerId, size);
      if (size > 1) {
        toSends.sort(MESSAGE_COMPARATOR);
      }
      Collection<ClientDescriptor> clients = getClients();
      for (ToSend toSend : toSends) {
        toSend.message.unwrap(Contextual.class)
            .stream()
            .filter(contextual -> !contextual.getContext().contains(Client.KEY))
            .forEach(contextual -> contextual.setContext(contextual.getContext().with(Stripe.KEY, stripeName)));
        try {
          if (toSend.to == null) {
            fireMessage(Message.class, toSend.message, false);
          } else if (clients.contains(toSend.to)) {
            fireMessage(Message.class, toSend.message, toSend.to);
          }
        } catch (Exception e) {
          LOGGER.warn("Unable to send message " + toSend.message + " : " + e.getMessage(), e);
        }
      }
    }
    return null;
  }

  @Override
  public void unSchedule() {
    throw new UnsupportedOperationException();
  }

  // Nms

  @Override
  public Future<Cluster> readTopology() {
    return CompletableFuture.completedFuture(readCluster());
  }

  @Override
  public Future<String> call(@ClientId Object callerDescriptor, Context context, String capabilityName, String methodName, Class<?> returnType, Parameter... parameters) {
    if (context.contains(Stripe.KEY)) {
      context = context.with(Stripe.KEY, "SINGLE");
    }
    return CompletableFuture.completedFuture(managementService.sendManagementCallRequest((ClientDescriptor) callerDescriptor, context, capabilityName, methodName, returnType, parameters));
  }

  private Cluster readCluster() {
    Cluster cluster = managementService.readTopology();
    // if we want a specific name for our stripe, just rename it
    if (!stripeName.equals(cluster.getSingleStripe().getName())) {

      Stripe namedStripe = Stripe.create(stripeName);
      Stripe currentStripe = cluster.getSingleStripe();

      // move servers
      currentStripe.serverStream().forEach(namedStripe::addServer);

      // add stripe
      cluster.addStripe(namedStripe);

      cluster.clientStream().forEach(client -> {
        // hole a list of connections to delete after
        List<Connection> toDelete = new ArrayList<>(client.getConnectionCount());
        List<Connection> toAdd = new ArrayList<>(client.getConnectionCount());

        client.connectionStream().forEach(currentConn -> {
          toDelete.add(currentConn);
          namedStripe.getServer(currentConn.getServerId())
              .ifPresent(server -> {
                Connection newConnection = Connection.create(currentConn.getLogicalConnectionUid(), server, currentConn.getClientEndpoint());
                currentConn.fetchedServerEntityStream().forEach(serverEntity -> newConnection.fetchServerEntity(serverEntity.getServerEntityIdentifier()));
                toAdd.add(newConnection);
              });
        });

        toDelete.forEach(Connection::remove);
        toAdd.forEach(client::addConnection);
      });

      // remove current stripe and add new one
      cluster.removeStripe(currentStripe.getId());
    }
    return cluster;
  }

  // ManagementExecutor

  @Override
  public void executeManagementCallOnServer(String managementCallIdentifier, ContextualCall<?> call) {
    LOGGER.trace("[{}] executeManagementCallOnServer({}, {})", consumerId, managementCallIdentifier, call);
    getMessenger().entityCallbackToExecuteManagementCall(managementCallIdentifier, call);
  }

  @Override
  public void sendMessageToClients(Message message) {
    LOGGER.trace("[{}] sendMessageToClients({}, {})", consumerId, message);
    messagesToBeSent.offer(new ToSend(message));
  }

  @Override
  public void sendMessageToClient(Message message, ClientDescriptor to) {
    if (getClients().contains(to)) {
      LOGGER.trace("[{}] sendMessageToClient({}, {})", consumerId, message, to);
      messagesToBeSent.offer(new ToSend(message, to));
    }
  }

  static private class ToSend {
    final Message message;
    final ClientDescriptor to;

    ToSend(Message message) {
      this(message, null);
    }

    ToSend(Message message, ClientDescriptor to) {
      this.message = message;
      this.to = to;
    }
  }
}
