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
package org.terracotta.config.data_roots.management;

import org.terracotta.config.data_roots.DataDirsConfigImpl;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.stats.StatisticRegistry;
import org.terracotta.management.registry.Named;
import org.terracotta.management.registry.RequiredContext;
import org.terracotta.management.registry.collect.StatisticProvider;
import org.terracotta.management.service.monitoring.registry.provider.AbstractExposedStatistics;
import org.terracotta.management.service.monitoring.registry.provider.AbstractStatisticsManagementProvider;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.terracotta.statistics.ValueStatistics.gauge;
import static org.terracotta.statistics.ValueStatistics.memoize;

@Named("DataRootStatistics")
@RequiredContext({@Named("consumerId"), @Named("type"), @Named("alias")})
@StatisticProvider
public class DataRootStatisticsManagementProvider extends AbstractStatisticsManagementProvider<DataRootBinding> {
  private final DataDirsConfigImpl dataRootConfig;

  public DataRootStatisticsManagementProvider(DataDirsConfigImpl dataRootConfig) {
    super(DataRootBinding.class);
    this.dataRootConfig = dataRootConfig;
  }

  @Override
  protected AbstractExposedStatistics<DataRootBinding> internalWrap(Context context, DataRootBinding managedObject, StatisticRegistry statisticRegistry) {
    return new DataRootBindingExposedStatistics(context, managedObject, statisticRegistry, dataRootConfig);
  }

  private static class DataRootBindingExposedStatistics extends AbstractExposedStatistics<DataRootBinding> {
    DataRootBindingExposedStatistics(Context context, DataRootBinding binding, StatisticRegistry statisticRegistry, final DataDirsConfigImpl dataRootConfig) {
      super(context.with("type", "DataRoot"), binding, statisticRegistry);

      getStatisticRegistry().registerStatistic("DataRoot:TotalDiskUsage", memoize(10, SECONDS, gauge(() -> dataRootConfig.getDiskUsageByRootIdentifier(binding.getAlias()))));
    }
  }
}
