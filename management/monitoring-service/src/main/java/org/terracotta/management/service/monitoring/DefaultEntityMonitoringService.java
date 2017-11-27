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

import java.util.Arrays;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.PlatformConfiguration;
import org.terracotta.management.model.call.ContextualReturn;
import org.terracotta.management.model.capabilities.Capability;
import org.terracotta.management.model.cluster.ClientIdentifier;
import org.terracotta.management.model.cluster.ManagementRegistry;
import org.terracotta.management.model.context.ContextContainer;
import org.terracotta.management.model.notification.ContextualNotification;
import org.terracotta.management.model.stats.ContextualStatistics;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static org.terracotta.management.service.monitoring.DefaultDataListener.TOPIC_SERVER_ENTITY_NOTIFICATION;
import static org.terracotta.management.service.monitoring.DefaultDataListener.TOPIC_SERVER_ENTITY_STATISTICS;
import org.terracotta.monitoring.IMonitoringProducer;

/**
 * @author Mathieu Carbou
 */
public class DefaultEntityMonitoringService extends AbstractEntityMonitoringService {

  private final TopologyService topology;
  private final IMonitoringProducer monitoringProducer;
  private final String[] callbackPath;

  DefaultEntityMonitoringService(long consumerId, TopologyService topology, IMonitoringProducer monitoringProducer, PlatformConfiguration platformConfiguration) {
    super(consumerId, platformConfiguration);
    this.topology = topology;
    this.monitoringProducer = monitoringProducer;
    this.callbackPath = new String[] {"management-answer"};
    monitoringProducer.addNode(new String[0], "management-answer", null);
  }

  @Override
  public void exposeManagementRegistry(ContextContainer contextContainer, Capability... capabilities) {
    if(logger.isTraceEnabled()) {
      List<String> names = Stream.of(capabilities).map(Capability::getName).collect(Collectors.toList());
      logger.trace("[{}] exposeManagementRegistry({})", getConsumerId(), names);
    }
    ManagementRegistry registry = ManagementRegistry.create(contextContainer);
    registry.addCapabilities(capabilities);
    monitoringProducer.addNode(new String[0], "registry", registry);
  }

  @Override
  public void pushNotification(ContextualNotification notification) {
    logger.trace("[{}] pushNotification({})", getConsumerId(), notification);
    monitoringProducer.pushBestEffortsData(TOPIC_SERVER_ENTITY_NOTIFICATION, notification);
  }

  @Override
  public void pushStatistics(ContextualStatistics... statistics) {
    if (statistics.length > 0) {
      logger.trace("[{}] pushStatistics({})", getConsumerId(), statistics.length);
      monitoringProducer.pushBestEffortsData(TOPIC_SERVER_ENTITY_STATISTICS, statistics);
    }
  }

  @Override
  public void answerManagementCall(String managementCallIdentifier, ContextualReturn<?> contextualReturn) {
    logger.trace("[{}] answerManagementCall({}, {})", getConsumerId(), managementCallIdentifier, contextualReturn);
    monitoringProducer.addNode(this.callbackPath, managementCallIdentifier, contextualReturn);
  }

  @Override
  public CompletableFuture<ClientIdentifier> getClientIdentifier(ClientDescriptor clientDescriptor) {
    logger.trace("[{}] getClientIdentifier({})", getConsumerId(), clientDescriptor);
    return topology.getClientIdentifier(getConsumerId(), clientDescriptor);
  }
}
