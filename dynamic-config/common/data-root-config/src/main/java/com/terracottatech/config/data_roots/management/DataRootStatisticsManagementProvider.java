/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.config.data_roots.management;

import com.terracottatech.config.data_roots.DataDirectoriesConfigImpl;
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
  private final DataDirectoriesConfigImpl dataRootConfig;

  public DataRootStatisticsManagementProvider(DataDirectoriesConfigImpl dataRootConfig) {
    super(DataRootBinding.class);
    this.dataRootConfig = dataRootConfig;
  }

  @Override
  protected AbstractExposedStatistics<DataRootBinding> internalWrap(Context context, DataRootBinding managedObject, StatisticRegistry statisticRegistry) {
    return new DataRootBindingExposedStatistics(context, managedObject, statisticRegistry, dataRootConfig);
  }

  private static class DataRootBindingExposedStatistics extends AbstractExposedStatistics<DataRootBinding> {
    DataRootBindingExposedStatistics(Context context, DataRootBinding binding, StatisticRegistry statisticRegistry, final DataDirectoriesConfigImpl dataRootConfig) {
      super(context.with("type", "DataRoot"), binding, statisticRegistry);

      getStatisticRegistry().registerStatistic("DataRoot:TotalDiskUsage", memoize(10, SECONDS, gauge(() -> dataRootConfig.getDiskUsageByRootIdentifier(binding.getAlias()))));
    }
  }
}
