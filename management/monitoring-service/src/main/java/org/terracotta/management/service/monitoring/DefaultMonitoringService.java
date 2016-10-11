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

import org.terracotta.entity.ClientCommunicator;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.management.model.call.ContextualCall;
import org.terracotta.management.model.call.ContextualReturn;
import org.terracotta.management.model.call.Parameter;
import org.terracotta.management.model.capabilities.Capability;
import org.terracotta.management.model.cluster.Client;
import org.terracotta.management.model.cluster.ClientIdentifier;
import org.terracotta.management.model.cluster.Cluster;
import org.terracotta.management.model.cluster.ManagementRegistry;
import org.terracotta.management.model.cluster.ServerEntity;
import org.terracotta.management.model.cluster.ServerEntityIdentifier;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.context.ContextContainer;
import org.terracotta.management.model.message.DefaultManagementCallMessage;
import org.terracotta.management.model.message.DefaultMessage;
import org.terracotta.management.model.message.ManagementCallMessage;
import org.terracotta.management.model.message.Message;
import org.terracotta.management.model.notification.ContextualNotification;
import org.terracotta.management.model.stats.ContextualStatistics;
import org.terracotta.management.sequence.Sequence;
import org.terracotta.management.service.monitoring.buffer.ReadOnlyBuffer;
import org.terracotta.management.service.monitoring.buffer.ReadWriteBuffer;
import org.terracotta.management.service.monitoring.buffer.RingBuffer;
import org.terracotta.voltron.proxy.server.messages.ProxyEntityResponse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A monitoring service is created per entity, and contains some states related to the entity that requested this service
 *
 * @author Mathieu Carbou
 */
class DefaultMonitoringService implements MonitoringService, Closeable {

  private final DefaultStripeMonitoring stripeMonitoring;
  private final long consumerId;
  private final ClientCommunicator clientCommunicator;
  private final Map<ClientDescriptor, ClientIdentifier> fetches = new ConcurrentHashMap<>();
  private final ConcurrentMap<ClientDescriptor, AtomicLong> pendingCalls = new ConcurrentHashMap<>();

  private volatile ReadWriteBuffer<Message> buffer;

  DefaultMonitoringService(DefaultStripeMonitoring stripeMonitoring, long consumerId, ClientCommunicator clientCommunicator) {
    this.stripeMonitoring = stripeMonitoring;
    this.consumerId = consumerId;
    this.clientCommunicator = clientCommunicator;
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
    return getClass().getSimpleName() + "{" + "\ncontext=" + getServerEntityContext() + ", \nfetches=" + fetches + ", \ncalls=" + pendingCalls + '}';
  }

  @Override
  public ClientIdentifier getClientIdentifier(ClientDescriptor clientDescriptor) {
    return getConnectedClientIdentifier(clientDescriptor);
  }

  @Override
  public void pushClientNotification(ClientDescriptor from, ContextualNotification notification) {
    ClientIdentifier clientIdentifier = getConnectedClientIdentifier(from);
    stripeMonitoring.consumeCluster(cluster -> cluster.getClient(clientIdentifier)
        .ifPresent(client -> {
          notification.setContext(notification.getContext().with(client.getContext()));
          stripeMonitoring.fireNotification(notification);
        }));
  }

  @Override
  public void pushClientStatistics(ClientDescriptor from, ContextualStatistics... statistics) {
    ClientIdentifier clientIdentifier = getConnectedClientIdentifier(from);
    stripeMonitoring.consumeCluster(cluster -> cluster.getClient(clientIdentifier)
        .ifPresent(client -> {
          Context context = client.getContext();
          for (ContextualStatistics statistic : statistics) {
            statistic.setContext(statistic.getContext().with(context));
          }
          stripeMonitoring.fireStatistics(statistics);
        }));
  }

  @Override
  public void exposeClientTags(ClientDescriptor from, String... tags) {
    ClientIdentifier clientIdentifier = getConnectedClientIdentifier(from);
    stripeMonitoring.consumeCluster(cluster -> cluster.getClient(clientIdentifier)
        .ifPresent(client -> {
          client.addTags(tags);
          stripeMonitoring.fireNotification(new ContextualNotification(client.getContext(), "CLIENT_TAGS_UPDATED"));
        }));
  }

  @Override
  public void exposeClientManagementRegistry(ClientDescriptor caller, ContextContainer contextContainer, Capability... capabilities) {
    ClientIdentifier clientIdentifier = getConnectedClientIdentifier(caller);
    stripeMonitoring.consumeCluster(cluster -> cluster.getClient(clientIdentifier)
        .ifPresent(client -> {
          ManagementRegistry registry = ManagementRegistry.create(contextContainer);
          registry.addCapabilities(capabilities);
          client.setManagementRegistry(registry);
          stripeMonitoring.fireNotification(new ContextualNotification(client.getContext(), "CLIENT_REGISTRY_UPDATED"));
        }));
  }

  @Override
  public void exposeServerEntityManagementRegistry(ContextContainer contextContainer, Capability... capabilities) {
    ManagementRegistry registry = ManagementRegistry.create(contextContainer);
    registry.addCapabilities(capabilities);
    stripeMonitoring.consumeCluster(cluster -> {
      ServerEntityIdentifier serverEntityIdentifier = getServerEntityIdentifier();
      ServerEntity serverEntity = cluster.getSingleStripe().getActiveServerEntity(serverEntityIdentifier)
          .<IllegalStateException>orElseThrow(() -> stripeMonitoring.newIllegalTopologyState("Missing entity: " + serverEntityIdentifier));
      serverEntity.setManagementRegistry(registry);
      stripeMonitoring.fireNotification(new ContextualNotification(serverEntity.getContext(), "ENTITY_REGISTRY_UPDATED"));
    });
  }

