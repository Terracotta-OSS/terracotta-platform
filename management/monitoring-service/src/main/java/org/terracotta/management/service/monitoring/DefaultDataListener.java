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
import org.terracotta.management.model.call.ContextualReturn;
import org.terracotta.management.model.cluster.ManagementRegistry;
import org.terracotta.management.model.cluster.ServerEntity;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.notification.ContextualNotification;
import org.terracotta.management.model.stats.ContextualStatistics;
import org.terracotta.monitoring.PlatformServer;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

/**
 * @author Mathieu Carbou
 */
class DefaultDataListener implements DataListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultDataListener.class);

  static final String TOPIC_SERVER_ENTITY_NOTIFICATION = "server-entity-notification";
  static final String TOPIC_SERVER_ENTITY_STATISTICS = "server-entity-statistics";

  private final long consumerId;
  private final TopologyService topologyService;
  private final FiringService firingService;

  DefaultDataListener(long consumerId, TopologyService topologyService, FiringService firingService) {
    this.consumerId = consumerId;
    this.topologyService = Objects.requireNonNull(topologyService);
    this.firingService = Objects.requireNonNull(firingService);
  }

  // ===========================================================================
  // CALLBACK: for data sent from passive and active through IMonitoringProducer
  // ===========================================================================

  @Override
  public void pushBestEffortsData(long consumerId, PlatformServer sender, String name, Serializable data) {
    LOGGER.trace("[{}] pushBestEffortsData({}, {}, {})", this.consumerId, consumerId, sender.getServerName(), name);

    switch (name) {

      case TOPIC_SERVER_ENTITY_NOTIFICATION: {
        // handles data coming from DefaultPassiveEntityMonitoringService.pushNotification()
        ContextualNotification notification = (ContextualNotification) data;
        Context notificationContext = notification.getContext();
        if (notificationContext.contains(ServerEntity.CONSUMER_ID)) {
          consumerId = Long.parseLong(notificationContext.get(ServerEntity.CONSUMER_ID));
        }
        topologyService.willPushEntityNotification(consumerId, sender.getServerName(), notification);
        break;
      }

      case TOPIC_SERVER_ENTITY_STATISTICS: {
        // handles data coming from DefaultPassiveEntityMonitoringService.pushStatistics()
        ContextualStatistics[] statistics = (ContextualStatistics[]) data;
        topologyService.willPushEntityStatistics(consumerId, sender.getServerName(), statistics);
        break;
      }

      default: {
        throw new IllegalArgumentException(name);
      }
    }

  }

  @Override
  public synchronized void setState(long consumerId, PlatformServer sender, String[] path, Serializable data) {
    LOGGER.trace("[{}] setState({}, {}, {})", this.consumerId, consumerId, sender.getServerName(), Arrays.toString(path));

    if (path.length == 1 && "registry".equals(path[0])) {
      // handles data coming from DefaultMonitoringService.exposeServerEntityManagementRegistry()
      ManagementRegistry newRegistry = (ManagementRegistry) data;
      topologyService.willSetEntityManagementRegistry(consumerId, sender.getServerName(), newRegistry);

    } else if (path.length == 2 && "management-answer".equals(path[0])) {
      // handles data coming from DefaultMonitoringService.answerManagementCall()
      String managementCallIdentifier = path[1];
      ContextualReturn<?> answer = (ContextualReturn<?>) data;
      firingService.fireManagementCallAnswer(managementCallIdentifier, answer);
    }
  }

}
