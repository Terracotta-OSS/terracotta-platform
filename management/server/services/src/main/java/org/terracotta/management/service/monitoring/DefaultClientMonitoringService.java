/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
import org.terracotta.management.model.call.ContextualReturn;
import org.terracotta.management.model.capabilities.Capability;
import org.terracotta.management.model.cluster.Client;
import org.terracotta.management.model.cluster.ClientIdentifier;
import org.terracotta.management.model.cluster.ManagementRegistry;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.context.ContextContainer;
import org.terracotta.management.model.context.Contextual;
import org.terracotta.management.model.message.Message;
import org.terracotta.management.model.notification.ContextualNotification;
import org.terracotta.management.model.stats.ContextualStatistics;
import org.terracotta.voltron.proxy.ProxyEntityResponse;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Mathieu Carbou
 */
class DefaultClientMonitoringService implements ClientMonitoringService, TopologyEventListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultClientMonitoringService.class);

  private final long consumerId;
  private final DefaultFiringService firingService;
  private final ClientCommunicator clientCommunicator;
  private final TopologyService topologyService;
  private final Map<ClientIdentifier, ClientDescriptor> manageableClients = new ConcurrentHashMap<>();

  DefaultClientMonitoringService(long consumerId, TopologyService topologyService, DefaultFiringService firingService, ClientCommunicator clientCommunicator) {
    this.consumerId = consumerId;
    this.topologyService = Objects.requireNonNull(topologyService);
    this.firingService = Objects.requireNonNull(firingService);
    this.clientCommunicator = Objects.requireNonNull(clientCommunicator);

    topologyService.addTopologyEventListener(this);
    firingService.addClientMonitoringService(this);
  }

  @Override
  public void pushNotification(ClientDescriptor from, ContextualNotification notification) {
    LOGGER.trace("[{}] pushNotification({}, {})", consumerId, from, notification);
    topologyService.willPushClientNotification(consumerId, from, notification);
  }

  @Override
  public void pushStatistics(ClientDescriptor from, ContextualStatistics... statistics) {
    if (statistics.length > 0) {
      LOGGER.trace("[{}] pushStatistics({}, {})", consumerId, from, statistics.length);
      topologyService.willPushClientStatistics(consumerId, from, statistics);
    }
  }

  @Override
  public void exposeTags(ClientDescriptor from, String... tags) {
    LOGGER.trace("[{}] exposeTags({}, {})", consumerId, from, Arrays.toString(tags));
    topologyService.willSetClientTags(consumerId, from, tags);
  }

  @Override
  public void exposeManagementRegistry(ClientDescriptor from, Context root, ContextContainer contextContainer, Capability... capabilities) {
    if (LOGGER.isTraceEnabled()) {
      List<String> names = Stream.of(capabilities).map(Capability::getName).collect(Collectors.toList());
      LOGGER.trace("[{}] exposeManagementRegistry({}, {})", consumerId, from, names);
    }
    ManagementRegistry newRegistry = ManagementRegistry.create(root, contextContainer);
    newRegistry.addCapabilities(capabilities);
    topologyService.willSetClientManagementRegistry(consumerId, from, newRegistry);
    topologyService.getClientIdentifier(consumerId, from)
        .thenAccept(clientIdentifier -> manageableClients.put(clientIdentifier, from));

  }

  @Override
  public void answerManagementCall(ClientDescriptor caller, String managementCallIdentifier, ContextualReturn<?> contextualReturn) {
    LOGGER.trace("[{}] answerManagementCall({}, {})", consumerId, managementCallIdentifier, contextualReturn);
    firingService.fireManagementCallAnswer(managementCallIdentifier, contextualReturn);
  }

  @Override
  public void onBecomeActive(String serverName) {
    LOGGER.trace("[{}] onBecomeActive()", this.consumerId);
    clear();
  }

  @Override
  public void onUnfetch(long consumerId, ClientDescriptor clientDescriptor) {
    if (consumerId == this.consumerId) {
      if (manageableClients.entrySet().removeIf(e -> e.getValue().equals(clientDescriptor))) {
        LOGGER.trace("[{}] onUnfetch({})", this.consumerId, clientDescriptor);
      }
    }
  }

  void fireMessage(Message message) {
    if (message.getType().equals("MANAGEMENT_CALL")) {
      Context context = message.unwrap(Contextual.class).get(0).getContext();
      String clientId = context.get(Client.KEY);
      if (clientId != null) {
        ClientIdentifier clientIdentifier = ClientIdentifier.valueOf(clientId);
        ClientDescriptor clientDescriptor = manageableClients.get(clientIdentifier);
        if (clientDescriptor != null) {
          send(clientDescriptor, message);
        }
      }
    } else {
      Utils.warnOrAssert(LOGGER, "[{}] fireMessage({}): message type unsupported", this.consumerId, message.getType());
    }
  }

  private void send(ClientDescriptor client, Message message) {
    LOGGER.trace("[{}] send({}, {})", consumerId, client, message);
    try {
      clientCommunicator.sendNoResponse(client, ProxyEntityResponse.messageResponse(Message.class, message));
    } catch (Exception e) {
      LOGGER.warn("Unable to send message " + message + " to client " + client);
    }
  }

  private void clear() {
    manageableClients.clear();
  }

  @Override
  public void close() {
    LOGGER.info("[{}] Closing", this.consumerId);
    clear();
    topologyService.removeTopologyEventListener(this);
    firingService.removeClientMonitoringService(this);
  }
}
