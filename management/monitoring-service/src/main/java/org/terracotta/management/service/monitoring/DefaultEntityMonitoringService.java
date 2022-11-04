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
import org.terracotta.management.model.call.ContextualReturn;
import org.terracotta.management.model.capabilities.Capability;
import org.terracotta.management.model.cluster.ClientIdentifier;
import org.terracotta.management.model.cluster.ManagementRegistry;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.context.ContextContainer;
import org.terracotta.management.model.notification.ContextualNotification;
import org.terracotta.management.model.stats.ContextualStatistics;
import org.terracotta.monitoring.IMonitoringProducer;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.terracotta.management.service.monitoring.ManagementMessage.Type.MANAGEMENT_ANSWER;
import static org.terracotta.management.service.monitoring.ManagementMessage.Type.NOTIFICATION;
import static org.terracotta.management.service.monitoring.ManagementMessage.Type.REGISTRY;
import static org.terracotta.management.service.monitoring.ManagementMessage.Type.STATISTICS;

/**
 * @author Mathieu Carbou
 */
class DefaultEntityMonitoringService implements EntityMonitoringService {

  static final String RELIABLE_CHANNEL_KEY = "management";
  static final String UNRELIABLE_CHANNEL_KEY = "management/statistics";

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultEntityMonitoringService.class);

  private final IMonitoringProducer monitoringProducer;
  private final long consumerId;
  private final TopologyService topologyService;
  private final boolean activeEntity;

  DefaultEntityMonitoringService(long consumerId, IMonitoringProducer monitoringProducer, TopologyService topologyService, boolean activeEntity) {
    this.consumerId = consumerId;
    this.activeEntity = activeEntity;
    this.monitoringProducer = Objects.requireNonNull(monitoringProducer);
    this.topologyService = Objects.requireNonNull(topologyService);
  }

  @Override
  public long getConsumerId() {
    return consumerId;
  }

  @Override
  public String getServerName() {
    return topologyService.getServerName();
  }

  @Override
  public boolean isActiveEntityService() {
    return activeEntity;
  }

  @Override
  public void exposeManagementRegistry(ContextContainer contextContainer, Capability... capabilities) {
    if (LOGGER.isTraceEnabled()) {
      List<String> names = Stream.of(capabilities).map(Capability::getName).collect(Collectors.toList());
      LOGGER.trace("[{}] exposeManagementRegistry({})", getConsumerId(), names);
    }
    ManagementRegistry registry = ManagementRegistry.create(Context.empty(), contextContainer);
    registry.addCapabilities(capabilities);
    forwardToActiveServer(REGISTRY, registry);
  }

  @Override
  public void pushNotification(ContextualNotification notification) {
    LOGGER.trace("[{}] pushNotification({})", getConsumerId(), notification);
    forwardToActiveServer(NOTIFICATION, notification);
  }

  @Override
  public void pushStatistics(ContextualStatistics... statistics) {
    if (statistics.length > 0) {
      LOGGER.trace("[{}] pushStatistics({})", getConsumerId(), statistics.length);
      monitoringProducer.pushBestEffortsData(UNRELIABLE_CHANNEL_KEY, new ManagementMessage(getServerName(), getConsumerId(), isActiveEntityService(), STATISTICS, statistics));
    }
  }

  @Override
  public void answerManagementCall(String managementCallIdentifier, ContextualReturn<?> contextualReturn) {
    LOGGER.trace("[{}] answerManagementCall({}, {})", getConsumerId(), managementCallIdentifier, contextualReturn);
    forwardToActiveServer(MANAGEMENT_ANSWER, new Object[]{managementCallIdentifier, contextualReturn});
  }

  @Override
  public CompletableFuture<ClientIdentifier> getClientIdentifier(ClientDescriptor clientDescriptor) {
    LOGGER.trace("[{}] getClientIdentifier({})", getConsumerId(), clientDescriptor);
    if (isActiveEntityService()) {
      return topologyService.getClientIdentifier(getConsumerId(), clientDescriptor);
    } else {
      CompletableFuture<ClientIdentifier> future = new CompletableFuture<>();
      future.completeExceptionally(new UnsupportedOperationException("getClientIdentifier() cannot be called from a passive entity (consumerId=" + getConsumerId() + ")"));
      return future;
    }
  }

  public void init() {
    LOGGER.info("[{}] Initializing entity monitoring service", getConsumerId());
    monitoringProducer.addNode(new String[0], RELIABLE_CHANNEL_KEY, null);
    Stream.of(REGISTRY, MANAGEMENT_ANSWER, NOTIFICATION)
        .forEach(type -> monitoringProducer.addNode(new String[]{RELIABLE_CHANNEL_KEY}, type.name(), null));
  }

  private void forwardToActiveServer(ManagementMessage.Type type, Serializable data) {
    LOGGER.trace("[{}] addNode(management/{}, {})", getConsumerId(), type, data);
    monitoringProducer.addNode(new String[]{RELIABLE_CHANNEL_KEY}, type.name(), new ManagementMessage(getServerName(), getConsumerId(), isActiveEntityService(), type, data));
  }

}
