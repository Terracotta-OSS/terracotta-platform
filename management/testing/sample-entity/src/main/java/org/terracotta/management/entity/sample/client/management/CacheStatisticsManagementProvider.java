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
import org.terracotta.context.extended.StatisticsRegistry;
import org.terracotta.management.entity.sample.CacheOperationOutcomes;
import org.terracotta.management.entity.sample.client.ClientCache;
import org.terracotta.management.model.capabilities.Capability;
import org.terracotta.management.model.capabilities.StatisticsCapability;
import org.terracotta.management.model.capabilities.descriptors.Descriptor;
import org.terracotta.management.model.capabilities.descriptors.StatisticDescriptor;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.stats.Statistic;
import org.terracotta.management.registry.AbstractManagementProvider;
import org.terracotta.management.registry.action.ExposedObject;
import org.terracotta.management.registry.action.Named;
import org.terracotta.management.registry.action.RequiredContext;
import org.terracotta.management.registry.collect.StatisticConfiguration;
import org.terracotta.management.registry.collect.StatisticsRegistryMetadata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.Collections.singleton;
import static java.util.EnumSet.allOf;
import static java.util.EnumSet.of;
import static org.terracotta.context.extended.ValueStatisticDescriptor.descriptor;

/**
 * @author Mathieu Carbou
 */
@Named("CacheStatistics")
@RequiredContext({@Named("appName"), @Named("cacheName")})
class CacheStatisticsManagementProvider extends AbstractManagementProvider<ClientCache> {

  private static final Comparator<StatisticDescriptor> STATISTIC_DESCRIPTOR_COMPARATOR = (o1, o2) -> o1.getName().compareTo(o2.getName());

  private final Context parentContext;
  private final ScheduledExecutorService scheduledExecutorService;
  private final StatisticConfiguration statisticConfiguration;

  CacheStatisticsManagementProvider(Context parentContext, ScheduledExecutorService scheduledExecutorService, StatisticConfiguration statisticConfiguration) {
    super(ClientCache.class);
    this.parentContext = parentContext;
    this.scheduledExecutorService = scheduledExecutorService;
    this.statisticConfiguration = statisticConfiguration;
  }

  @Override
  protected void dispose(ExposedObject<ClientCache> exposedObject) {
    ((ExposedClientCache) exposedObject).dispose();
  }

  @Override
  public Capability getCapability() {
    StatisticsCapability.Properties properties = new StatisticsCapability.Properties(
        statisticConfiguration.averageWindowDuration(), statisticConfiguration.averageWindowUnit(),
        statisticConfiguration.historySize(),
        statisticConfiguration.historyInterval(), statisticConfiguration.historyIntervalUnit(),
        statisticConfiguration.timeToDisable(), statisticConfiguration.timeToDisableUnit());
    return new StatisticsCapability(getCapabilityName(), properties, getDescriptors(), getCapabilityContext());
  }

  @Override
  public final Collection<? extends Descriptor> getDescriptors() {
    Collection<StatisticDescriptor> capabilities = new HashSet<>();
    for (ExposedObject o : getExposedObjects()) {
      capabilities.addAll(((ExposedClientCache) o).getDescriptors());
    }
    List<StatisticDescriptor> list = new ArrayList<>(capabilities);
    Collections.sort(list, STATISTIC_DESCRIPTOR_COMPARATOR);
    return list;
  }

  @Override
  public Map<String, Statistic<?, ?>> collectStatistics(Context context, Collection<String> statisticNames, long since) {
    Map<String, Statistic<?, ?>> statistics = new TreeMap<String, Statistic<?, ?>>();
    ExposedClientCache exposedClientCache = (ExposedClientCache) findExposedObject(context);
    if (exposedClientCache != null) {
      for (String statisticName : statisticNames) {
        try {
          statistics.put(statisticName, exposedClientCache.queryStatistic(statisticName, since));
        } catch (IllegalArgumentException ignored) {
          // ignore when statisticName does not exist and throws an exception
        }
      }
    }
    return statistics;
  }

  @Override
  protected ExposedObject<ClientCache> wrap(ClientCache managedObject) {

    StatisticsRegistry statisticsRegistry = new StatisticsRegistry(
        managedObject,
        scheduledExecutorService,
        statisticConfiguration.averageWindowDuration(), statisticConfiguration.averageWindowUnit(),
        statisticConfiguration.historySize(),
        statisticConfiguration.historyInterval(), statisticConfiguration.historyIntervalUnit(),
        statisticConfiguration.timeToDisable(), statisticConfiguration.timeToDisableUnit());

    EnumSet<CacheOperationOutcomes.GetOutcome> hit = of(CacheOperationOutcomes.GetOutcome.HIT);
    EnumSet<CacheOperationOutcomes.GetOutcome> miss = of(CacheOperationOutcomes.GetOutcome.MISS);
    OperationStatisticDescriptor<CacheOperationOutcomes.GetOutcome> getCacheStatisticDescriptor = OperationStatisticDescriptor.descriptor("get", singleton("cache"), CacheOperationOutcomes.GetOutcome.class);

    statisticsRegistry.registerCompoundOperations("Cache:Hit", getCacheStatisticDescriptor, hit);
    statisticsRegistry.registerCompoundOperations("Cache:Miss", getCacheStatisticDescriptor, miss);
    statisticsRegistry.registerCompoundOperations("Cache:Clear", OperationStatisticDescriptor.descriptor("clear", singleton("cache"), CacheOperationOutcomes.ClearOutcome.class), allOf(CacheOperationOutcomes.ClearOutcome.class));

    statisticsRegistry.registerRatios("Cache:HitRatio", getCacheStatisticDescriptor, hit, allOf(CacheOperationOutcomes.GetOutcome.class));
    statisticsRegistry.registerRatios("Cache:MissRatio", getCacheStatisticDescriptor, miss, allOf(CacheOperationOutcomes.GetOutcome.class));

    statisticsRegistry.registerSize("Size", descriptor("size", singleton("cache")));

    return new ExposedClientCache(managedObject, parentContext.with("cacheName", managedObject.getName()), statisticsRegistry);
  }


  private static class ExposedClientCache implements ExposedObject<ClientCache> {

    private final ClientCache clientCache;
    private final Context context;
    private final StatisticsRegistry statisticsRegistry;
    private final StatisticsRegistryMetadata statisticsRegistryMetadata;

    ExposedClientCache(ClientCache clientCache, Context context, StatisticsRegistry statisticsRegistry) {
      this.clientCache = clientCache;
      this.context = context;
      this.statisticsRegistry = statisticsRegistry;
      this.statisticsRegistryMetadata = new StatisticsRegistryMetadata(this.statisticsRegistry);
    }

    void dispose() {
      statisticsRegistry.clearRegistrations();
    }

    Statistic<?, ?> queryStatistic(String fullStatisticName, long since) {
      return statisticsRegistryMetadata.queryStatistic(fullStatisticName, since);
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
      return statisticsRegistryMetadata.getDescriptors();
    }
  }
}
