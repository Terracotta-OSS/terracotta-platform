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
package org.terracotta.management.service.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.management.model.call.ContextualCall;
import org.terracotta.management.model.call.ContextualReturn;
import org.terracotta.management.model.call.Parameter;
import org.terracotta.management.model.capabilities.Capability;
import org.terracotta.management.model.cluster.Client;
import org.terracotta.management.model.cluster.ClientIdentifier;
import org.terracotta.management.model.cluster.Cluster;
import org.terracotta.management.model.cluster.ManagementRegistry;
import org.terracotta.management.model.cluster.ServerEntityIdentifier;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.context.ContextContainer;
import org.terracotta.management.model.message.DefaultManagementCallMessage;
import org.terracotta.management.model.message.DefaultMessage;
import org.terracotta.management.model.message.Message;
import org.terracotta.management.model.notification.ContextualNotification;
import org.terracotta.management.model.stats.ContextualStatistics;
import org.terracotta.management.sequence.Sequence;
import org.terracotta.monitoring.IMonitoringProducer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * A monitoring service is created per entity, and contains some states related to the entity that requested this service
 *
 * @author Mathieu Carbou
 */
class DefaultMonitoringService implements MonitoringService, Closeable {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultMonitoringService.class);

  private final DefaultListener stripeMonitoring;
  private final long consumerId;
  private final IMonitoringProducer monitoringProducer;
  private final ManagementCommunicator managementCommunicator;
  private final Map<ClientDescriptor, ClientIdentifier> fetches = new ConcurrentHashMap<>();
  private final ConcurrentMap<ClientDescriptor, Set<String>> pendingCalls = new ConcurrentHashMap<>();

  private volatile ReadWriteBuffer<Message> buffer;

  DefaultMonitoringService(DefaultListener stripeMonitoring, long consumerId, IMonitoringProducer monitoringProducer, ManagementCommunicator managementCommunicator) {
    this.stripeMonitoring = stripeMonitoring;
    this.consumerId = consumerId;
    this.monitoringProducer = monitoringProducer;
    this.managementCommunicator = managementCommunicator;
  }

  @Override
  public void close() {
    fetches.clear();
    pendingCalls.clear();
    if (buffer != null) {
      buffer.clear();
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" + "\nconsumerId=" + consumerId + ", \nfetches=" + fetches + ", \ncalls=" + pendingCalls + '}';
  }

  @Override
  public long getConsumerId() {
    return consumerId;
  }

  // ===================
  // ACTIVE-ONLY methods
  // ===================

  @Override
  public ClientIdentifier getClientIdentifier(ClientDescriptor clientDescriptor) {
    LOGGER.trace("[{}] getClientIdentifier({})", consumerId, clientDescriptor);

    ensureAliveOnActive();
    return getConnectedClientIdentifier(clientDescriptor);
  }

  @Override
  public void pushClientNotification(ClientDescriptor from, ContextualNotification notification) {
    LOGGER.trace("[{}] pushClientNotification({}, {})", consumerId, from, notification);

    ensureAliveOnActive();
    ClientIdentifier clientIdentifier = getConnectedClientIdentifier(from);
    stripeMonitoring.consumeCluster(cluster -> {
      Client client = cluster.getClient(clientIdentifier).get();
      notification.setContext(notification.getContext().with(client.getContext()));
      stripeMonitoring.fireNotification(notification);
    });
  }

  @Override
  public void pushClientStatistics(ClientDescriptor from, ContextualStatistics... statistics) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("[{}] pushClientStatistics({}, {})", consumerId, from, Arrays.toString(statistics));
    }

    ensureAliveOnActive();
    ClientIdentifier clientIdentifier = getConnectedClientIdentifier(from);
    stripeMonitoring.consumeCluster(cluster -> {
      Client client = cluster.getClient(clientIdentifier).get();
      Context context = client.getContext();
      for (ContextualStatistics statistic : statistics) {
        statistic.setContext(statistic.getContext().with(context));
      }
      stripeMonitoring.fireStatistics(statistics);
    });
  }

  @Override
  public void exposeClientTags(ClientDescriptor from, String... tags) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("[{}] exposeClientTags({}, {})", consumerId, from, Arrays.toString(tags));
    }

    ensureAliveOnActive();
    ClientIdentifier clientIdentifier = getConnectedClientIdentifier(from);
    stripeMonitoring.mutateCluster(cluster -> {
      Client client = cluster.getClient(clientIdentifier).get();
      client.addTags(tags);
      stripeMonitoring.fireNotification(new ContextualNotification(client.getContext(), "CLIENT_TAGS_UPDATED"));
    });
  }

  @Override
  public void exposeClientManagementRegistry(ClientDescriptor from, ContextContainer contextContainer, Capability... capabilities) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("[{}] exposeClientManagementRegistry({}, {}, {})", consumerId, from, contextContainer, Arrays.toString(capabilities));
    }

    ensureAliveOnActive();
    ClientIdentifier clientIdentifier = getConnectedClientIdentifier(from);
    stripeMonitoring.mutateCluster(cluster -> {
      Client client = cluster.getClient(clientIdentifier).get();
      ManagementRegistry newRegistry = ManagementRegistry.create(contextContainer);
      newRegistry.addCapabilities(capabilities);
      String notif = client.getManagementRegistry().map(current -> current.equals(newRegistry) ? "" : "CLIENT_REGISTRY_UPDATED").orElse("CLIENT_REGISTRY_AVAILABLE");
      if (!notif.isEmpty()) {
        client.setManagementRegistry(newRegistry);
        stripeMonitoring.fireNotification(new ContextualNotification(client.getContext(), notif));
      }
    });
  }

  @Override
  public Cluster readTopology() {
    ensureAliveOnActive();
    return stripeMonitoring.consumeClusterFn(o -> {
      try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
          oos.writeObject(o);
          oos.flush();
        }
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
          return (Cluster) ois.readObject();
        }
      } catch (IOException | ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Override
  public Optional<ServerEntityIdentifier> getServerEntityIdentifier(Context context) throws NoSuchElementException {
    LOGGER.trace("[{}] getServerEntityIdentifier({})", consumerId, context);
    return stripeMonitoring.getServerEntityIdentifier(context);
  }

  @Override
  public String getCurrentServerName() {
    return stripeMonitoring.getCurrentServerName();
  }

  @Override
  public String sendManagementCallRequest(ClientDescriptor from, Context context, String capabilityName, String methodName, Class<?> returnType, Parameter... parameters) {
    LOGGER.trace("[{}] sendManagementCallRequest({}, {}, {}, {})", consumerId, from, context, capabilityName, methodName);

    ensureAliveOnActive();

    if (managementCommunicator == null) {
      throw new IllegalStateException("No " + ManagementCommunicator.class.getSimpleName());
    }

    if (!context.contains(Client.KEY)) {
      throw new IllegalArgumentException("Bad context, missing " + Client.KEY + ": " + context);
    }

    ClientIdentifier to = ClientIdentifier.valueOf(context.get(Client.KEY));
    ClientIdentifier callerClientIdentifier = getConnectedClientIdentifier(from);
    ClientDescriptor toClientDescriptor = getClientDescriptor(to);

    Client targetClient = stripeMonitoring.consumeClusterFn(cluster -> cluster.getClient(to)
        .<IllegalStateException>orElseThrow(() -> new IllegalStateException(to.toString())));

    if (!targetClient.isManageable()) {
      throw new SecurityException("Client " + to + " is not manageable");
    }

    String id = UUID.randomUUID().toString();

    DefaultManagementCallMessage message = new DefaultManagementCallMessage(
        callerClientIdentifier,
        id,
        nextSequence(),
        "MANAGEMENT_CALL",
        new ContextualCall(context.with(targetClient.getContext()), capabilityName, methodName, returnType, parameters));

    // atomically increase the counter of management calls made by this client
    Set<String> ids = pendingCalls.get(from);
    if (ids == null) {
      throw new SecurityException("Client " + from + " cannot send management calls");
    }
    ids.add(id);

    managementCommunicator.send(toClientDescriptor, message);

    return id;
  }

  @Override
  public void answerManagementCall(ClientDescriptor from, ClientIdentifier caller, String managementCallIdentifier, ContextualReturn<?> contextualReturn) {
    LOGGER.trace("[{}] answerManagementCall({}, {}, {})", consumerId, from, caller, managementCallIdentifier);

    ensureAliveOnActive();

    ClientIdentifier calledClientIdentifier = getConnectedClientIdentifier(from);
    ClientDescriptor callerClientDescriptor = getClientDescriptor(caller);

    Client targettedClient = stripeMonitoring.consumeClusterFn(cluster -> cluster.getClient(caller).<IllegalStateException>orElseThrow(() -> new IllegalStateException(caller.toString())));

    contextualReturn.setContext(contextualReturn.getContext().with(targettedClient.getContext()));

    DefaultManagementCallMessage message = new DefaultManagementCallMessage(
        calledClientIdentifier,
        managementCallIdentifier,
        nextSequence(),
        "MANAGEMENT_CALL_RETURN",
        contextualReturn);

    Set<String> ids = pendingCalls.get(callerClientDescriptor);
    if (ids == null) {
      throw new SecurityException("Client " + callerClientDescriptor + " cannot answer management calls");
    }

    if (!ids.remove(managementCallIdentifier)) {
      throw new SecurityException("Client " + caller + " did not ask for management call " + managementCallIdentifier);
    }

    managementCommunicator.send(callerClientDescriptor, message);
  }

  @Override
  public Sequence nextSequence() {
    ensureAliveOnActive();

    return stripeMonitoring.nextSequence();
  }

  private ClientIdentifier getConnectedClientIdentifier(ClientDescriptor from) {
    ClientIdentifier clientIdentifier = fetches.get(from);
    if (clientIdentifier == null) {
      throw new SecurityException("Descriptor " + from + " is not a client of entity " + stripeMonitoring.getCurrentActiveServerEntity(consumerId).getServerEntityIdentifier() + " (consumerId={})");
    }
    return clientIdentifier;
  }

  private ClientDescriptor getClientDescriptor(ClientIdentifier to) {
    ClientDescriptor toClientDescriptor = null;
    for (Map.Entry<ClientDescriptor, ClientIdentifier> entry : fetches.entrySet()) {
      if (entry.getValue().equals(to)) {
        toClientDescriptor = entry.getKey();
        break;
      }
    }
    if (toClientDescriptor == null) {
      throw new SecurityException("Client identifier " + to + " is not a client of entity " + stripeMonitoring.getCurrentActiveServerEntity(consumerId).getServerEntityIdentifier() + " (consumerId={})");
    }
    return toClientDescriptor;
  }

  private void ensureAliveOnActive() {
    // this call throws an exception if the current server is not active and does not have the active entity with this consumer id
    stripeMonitoring.getCurrentActiveServerEntity(consumerId);
  }

  // ==========================
  // ACTIVE and PASSIVE methods
  // ==========================

  @Override
  public void exposeServerEntityManagementRegistry(ContextContainer contextContainer, Capability... capabilities) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("[{}] exposeServerEntityManagementRegistry({}, {})", consumerId, contextContainer, Arrays.toString(capabilities));
    }

    ManagementRegistry registry = ManagementRegistry.create(contextContainer);
    registry.addCapabilities(capabilities);

    // this call will be routed to the current active server by voltron
    monitoringProducer.addNode(new String[0], "registry", registry);
  }

  @Override
  public void pushServerEntityNotification(ContextualNotification notification) {
    LOGGER.trace("[{}] pushServerEntityNotification({})", consumerId, notification);

    // this call will be routed to the current active server by voltron
    monitoringProducer.pushBestEffortsData(DefaultListener.TOPIC_SERVER_ENTITY_NOTIFICATION, notification);
  }

  @Override
  public void pushServerEntityStatistics(ContextualStatistics... statistics) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("[{}] pushServerEntityStatistics({})", consumerId, Arrays.toString(statistics));
    }

    // this call will be routed to the current active server by voltron
    monitoringProducer.pushBestEffortsData(DefaultListener.TOPIC_SERVER_ENTITY_STATISTICS, statistics);
  }

  @Override
  public synchronized ReadOnlyBuffer<Message> createMessageBuffer(int maxBufferSize) {
    LOGGER.trace("[{}] createMessageBuffer({})", consumerId, maxBufferSize);

    if (buffer == null) {
      buffer = new RingBuffer<>(maxBufferSize);
    }
    return buffer;
  }

  // =============================================
  // CALLED IN ACTIVE FROM DefaultListener
  // =============================================

  void push(Message message) {
    if (buffer != null) {
      LOGGER.trace("[{}] push({})", consumerId, message);
      if (buffer.put(message) != null) {
        // notify the loss of messages if the ring buffer is full
        buffer.put(new DefaultMessage(
            nextSequence(),
            "NOTIFICATION",
            new ContextualNotification(stripeMonitoring.getCurrentActiveServerEntity(consumerId).getContext(), "LOST_MESSAGES")));
      }
    }
  }

  void addFetch(ClientDescriptor clientDescriptor, ClientIdentifier clientIdentifier) {
    fetches.put(clientDescriptor, clientIdentifier);
    pendingCalls.put(clientDescriptor, new ConcurrentSkipListSet<>());
  }

  void removeFetch(ClientDescriptor clientDescriptor, ClientIdentifier clientIdentifier) {
    fetches.remove(clientDescriptor, clientIdentifier);
    pendingCalls.remove(clientDescriptor);
  }

}
