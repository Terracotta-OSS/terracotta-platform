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
package org.terracotta.management.registry;

import org.terracotta.management.model.capabilities.descriptors.Descriptor;
import org.terracotta.management.model.capabilities.descriptors.StatisticDescriptor;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.registry.collect.StatisticProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@StatisticProvider
public class DefaultStatisticsManagementProvider<T> extends AbstractManagementProvider<T> {

  protected final Context parentContext;

  @SuppressWarnings("unchecked")
  public DefaultStatisticsManagementProvider(Class<T> type, Context parentContext) {
    super(type);
    this.parentContext = org.terracotta.management.model.Objects.requireNonNull(parentContext);
  }

  @SuppressWarnings("unchecked")
  public DefaultStatisticsManagementProvider(Class<T> type) {
    this(type, Context.empty());
  }

  @Override
  protected DefaultStatisticsExposedObject<T> wrap(T o) {
    return new DefaultStatisticsExposedObject<T>(o, parentContext);
  }

  @SuppressWarnings("unchecked")
  @Override
  public final Collection<? extends Descriptor> getDescriptors() {
    Collection<StatisticDescriptor> capabilities = new HashSet<StatisticDescriptor>();
    for (ExposedObject o : getExposedObjects()) {
      capabilities.addAll(((DefaultStatisticsExposedObject<T>) o).getDescriptors());
    }
    List<StatisticDescriptor> list = new ArrayList<StatisticDescriptor>(capabilities);
    Collections.sort(list, STATISTIC_DESCRIPTOR_COMPARATOR);
    return list;
  }

  @Override
  public Map<String, Number> collectStatistics(Context context, Collection<String> statisticNames) {
    DefaultStatisticsExposedObject<T> exposedObject = (DefaultStatisticsExposedObject<T>) findExposedObject(context);
    if (exposedObject != null) {
      if (statisticNames == null || statisticNames.isEmpty()) {
        return exposedObject.queryStatistics();
      } else {
        Map<String, Number> statistics = new TreeMap<String, Number>();
        for (String statisticName : statisticNames) {
          try {
            statistics.put(statisticName, exposedObject.queryStatistic(statisticName));
          } catch (IllegalArgumentException ignored) {
            // ignore when statisticName does not exist and throws an exception
          }
        }
        return statistics;
      }
    }
    return Collections.emptyMap();
  }

}
