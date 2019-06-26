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
import org.terracotta.management.model.stats.Statistic;
import org.terracotta.management.registry.collect.StatisticProvider;
import org.terracotta.management.model.stats.StatisticRegistry;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.function.LongSupplier;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@StatisticProvider
public class DefaultStatisticsManagementProvider<T> extends AbstractManagementProvider<T> {

  protected final LongSupplier timeSource;
  protected final Context parentContext;

  @SuppressWarnings("unchecked")
  public DefaultStatisticsManagementProvider(Class<T> type, LongSupplier timeSource, Context parentContext) {
    super(type);
    this.timeSource = timeSource;
    this.parentContext = Objects.requireNonNull(parentContext);
  }

  @SuppressWarnings("unchecked")
  public DefaultStatisticsManagementProvider(Class<T> type, LongSupplier timeSource) {
    this(type, timeSource, Context.empty());
  }

  @Override
  protected DefaultStatisticsExposedObject<T> wrap(T o) {
    return new DefaultStatisticsExposedObject<T>(o, timeSource, parentContext);
  }

  @SuppressWarnings("unchecked")
  @Override
  public final Collection<? extends Descriptor> getDescriptors() {
    // To keep ordering because these objects end up in an immutable
    // topology so this is easier for testing to compare with json payloads
    return super.getDescriptors()
        .stream()
        .map(d -> (StatisticDescriptor) d)
        .sorted(STATISTIC_DESCRIPTOR_COMPARATOR)
        .collect(toList());
  }

  @Override
  public Map<String, Statistic<? extends Serializable>> collectStatistics(Context context, Collection<String> statisticNames, long since) {
    DefaultStatisticsExposedObject<T> exposedObject = (DefaultStatisticsExposedObject<T>) findExposedObject(context);
    if (exposedObject == null) {
      return Collections.emptyMap();
    }
    return StatisticRegistry.collect(exposedObject.getStatisticRegistry(), statisticNames, since);
  }

}
