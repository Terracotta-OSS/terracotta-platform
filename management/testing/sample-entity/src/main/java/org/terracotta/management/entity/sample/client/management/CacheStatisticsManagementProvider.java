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
package org.terracotta.management.entity.sample.client.management;

import org.terracotta.management.entity.sample.CacheOperationOutcomes;
import org.terracotta.management.entity.sample.client.ClientCache;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.registry.DefaultStatisticsExposedObject;
import org.terracotta.management.registry.DefaultStatisticsManagementProvider;
import org.terracotta.management.registry.Named;
import org.terracotta.management.registry.RequiredContext;
import org.terracotta.statistics.registry.OperationStatisticDescriptor;
import org.terracotta.statistics.registry.ValueStatisticDescriptor;

import java.util.function.LongSupplier;

import static java.util.Collections.singleton;
import static java.util.EnumSet.allOf;
import static java.util.EnumSet.of;

/**
 * @author Mathieu Carbou
 */
@Named("CacheStatistics")
@RequiredContext({@Named("instanceId"), @Named("appName"), @Named("cacheName")})
class CacheStatisticsManagementProvider extends DefaultStatisticsManagementProvider<ClientCache> {

  private final Context parentContext;

  CacheStatisticsManagementProvider(Context parentContext, LongSupplier timeSource) {
    super(ClientCache.class, timeSource);
    this.parentContext = parentContext;
  }

  @Override
  protected ExposedClientCache wrap(ClientCache managedObject) {
    return new ExposedClientCache(managedObject, timeSource, parentContext.with("cacheName", managedObject.getName()));
  }

  private static class ExposedClientCache extends DefaultStatisticsExposedObject<ClientCache> {

    ExposedClientCache(ClientCache clientCache, LongSupplier timeSource, Context context) {
      super(clientCache, timeSource, context);

      OperationStatisticDescriptor<CacheOperationOutcomes.GetOutcome> get = OperationStatisticDescriptor.descriptor("get", singleton("cache"), CacheOperationOutcomes.GetOutcome.class);
      OperationStatisticDescriptor<CacheOperationOutcomes.ClearOutcome> clear = OperationStatisticDescriptor.descriptor("clear", singleton("cache"), CacheOperationOutcomes.ClearOutcome.class);

      statisticRegistry.registerStatistic("Cache:HitCount", get, of(CacheOperationOutcomes.GetOutcome.HIT));
      statisticRegistry.registerStatistic("Cache:MissCount", get, of(CacheOperationOutcomes.GetOutcome.MISS));
      statisticRegistry.registerStatistic("Cache:ClearCount", clear, allOf(CacheOperationOutcomes.ClearOutcome.class));

      statisticRegistry.registerStatistic("Size", ValueStatisticDescriptor.descriptor("size", singleton("cache")));
    }
  }
}