  @Override
  public void pushServerEntityNotification(ContextualNotification notification) {
    notification.setContext(notification.getContext().with(getServerEntityContext()));
    stripeMonitoring.fireNotification(notification);
  }

  @Override
  public void pushServerEntityStatistics(ContextualStatistics... statistics) {
    Context context = getServerEntityContext();
    for (ContextualStatistics statistic : statistics) {
      statistic.setContext(statistic.getContext().with(context));
    }
    stripeMonitoring.fireStatistics(statistics);
  }

  @Override
  public Cluster readTopology() {
    return stripeMonitoring.applyCluster(o -> {
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
  public ServerEntityIdentifier getServerEntityIdentifier() {
    return stripeMonitoring.getServerEntityIdentifier(consumerId);
  }

  @Override
  public String sendManagementCallRequest(ClientDescriptor caller, ClientIdentifier to, Context context, String capabilityName, String methodName, Class<?> returnType, Parameter... parameters) {
    if (clientCommunicator == null) {
      throw new IllegalStateException("No " + ClientCommunicator.class.getSimpleName());
    }

    ClientIdentifier callerClientIdentifier = getConnectedClientIdentifier(caller);
    ClientDescriptor toClientDescriptor = getClientDescriptor(to);

    Client targetClient = stripeMonitoring.applyCluster(cluster -> cluster.getClient(to)
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
    pendingCalls.computeIfAbsent(caller, descriptor -> new AtomicLong()).incrementAndGet();

    try {
      clientCommunicator.sendNoResponse(toClientDescriptor, ProxyEntityResponse.response(ManagementCallMessage.class, message));
    } catch (MessageCodecException e) {
      throw new RuntimeException(e);
    }

    return id;
  }

  @Override
  public void answerManagementCall(ClientDescriptor calledDescriptor, ClientIdentifier caller, String managementCallIdentifier, ContextualReturn<?> contextualReturn) {
    ClientIdentifier calledClientIdentifier = getConnectedClientIdentifier(calledDescriptor);
    ClientDescriptor callerClientDescriptor = getClientDescriptor(caller);

    Client targettedClient = stripeMonitoring.applyCluster(cluster -> cluster.getClient(caller).<IllegalStateException>orElseThrow(() -> new IllegalStateException(caller.toString())));

    contextualReturn.setContext(contextualReturn.getContext().with(targettedClient.getContext()));

    DefaultManagementCallMessage message = new DefaultManagementCallMessage(
        calledClientIdentifier,
        managementCallIdentifier,
        nextSequence(),
        "MANAGEMENT_CALL_RETURN",
        contextualReturn);

    if (Optional.ofNullable(pendingCalls.get(callerClientDescriptor))
        .<SecurityException>orElseThrow(() -> new SecurityException("Client " + caller + " did not ask for a management call"))
        .getAndUpdate(current -> Math.max(0, current - 1)) <= 0) {
      throw new SecurityException("Client " + caller + " did not ask for a management call");
    }

    try {
      clientCommunicator.sendNoResponse(callerClientDescriptor, ProxyEntityResponse.response(ManagementCallMessage.class, message));
    } catch (MessageCodecException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public synchronized ReadOnlyBuffer<Message> createMessageBuffer(int maxBufferSize) {
    if (buffer == null) {
      buffer = new RingBuffer<>(maxBufferSize);
    }
    return buffer;
  }

  @Override
  public Sequence nextSequence() {
    return stripeMonitoring.nextSequence();
  }

  void push(Message message) {
    if (buffer != null) {
      if (buffer.put(message) != null) {
        // notify the loss of messages if the ring buffer is full
        buffer.put(new DefaultMessage(nextSequence(), "NOTIFICATION", new ContextualNotification(getServerEntityContext(), "LOST_NOTIFICATIONS")));
      }
    }
  }

  void addFetch(ClientDescriptor clientDescriptor, ClientIdentifier clientIdentifier) {
    fetches.put(clientDescriptor, clientIdentifier);
  }

  void removeFetch(ClientDescriptor clientDescriptor, ClientIdentifier clientIdentifier) {
    fetches.remove(clientDescriptor, clientIdentifier);
  }

  private Context getServerEntityContext() {
    return stripeMonitoring.applyCluster(cluster -> {
      ServerEntityIdentifier serverEntityIdentifier = stripeMonitoring.getServerEntityIdentifier(consumerId);
      return cluster.getSingleStripe()
          .getActiveServerEntity(serverEntityIdentifier)
          .<IllegalStateException>orElseThrow(() -> stripeMonitoring.newIllegalTopologyState("Missing entity: " + serverEntityIdentifier))
          .getContext();
    });
  }

  private ClientIdentifier getConnectedClientIdentifier(ClientDescriptor from) {
    ClientIdentifier clientIdentifier = fetches.get(from);
    if (clientIdentifier == null) {
      throw new SecurityException("Descriptor " + from + " is not a client of entity " + getServerEntityIdentifier() + " (consumerId=" + consumerId + ")");
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
      throw new SecurityException("Client identifier " + to + " is not a client of entity " + getServerEntityIdentifier() + " (consumerId=" + consumerId + ")");
    }
    return toClientDescriptor;
  }

}
