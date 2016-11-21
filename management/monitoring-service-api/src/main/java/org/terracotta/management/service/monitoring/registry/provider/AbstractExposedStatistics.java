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
import org.terracotta.context.extended.StatisticsRegistry;
import org.terracotta.management.model.capabilities.descriptors.Descriptor;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.stats.Statistic;
import org.terracotta.management.registry.collect.StatisticsRegistryMetadata;

import java.io.Closeable;
import java.util.Collection;

@CommonComponent
public class AbstractExposedStatistics<T extends AliasBinding> extends AliasBindingManagementProvider.ExposedAliasBinding<T> implements Closeable {

  private final StatisticsRegistry statisticsRegistry;
  private final StatisticsRegistryMetadata statisticsRegistryMetadata;

  protected AbstractExposedStatistics(Context context, T binding, StatisticsRegistry statisticsRegistry) {
    super(context, binding);
    // allows it to be null
    this.statisticsRegistry = statisticsRegistry;
    this.statisticsRegistryMetadata = new StatisticsRegistryMetadata(statisticsRegistry);
  }

  @Override
  public void close() {
    if (statisticsRegistry != null) {
      statisticsRegistry.clearRegistrations();
    }
  }

  protected StatisticsRegistry getStatisticsRegistry() {
    return statisticsRegistry;
  }

  protected StatisticsRegistryMetadata getStatisticsRegistryMetadata() {
    return statisticsRegistryMetadata;
  }

  @SuppressWarnings("unchecked")
  public Statistic<?, ?> queryStatistic(String fullStatisticName, long since) {
    return statisticsRegistryMetadata.queryStatistic(fullStatisticName, since);
  }

  @Override
  public Collection<Descriptor> getDescriptors() {
    return statisticsRegistryMetadata.getDescriptors();
  }

}
