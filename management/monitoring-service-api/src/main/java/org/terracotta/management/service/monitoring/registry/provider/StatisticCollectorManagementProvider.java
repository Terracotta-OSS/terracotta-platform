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
package org.terracotta.management.service.monitoring.registry.provider;

import com.tc.classloader.CommonComponent;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.stats.ContextualStatistics;
import org.terracotta.management.registry.action.ExposedObject;
import org.terracotta.management.registry.action.Named;
import org.terracotta.management.registry.action.RequiredContext;
import org.terracotta.management.registry.collect.StatisticCollector;
import org.terracotta.management.registry.collect.StatisticCollectorProvider;
import org.terracotta.management.registry.collect.StatisticConfiguration;
import org.terracotta.management.service.monitoring.MonitoringService;
import org.terracotta.management.service.monitoring.StatisticsService;

@Named("StatisticCollectorCapability")
@RequiredContext({@Named("consumerId")})
@CommonComponent
public class StatisticCollectorManagementProvider extends StatisticCollectorProvider<StatisticCollector> implements StatisticsServiceAware, MonitoringServiceAware {

  private final StatisticConfiguration statisticConfiguration;
  private StatisticsService statisticsService;
  private MonitoringService monitoringService;

  public StatisticCollectorManagementProvider(Context context, StatisticConfiguration statisticConfiguration) {
    super(StatisticCollector.class, context);
    this.statisticConfiguration = statisticConfiguration;
  }

  @Override
  public void setMonitoringService(MonitoringService monitoringService) {
    this.monitoringService = monitoringService;
  }

  @Override
  public void setStatisticsService(StatisticsService statisticsService) {
    this.statisticsService = statisticsService;
  }

  @Override
  protected void dispose(ExposedObject<StatisticCollector> exposedObject) {
    exposedObject.getTarget().stopStatisticCollector();
  }

  public void init() {
    StatisticCollector statisticCollector = statisticsService.createStatisticCollector(
        statisticConfiguration,
        statistics -> monitoringService.pushServerEntityStatistics(statistics.toArray(new ContextualStatistics[statistics.size()])));
    register(statisticCollector);
    statisticCollector.startStatisticCollector();
  }

}
