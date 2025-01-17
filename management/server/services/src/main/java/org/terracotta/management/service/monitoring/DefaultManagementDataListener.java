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
import org.terracotta.management.model.call.ContextualReturn;
import org.terracotta.management.model.cluster.ManagementRegistry;
import org.terracotta.management.model.cluster.ServerEntity;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.notification.ContextualNotification;
import org.terracotta.management.model.stats.ContextualStatistics;

import java.util.Objects;

/**
 * @author Mathieu Carbou
 */
class DefaultManagementDataListener implements ManagementDataListener, TopologyEventListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultManagementDataListener.class);

  private final TopologyService topologyService;
  private final FiringService firingService;

  private volatile boolean currentServerActive;
  private volatile String currentServerName;

  // This flag is used to detect the tree replay phase after a failover. After a failover:
  // 1. Platform sends event serverDidBecomeActive()
  // 2. Platform rebuilds the tree with addNode() for created active entities, etc (platform-level stuff)
  // 3. Platform replays addNode() calls made by old passive entities
  // 4. Platform replays pushBestEffortData() calls made by old passive entities
  // 5. Platform starts sending addNode() and pushBestEffortData() for messages coming from the new active entity
  // We need to detect this replay phase so that we can ignore incoming messages coming from passive entities,
  // until we are out of this failover phase, and throw assertion f we are still receiving messages from passives.
  // Steps 1-4 are done in the failover thread and 5 on the request processor thread.
  private volatile String failoverThreadName;

  DefaultManagementDataListener(TopologyService topologyService, FiringService firingService) {
    this.topologyService = Objects.requireNonNull(topologyService);
    this.firingService = Objects.requireNonNull(firingService);
  }

  @Override
  public void onBecomeActive(String serverName) {
    currentServerName = serverName;
    currentServerActive = true;
    failoverThreadName = Thread.currentThread().getName();
  }

  // ===================================================
  // CALLBACK: for data sent through IMonitoringProducer
  // ===================================================

  @Override
  public void onStatistics(MessageSource messageSource, ContextualStatistics[] statistics) {
    accept(messageSource, "onStatistics", () -> {
      // handles data coming from DefaultEntityMonitoringService.pushStatistics()
      topologyService.willPushEntityStatistics(messageSource.getConsumerId(), messageSource.getServerName(), statistics);
    });
  }

  @Override
  public void onCallAnswer(MessageSource messageSource, String managementCallIdentifier, ContextualReturn<?> answer) {
    accept(messageSource, "onCallAnswer", () -> {
      // handles data coming from DefaultEntityMonitoringService.answerManagementCall()
      firingService.fireManagementCallAnswer(managementCallIdentifier, answer);
    });
  }

  @Override
  public void onRegistry(MessageSource messageSource, ManagementRegistry registry) {
    accept(messageSource, "onRegistry", () -> {
      // handles data coming from DefaultEntityMonitoringService.exposeServerEntityManagementRegistry()
      topologyService.willSetEntityManagementRegistry(messageSource.getConsumerId(), messageSource.getServerName(), registry);
    });
  }

  @Override
  public void onNotification(MessageSource messageSource, ContextualNotification notification) {
    accept(messageSource, "onNotification", () -> {
      // handles data coming from DefaultEntityMonitoringService.pushNotification()
      Context notificationContext = notification.getContext();
      long consumerId = messageSource.getConsumerId();
      if (notificationContext.contains(ServerEntity.CONSUMER_ID)) {
        consumerId = Long.parseLong(notificationContext.get(ServerEntity.CONSUMER_ID));
      }
      topologyService.willPushEntityNotification(consumerId, messageSource.getServerName(), notification);
    });
  }

  private void accept(MessageSource messageSource, String method, Runnable runnable) {
    LOGGER.trace("[{}] {}(from={}, activeEntity={})", messageSource.getConsumerId(), method, messageSource.getServerName(), messageSource.isActiveEntity());

    // We only accept:
    // - messages coming from an active entity onto an active with same server names
    // - messages coming from a passive entity to an active server with different server names
    // We discard messages from the replay.
    // We fail if we still receive data from the old passive entity and promotion is finished
    boolean activeToActive = messageSource.isActiveEntity() && currentServerActive && currentServerName.equals(messageSource.getServerName());
    boolean passiveToActive = !messageSource.isActiveEntity() && currentServerActive && !currentServerName.equals(messageSource.getServerName());
    boolean fromOldPassiveEntity = !messageSource.isActiveEntity() && currentServerActive && currentServerName.equals(messageSource.getServerName());
    boolean replaying = fromOldPassiveEntity && Thread.currentThread().getName().equals(failoverThreadName);

    if (activeToActive || passiveToActive) {
      runnable.run();
    } else if (replaying) {
      LOGGER.warn("[{}] {}(from={}, activeEntity={}) IGNORED. Failover in progress. Current server: {}, active={}",
          messageSource.getConsumerId(), method, messageSource.getServerName(), messageSource.isActiveEntity(), currentServerName, currentServerActive);
    } else {
      Utils.warnOrAssert(LOGGER, "[{}] {}(from={}, activeEntity={}) IGNORED. Current server: {}, active={}",
          messageSource.getConsumerId(), method, messageSource.getServerName(), messageSource.isActiveEntity(), currentServerName, currentServerActive);
    }
  }

}
