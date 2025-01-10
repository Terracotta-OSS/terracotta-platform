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
package org.terracotta.management.registry;

import org.terracotta.management.model.capabilities.descriptors.StatisticDescriptor;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.stats.StatisticRegistry;
import org.terracotta.statistics.registry.Statistic;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.LongSupplier;

/**
 * @author Mathieu Carbou
 */
public class DefaultStatisticsExposedObject<T> extends DefaultExposedObject<T> {

  protected final StatisticRegistry statisticRegistry;

  public DefaultStatisticsExposedObject(T o, LongSupplier timeSource, Context context) {
    super(o, context);
    statisticRegistry = new StatisticRegistry(o, timeSource);
  }

  public DefaultStatisticsExposedObject(T o, LongSupplier timeSource) {
    super(o);
    statisticRegistry = new StatisticRegistry(o, timeSource);
  }

  public StatisticRegistry getStatisticRegistry() {
    return statisticRegistry;
  }

  public <U extends Serializable> Optional<Statistic<U>> queryStatistic(String fullStatisticName, long since) {
    return statisticRegistry.queryStatistic(fullStatisticName, since);
  }

  public Map<String, Statistic<? extends Serializable>> queryStatistics(long since) {
    return statisticRegistry.queryStatistics(since);
  }

  @Override
  public Collection<StatisticDescriptor> getDescriptors() {
    return statisticRegistry.getDescriptors();
  }
}
