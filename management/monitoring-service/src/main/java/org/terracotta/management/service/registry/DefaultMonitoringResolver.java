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
package org.terracotta.management.service.registry;

import org.terracotta.entity.ClientDescriptor;
import org.terracotta.management.model.cluster.ClientIdentifier;
import org.terracotta.management.model.notification.ContextualNotification;
import org.terracotta.management.model.stats.ContextualStatistics;
import org.terracotta.management.service.monitoring.MonitoringService;

/**
 * @author Mathieu Carbou
 */
class DefaultMonitoringResolver implements MonitoringResolver {

  private final MonitoringService monitoringService;

  DefaultMonitoringResolver(MonitoringService monitoringService) {
    this.monitoringService = monitoringService;
  }

  @Override
  public ClientIdentifier getConnectedClientIdentifier(ClientDescriptor clientDescriptor) {
    return monitoringService.getClientIdentifier(clientDescriptor);
  }

  @Override
  public long getConsumerId() {
    return monitoringService.getConsumerId();
  }

  @Override
  public void pushServerEntityNotification(ContextualNotification notification) {
    monitoringService.pushServerEntityNotification(notification);
  }

  @Override
  public void pushServerEntityStatistics(ContextualStatistics... statistics) {
    monitoringService.pushServerEntityStatistics(statistics);
  }

}
