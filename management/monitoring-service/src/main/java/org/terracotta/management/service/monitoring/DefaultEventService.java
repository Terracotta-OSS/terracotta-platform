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
import org.terracotta.entity.PlatformConfiguration;
import org.terracotta.management.model.call.ContextualCall;
import org.terracotta.management.model.call.ContextualReturn;
import org.terracotta.management.model.cluster.Client;
import org.terracotta.management.model.cluster.Server;
import org.terracotta.management.model.message.DefaultManagementCallMessage;
import org.terracotta.management.model.message.DefaultMessage;
import org.terracotta.management.model.notification.ContextualNotification;
import org.terracotta.management.model.stats.ContextualStatistics;
import org.terracotta.management.sequence.SequenceGenerator;

import java.io.Closeable;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Mathieu Carbou
 */
class DefaultEventService implements EventService, Closeable {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultEventService.class);

  private final SequenceGenerator sequenceGenerator;
  private final PlatformConfiguration platformConfiguration;
  private final SharedManagementRegistry sharedManagementRegistry;
  private final Map<Long, DefaultManagementService> managementServices;
  private final Map<Long, DefaultClientMonitoringService> clientMonitoringServices;

  //TODO: A/P support: https://github.com/Terracotta-OSS/terracotta-platform/issues/188
  // temporary there to simulate an entity callback with IEntityMessenger
  private final ExecutorService executorService = Executors.newSingleThreadExecutor();

  DefaultEventService(SequenceGenerator sequenceGenerator, PlatformConfiguration platformConfiguration, SharedManagementRegistry sharedManagementRegistry, Map<Long, DefaultManagementService> managementServices, Map<Long, DefaultClientMonitoringService> clientMonitoringServices) {
    this.sequenceGenerator = Objects.requireNonNull(sequenceGenerator);
    this.platformConfiguration = Objects.requireNonNull(platformConfiguration);
    this.sharedManagementRegistry = Objects.requireNonNull(sharedManagementRegistry);
    this.managementServices = Objects.requireNonNull(managementServices);
    this.clientMonitoringServices = Objects.requireNonNull(clientMonitoringServices);
  }

  @Override
  public void fireNotification(ContextualNotification notification) {
    DefaultMessage message = new DefaultMessage(sequenceGenerator.next(), "NOTIFICATION", notification);
    managementServices.values().forEach(managementService -> managementService.fireMessage(message));
  }

  @Override
  public void fireStatistics(ContextualStatistics[] statistics) {
    DefaultMessage message = new DefaultMessage(sequenceGenerator.next(), "STATISTICS", statistics);
    managementServices.values().forEach(managementService -> managementService.fireMessage(message));
  }

  @Override
  public void fireManagementCallAnswer(String managementCallIdentifier, ContextualReturn<?> answer) {
    DefaultManagementCallMessage message = new DefaultManagementCallMessage(managementCallIdentifier, sequenceGenerator.next(), "MANAGEMENT_CALL_RETURN", answer);
    managementServices.values().forEach(managementService -> managementService.fireMessage(message));
  }

  @Override
  public void fireManagementCallRequest(String managementCallIdentifier, ContextualCall<?> call) {
    DefaultManagementCallMessage message = new DefaultManagementCallMessage(managementCallIdentifier, sequenceGenerator.next(), "MANAGEMENT_CALL", call);

    if (call.getContext().contains(Client.KEY)) {
      clientMonitoringServices.values().forEach(clientMonitoringService -> clientMonitoringService.fireMessage(message));

    } else {
      //TODO: A/P support: https://github.com/Terracotta-OSS/terracotta-platform/issues/188
      // 1. use IEntityMessenger to call entity itself (simulated there with an executor service until we setup HA)
      executorService.submit(() -> {
        try {
          // 2. entity (active or passive) needs to check if the call is for it
          String serverName = call.getContext().get(Server.NAME_KEY);
          if (!platformConfiguration.getServerName().equals(serverName)) {
            throw new UnsupportedOperationException("Unable to route management call to server " + serverName);
          }
          // 3. call execution
          LOGGER.trace("[0] execute({})", call);
          ContextualReturn<?> contextualReturn = sharedManagementRegistry.withCapability(call.getCapability())
              .call(call.getMethodName(), call.getReturnType(), call.getParameters())
              .on(call.getContext())
              .build()
              .execute()
              .getSingleResult();
          // 4. A) a. if on passive, will call PassiveEntityMonitoringService.answerManagementCall()
          // 4. A) b. monitoring producer forward to the active, into DefaultDataListener.setState()
          // 4. A) c. call redirected to fireManagementCallAnswer()
          // 4. B) a. if on active, will call ActiveEntityMonitoringService.answerManagementCall()
          // 4. B) b. call redirected to fireManagementCallAnswer()
          fireManagementCallAnswer(managementCallIdentifier, contextualReturn);
        } catch (Exception e) {
          LOGGER.error("[0] Error executing management call " + call + ": " + e.getMessage(), e);
        }
      });
    }
  }

  @Override
  public void close() {
    executorService.shutdown();
  }

}
