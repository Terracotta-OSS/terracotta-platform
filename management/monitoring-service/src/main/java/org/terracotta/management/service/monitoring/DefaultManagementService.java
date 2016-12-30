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
import org.terracotta.entity.ClientCommunicator;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.management.model.call.ContextualCall;
import org.terracotta.management.model.call.Parameter;
import org.terracotta.management.model.cluster.Client;
import org.terracotta.management.model.cluster.ClientIdentifier;
import org.terracotta.management.model.cluster.Cluster;
import org.terracotta.management.model.cluster.Server;
import org.terracotta.management.model.cluster.ServerEntity;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.message.DefaultMessage;
import org.terracotta.management.model.message.ManagementCallMessage;
import org.terracotta.management.model.message.Message;
import org.terracotta.management.model.notification.ContextualNotification;
import org.terracotta.management.sequence.Sequence;
import org.terracotta.management.sequence.SequenceGenerator;
import org.terracotta.voltron.proxy.MessageType;
import org.terracotta.voltron.proxy.ProxyEntityResponse;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * @author Mathieu Carbou
 */
class DefaultManagementService implements ManagementService, TopologyEventListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultManagementService.class);

  private final long consumerId;
  private final FiringService firingService;
  private final ClientCommunicator clientCommunicator;
  private final SequenceGenerator sequenceGenerator;
  private final ManagementCallExecutor managementCallExecutor;
  private final TopologyService topologyService;
  private final Map<ClientDescriptor, Collection<String>> managementCallRequests = new ConcurrentHashMap<>();

  private volatile ReadWriteBuffer<Message> buffer;
  private volatile ContextualNotification full;

  DefaultManagementService(long consumerId, TopologyService topologyService, FiringService firingService, ClientCommunicator clientCommunicator, SequenceGenerator sequenceGenerator, ManagementCallExecutor managementCallExecutor) {
    this.consumerId = consumerId;
    this.topologyService = Objects.requireNonNull(topologyService);
    this.firingService = Objects.requireNonNull(firingService);
    this.clientCommunicator = Objects.requireNonNull(clientCommunicator);
    this.sequenceGenerator = Objects.requireNonNull(sequenceGenerator);
    this.managementCallExecutor = Objects.requireNonNull(managementCallExecutor);
  }

  @Override
  public Cluster readTopology() {
    LOGGER.trace("[{}] readTopology()", consumerId);
    return topologyService.getClusterCopy();
  }

  @Override
  public synchronized ReadOnlyBuffer<Message> createMessageBuffer(int maxBufferSize) {
    LOGGER.trace("[{}] createMessageBuffer({})", consumerId, maxBufferSize);
    if (buffer == null) {
      buffer = new RingBuffer<>(maxBufferSize);
    }
    return buffer;
  }

  @Override
  public Sequence nextSequence() {
    return sequenceGenerator.next();
  }

  @Override
  public String sendManagementCallRequest(ClientDescriptor caller, final Context context, String capabilityName, String methodName, Class<?> returnType, Parameter... parameters) {
    LOGGER.trace("[{}] sendManagementCallRequest({}, {}, {})", consumerId, context, capabilityName, methodName);

    String managementCallIdentifier = UUID.randomUUID().toString();
    Context fullContext = null;

    if (context.contains(Client.KEY)) {
      // handle client call
      ClientIdentifier to = ClientIdentifier.valueOf(context.get(Client.KEY));
      fullContext = context.with(topologyService.getManageableClientContext(to).orElseThrow(() -> new IllegalStateException("Client " + to + " is either not found or not manageable")));
    }

    if ((context.contains(Server.NAME_KEY) || context.contains(Server.KEY))
        && (context.contains(ServerEntity.CONSUMER_ID) || context.contains(ServerEntity.TYPE_KEY) && context.contains(ServerEntity.NAME_KEY))) {
      // handle entity call
      String serverName = context.getOrDefault(Server.NAME_KEY, context.get(Server.KEY));
      Context entityCtx = (context.contains(ServerEntity.CONSUMER_ID) ?
          topologyService.getManageableEntityContext(serverName, Long.parseLong(context.get(ServerEntity.CONSUMER_ID))) :
          topologyService.getManageableEntityContext(serverName, context.get(ServerEntity.NAME_KEY), context.get(ServerEntity.TYPE_KEY)))
          .orElseThrow(() -> new IllegalStateException("Server Entity " + context + " is either not found or not manageable"));
      fullContext = context.with(entityCtx);
    }

    if (fullContext == null) {
      throw new IllegalArgumentException(context.toString());
    }

    track(caller, managementCallIdentifier);
    firingService.fireManagementCallRequest(managementCallIdentifier, new ContextualCall<>(fullContext, capabilityName, methodName, returnType, parameters));
    return managementCallIdentifier;
  }

  @Override
  public void onBecomeActive() {
    LOGGER.trace("[{}] onBecomeActive()", this.consumerId);
    clear();
  }

  @Override
  public void onFetch(long consumerId, ClientDescriptor clientDescriptor) {
  }

  @Override
  public void onUnfetch(long consumerId, ClientDescriptor clientDescriptor) {
    if (consumerId == this.consumerId) {
      LOGGER.trace("[{}] onUnfetch({})", this.consumerId, clientDescriptor);
      managementCallRequests.remove(clientDescriptor);
    }
  }

  @Override
  public void onEntityDestroyed(long consumerId) {
    if (consumerId == this.consumerId) {
      LOGGER.trace("[{}] onEntityDestroyed()", this.consumerId);
      clear();
    }
  }

  @Override
  public void onEntityCreated(long consumerId) {
  }

  void fireMessage(Message message) {
    switch (message.getType()) {

      //TODO: send notifications directly to TMS https://github.com/Terracotta-OSS/terracotta-platform/issues/195
      case "NOTIFICATION":
      case "STATISTICS":
        push(message);
        break;

      case "MANAGEMENT_CALL":
        ManagementCallMessage managementCallMessage = (ManagementCallMessage) message;
        String managementCallIdentifier = managementCallMessage.getManagementCallIdentifier();
        if (isTracked(managementCallIdentifier)) {
          ContextualCall call = managementCallMessage.unwrap(ContextualCall.class).get(0);
          managementCallExecutor.executeManagementCall(managementCallIdentifier, call);
        }
        break;

      case "MANAGEMENT_CALL_RETURN":
        ManagementCallMessage managementCallResultMessage = (ManagementCallMessage) message;
        unTrack(managementCallResultMessage.getManagementCallIdentifier())
            .ifPresent(clientDescriptor -> send(clientDescriptor, message));
        break;

      default:
        throw new UnsupportedOperationException(message.getType());
    }
  }

  private void send(ClientDescriptor client, Message message) {
    LOGGER.trace("[{}] send({}, {})", consumerId, client, message);
    try {
      clientCommunicator.sendNoResponse(client, ProxyEntityResponse.messageResponse(Message.class, message));
    } catch (Exception e) {
      LOGGER.error("Unable to send message " + message + " to client " + client);
    }
  }

  private void push(Message message) {
    ReadWriteBuffer<Message> buffer = this.buffer;
    if (buffer != null) {
      LOGGER.trace("[{}] push({})", consumerId, message);
      if (buffer.put(message) != null) {
        // notify the loss of messages if the ring buffer is full
        if (full == null) {
          full = new ContextualNotification(topologyService.getEntityContext(topologyService.getCurrentServerName(), consumerId).get(), "LOST_MESSAGES");
        }
        buffer.put(new DefaultMessage(nextSequence(), "NOTIFICATION", full));
      }
    }
  }

  private void track(ClientDescriptor caller, String managementCallIdentifier) {
    managementCallRequests
        .computeIfAbsent(caller, clientDescriptor -> new ConcurrentSkipListSet<>())
        .add(managementCallIdentifier);
  }

  private Optional<ClientDescriptor> unTrack(String managementCallIdentifier) {
    for (Map.Entry<ClientDescriptor, Collection<String>> entry : managementCallRequests.entrySet()) {
      if (entry.getValue().remove(managementCallIdentifier)) {
        return Optional.of(entry.getKey());
      }
    }
    return Optional.empty();
  }

  private boolean isTracked(String managementCallIdentifier) {
    for (Map.Entry<ClientDescriptor, Collection<String>> entry : managementCallRequests.entrySet()) {
      if (entry.getValue().contains(managementCallIdentifier)) {
        return true;
      }
    }
    return false;
  }

  private void clear() {
    managementCallRequests.clear();
    full = null;
    if (buffer != null) {
      buffer.clear();
    }
  }

}
