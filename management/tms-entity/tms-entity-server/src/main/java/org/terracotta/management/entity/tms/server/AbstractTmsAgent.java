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
import org.terracotta.management.service.monitoring.ConsumerManagementRegistry;
import org.terracotta.management.service.monitoring.EntityMonitoringService;
import org.terracotta.management.service.monitoring.SharedManagementRegistry;

import java.util.Objects;

/**
 * @author Mathieu Carbou
 */
abstract class AbstractTmsAgent implements TmsAgent, TmsAgentMessenger {

  private final ConsumerManagementRegistry consumerManagementRegistry;
  private final EntityMonitoringService entityMonitoringService;
  private final SharedManagementRegistry sharedManagementRegistry;

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  AbstractTmsAgent(ConsumerManagementRegistry consumerManagementRegistry, EntityMonitoringService entityMonitoringService, SharedManagementRegistry sharedManagementRegistry) {
    this.consumerManagementRegistry = Objects.requireNonNull(consumerManagementRegistry);
    this.entityMonitoringService = Objects.requireNonNull(entityMonitoringService);
    this.sharedManagementRegistry = Objects.requireNonNull(sharedManagementRegistry);
  }

  void init() {
    logger.trace("[{}] init()", entityMonitoringService.getConsumerId());
    consumerManagementRegistry.refresh();
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
