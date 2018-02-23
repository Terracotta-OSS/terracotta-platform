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
import org.terracotta.management.model.capabilities.descriptors.StatisticDescriptor;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.registry.collect.StatisticRegistry;
import org.terracotta.statistics.registry.Statistic;

import java.io.Closeable;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

@CommonComponent
public class AbstractExposedStatistics<T extends AliasBinding> extends AliasBindingManagementProvider.ExposedAliasBinding<T> implements Closeable {

  private final StatisticRegistry statisticRegistry;

  protected AbstractExposedStatistics(Context context, T binding, StatisticRegistry statisticRegistry) {
    super(context, binding);
    this.statisticRegistry = statisticRegistry;
  }

  @Override
  public void close() {
  }

  protected StatisticRegistry getStatisticRegistry() {
    return statisticRegistry;
  }

  @SuppressWarnings("unchecked")
  public <T extends Serializable> Optional<Statistic<T>> queryStatistic(String fullStatisticName, long since) {
    return statisticRegistry.queryStatistic(fullStatisticName, since);
  }

  public Map<String, Statistic<? extends Serializable>> queryStatistics(long since) {
    return statisticRegistry.queryStatistics(since);
  }

  @Override
  public Collection<? extends StatisticDescriptor> getDescriptors() {
    return statisticRegistry.getDescriptors();
  }

}
