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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.entity.tms.TmsAgent;
import org.terracotta.management.model.call.ContextualCall;
import org.terracotta.management.model.call.ContextualReturn;
import org.terracotta.management.model.cluster.Server;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.context.ContextContainer;
import org.terracotta.management.service.monitoring.ConsumerManagementRegistry;
import org.terracotta.management.service.monitoring.EntityMonitoringService;
import org.terracotta.management.service.monitoring.SharedManagementRegistry;
import org.terracotta.management.service.monitoring.registry.provider.StatisticCollectorManagementProvider;

import java.util.Objects;

/**
 * @author Mathieu Carbou
 */
abstract class AbstractTmsAgent implements TmsAgent, TmsAgentMessenger {

  private final ConsumerManagementRegistry consumerManagementRegistry;
  private final EntityMonitoringService entityMonitoringService;
  private final SharedManagementRegistry sharedManagementRegistry;

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private boolean initialized;

  AbstractTmsAgent(ConsumerManagementRegistry consumerManagementRegistry, EntityMonitoringService entityMonitoringService, SharedManagementRegistry sharedManagementRegistry) {
    this.consumerManagementRegistry = Objects.requireNonNull(consumerManagementRegistry);
    this.entityMonitoringService = Objects.requireNonNull(entityMonitoringService);
    this.sharedManagementRegistry = Objects.requireNonNull(sharedManagementRegistry);
  }

  // Workaround for https://github.com/Terracotta-OSS/terracotta-core/issues/426
  synchronized void init() {
    if (!initialized) {
      logger.trace("[{}] init()", entityMonitoringService.getConsumerId());

      ContextContainer contextContainer = consumerManagementRegistry.getContextContainer();

      // the context for the collector, created from the the registry of the tms entity
      Context context = Context.create(contextContainer.getName(), contextContainer.getValue());

      // we create a provider that will receive management calls to control the global voltron's statistic collector
      // this provider will thus be on top of the tms entity
      StatisticCollectorManagementProvider collectorManagementProvider = new StatisticCollectorManagementProvider(context);
      consumerManagementRegistry.addManagementProvider(collectorManagementProvider);

      // start the stat collector (it won't collect any stats though, because they need to be configured through a management call)
      collectorManagementProvider.init();

      consumerManagementRegistry.refresh();

      initialized = true;
    }
  }

  @Override
  public void executeManagementCall(String managementCallIdentifier, ContextualCall<?> call) {
    String serverName = call.getContext().get(Server.NAME_KEY);
    if (serverName == null) {
      throw new IllegalArgumentException("Bad context: " + call.getContext());
    }
    if (entityMonitoringService.getServerName().equals(serverName)) {
      logger.trace("[{}] executeManagementCall({}, {}, {}, {})", entityMonitoringService.getConsumerId(), managementCallIdentifier, call.getContext(), call.getCapability(), call.getMethodName());
      ContextualReturn<?> contextualReturn = sharedManagementRegistry.withCapability(call.getCapability())
          .call(call.getMethodName(), call.getReturnType(), call.getParameters())
          .on(call.getContext())
          .build()
          .execute()
          .getSingleResult();
      entityMonitoringService.answerManagementCall(managementCallIdentifier, contextualReturn);
    }
  }

}
