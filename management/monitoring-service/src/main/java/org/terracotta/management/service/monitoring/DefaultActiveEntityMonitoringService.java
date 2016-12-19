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

import org.terracotta.entity.ClientDescriptor;
import org.terracotta.management.model.call.ContextualReturn;
import org.terracotta.management.model.capabilities.Capability;
import org.terracotta.management.model.cluster.Client;
import org.terracotta.management.model.cluster.ClientIdentifier;
import org.terracotta.management.model.cluster.ManagementRegistry;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.context.ContextContainer;
import org.terracotta.management.model.notification.ContextualNotification;
import org.terracotta.management.model.stats.ContextualStatistics;

import java.util.Objects;

/**
 * @author Mathieu Carbou
 */
public class DefaultActiveEntityMonitoringService extends AbstractEntityMonitoringService implements ActiveEntityMonitoringService {

  private final TopologyService topologyService;
  private final FiringService firingService;
  private final String serverName;

  DefaultActiveEntityMonitoringService(long consumerId, TopologyService topologyService, FiringService firingService) {
    super(consumerId);
    this.topologyService = Objects.requireNonNull(topologyService);
    this.firingService = Objects.requireNonNull(firingService);
    this.serverName = topologyService.getCurrentServerName();
  }

  @Override
  public void exposeManagementRegistry(ContextContainer contextContainer, Capability... capabilities) {
    logger.trace("[{}] exposeManagementRegistry({})", getConsumerId(), contextContainer);
    ManagementRegistry registry = ManagementRegistry.create(contextContainer);
    registry.addCapabilities(capabilities);
    topologyService.setEntityManagementRegistry(getConsumerId(), serverName, registry);
  }

  @Override
  public void pushNotification(ContextualNotification notification) {
    logger.trace("[{}] pushNotification({})", getConsumerId(), notification);
    topologyService.getEntityContext(serverName, getConsumerId()).ifPresent(context -> {
      notification.setContext(notification.getContext().with(context));
      firingService.fireNotification(notification);
    });
  }

  @Override
  public void pushStatistics(ContextualStatistics... statistics) {
    if (statistics.length > 0) {
      logger.trace("[{}] pushStatistics({})", getConsumerId(), statistics.length);
      for (ContextualStatistics statistic : statistics) {
        Context statContext = statistic.getContext();
        topologyService.getEntityContext(serverName, getConsumerId())
            .ifPresent(context -> statistic.setContext(statistic.getContext().with(context)));
      }
      firingService.fireStatistics(statistics);
    }
  }

  @Override
  public void answerManagementCall(String managementCallIdentifier, ContextualReturn<?> contextualReturn) {
    logger.trace("[{}] answerManagementCall({}, executed={}, error={})", getConsumerId(), managementCallIdentifier, contextualReturn.hasExecuted(), contextualReturn.errorThrown());
    firingService.fireManagementCallAnswer(managementCallIdentifier, contextualReturn);
  }

  @Override
  public ClientIdentifier getClientIdentifier(ClientDescriptor clientDescriptor) {
    return topologyService.getClientContext(getConsumerId(), clientDescriptor)
        .map(context -> ClientIdentifier.valueOf(context.get(Client.KEY)))
        .orElseThrow(() -> new IllegalStateException("ClientDescriptor " + clientDescriptor + " is not a client of entity " + getConsumerId()));
  }

}
