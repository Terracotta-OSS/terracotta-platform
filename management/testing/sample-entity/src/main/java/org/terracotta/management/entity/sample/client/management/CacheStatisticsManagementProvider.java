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
package org.terracotta.management.entity.sample.client.management;

import org.terracotta.context.extended.OperationStatisticDescriptor;
import org.terracotta.context.extended.ValueStatisticDescriptor;
import org.terracotta.management.entity.sample.CacheOperationOutcomes;
import org.terracotta.management.entity.sample.client.ClientCache;
import org.terracotta.management.model.capabilities.descriptors.Descriptor;
import org.terracotta.management.model.capabilities.descriptors.StatisticDescriptor;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.stats.Statistic;
import org.terracotta.management.registry.AbstractManagementProvider;
import org.terracotta.management.registry.Named;
import org.terracotta.management.registry.RequiredContext;
import org.terracotta.management.registry.action.ExposedObject;
import org.terracotta.management.registry.collect.StatisticProvider;
import org.terracotta.management.registry.collect.StatisticRegistry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static java.util.Collections.singleton;
import static java.util.EnumSet.allOf;
import static java.util.EnumSet.of;

/**
 * @author Mathieu Carbou
 */
@Named("CacheStatistics")
@RequiredContext({@Named("appName"), @Named("cacheName")})
@StatisticProvider
class CacheStatisticsManagementProvider extends AbstractManagementProvider<ClientCache> {

  private final Context parentContext;

  CacheStatisticsManagementProvider(Context parentContext) {
    super(ClientCache.class);
    this.parentContext = parentContext;
  }

  @Override
  public final Collection<? extends Descriptor> getDescriptors() {
    List<StatisticDescriptor> list = new ArrayList<>((Collection<? extends StatisticDescriptor>) super.getDescriptors());
    // To keep ordering because these objects end up in an immutable
    // topology so this is easier for testing to compare with json payloads
    Collections.sort(list, STATISTIC_DESCRIPTOR_COMPARATOR);
    return list;
  }

  @Override
  public Map<String, Statistic<?, ?>> collectStatistics(Context context, Collection<String> statisticNames) {
    // To keep ordering because these objects end up in an immutable
    // topology so this is easier for testing to compare with json payloads
    Map<String, Statistic<?, ?>> statistics = new TreeMap<String, Statistic<?, ?>>();
    ExposedClientCache exposedClientCache = (ExposedClientCache) findExposedObject(context);
    if (exposedClientCache != null) {
      if (statisticNames == null || statisticNames.isEmpty()) {
        statistics.putAll(exposedClientCache.queryStatistics());
      } else {
        for (String statisticName : statisticNames) {
          Statistic<?, ?> statistic = exposedClientCache.queryStatistic(statisticName);
          if (statistic != null) {
            statistics.put(statisticName, statistic);
          }
        }
      }
    }
    return statistics;
  }

  @Override
  protected ExposedObject<ClientCache> wrap(ClientCache managedObject) {
    return new ExposedClientCache(managedObject, parentContext.with("cacheName", managedObject.getName()));
  }

  private static class ExposedClientCache implements ExposedObject<ClientCache> {

    private final ClientCache clientCache;
    private final Context context;
    private final StatisticRegistry statisticRegistry;

    ExposedClientCache(ClientCache clientCache, Context context) {
      this.clientCache = clientCache;
      this.context = context;
      this.statisticRegistry = new StatisticRegistry(clientCache);

      OperationStatisticDescriptor<CacheOperationOutcomes.GetOutcome> get = OperationStatisticDescriptor.descriptor("get", singleton("cache"), CacheOperationOutcomes.GetOutcome.class);
      OperationStatisticDescriptor<CacheOperationOutcomes.ClearOutcome> clear = OperationStatisticDescriptor.descriptor("clear", singleton("cache"), CacheOperationOutcomes.ClearOutcome.class);

      statisticRegistry.registerCounter("Cache:HitCount", get, of(CacheOperationOutcomes.GetOutcome.HIT));
      statisticRegistry.registerCounter("Cache:MissCount", get, of(CacheOperationOutcomes.GetOutcome.MISS));
      statisticRegistry.registerCounter("Cache:ClearCount", clear, allOf(CacheOperationOutcomes.ClearOutcome.class));

      statisticRegistry.registerSize("Size", ValueStatisticDescriptor.descriptor("size", singleton("cache")));
    }

    Statistic<?, ?> queryStatistic(String fullStatisticName) {
      return statisticRegistry.queryStatistic(fullStatisticName);
    }

    Map<String, Statistic<?, ?>> queryStatistics() {
      return statisticRegistry.queryStatistics();
    }

    @Override
    public ClientCache getTarget() {
      return clientCache;
    }

    @Override
    public ClassLoader getClassLoader() {
      return clientCache.getClass().getClassLoader();
    }

    @Override
    public Context getContext() {
      return context;
    }

    @Override
    public Collection<? extends StatisticDescriptor> getDescriptors() {
      return statisticRegistry.getDescriptors();
    }

  }
}
