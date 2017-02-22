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

import com.tc.classloader.CommonComponent;
import org.terracotta.entity.PlatformConfiguration;

/**
 * @author Mathieu Carbou
 */
@CommonComponent
public class ManagementPluginContext {

  private final PlatformConfiguration platformConfiguration;
  private final StatisticService statisticService;
  private final long consumerId;
  private final ConsumerManagementRegistry consumerManagementRegistry;
  private final EntityMonitoringService monitoringService;

  public ManagementPluginContext(PlatformConfiguration platformConfiguration, StatisticService statisticService, long consumerId, ConsumerManagementRegistry consumerManagementRegistry, EntityMonitoringService monitoringService) {
    this.platformConfiguration = platformConfiguration;
    this.statisticService = statisticService;
    this.consumerId = consumerId;
    this.consumerManagementRegistry = consumerManagementRegistry;
    this.monitoringService = monitoringService;
  }

  public PlatformConfiguration getPlatformConfiguration() {
    return platformConfiguration;
  }

  public long getConsumerId() {
    return consumerId;
  }

  public ConsumerManagementRegistry getConsumerManagementRegistry() {
    return consumerManagementRegistry;
  }

  public EntityMonitoringService getMonitoringService() {
    return monitoringService;
  }

  public StatisticService getStatisticService() {
    return statisticService;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("ManagementPluginContext{");
    sb.append("consumerId=").append(consumerId);
    sb.append('}');
    return sb.toString();
  }
}
