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
import org.terracotta.management.model.context.Context;
import org.terracotta.management.registry.DefaultStatisticsExposedObject;
import org.terracotta.management.registry.DefaultStatisticsManagementProvider;
import org.terracotta.management.registry.Named;
import org.terracotta.management.registry.RequiredContext;

import static java.util.Collections.singleton;
import static java.util.EnumSet.allOf;
import static java.util.EnumSet.of;

/**
 * @author Mathieu Carbou
 */
@Named("CacheStatistics")
@RequiredContext({@Named("appName"), @Named("cacheName")})
class CacheStatisticsManagementProvider extends DefaultStatisticsManagementProvider<ClientCache> {

  private final Context parentContext;

  CacheStatisticsManagementProvider(Context parentContext) {
    super(ClientCache.class);
    this.parentContext = parentContext;
  }

  @Override
  protected ExposedClientCache wrap(ClientCache managedObject) {
    return new ExposedClientCache(managedObject, parentContext.with("cacheName", managedObject.getName()));
  }

  private static class ExposedClientCache extends DefaultStatisticsExposedObject<ClientCache> {

    ExposedClientCache(ClientCache clientCache, Context context) {
      super(clientCache, context);
      
      OperationStatisticDescriptor<CacheOperationOutcomes.GetOutcome> get = OperationStatisticDescriptor.descriptor("get", singleton("cache"), CacheOperationOutcomes.GetOutcome.class);
      OperationStatisticDescriptor<CacheOperationOutcomes.ClearOutcome> clear = OperationStatisticDescriptor.descriptor("clear", singleton("cache"), CacheOperationOutcomes.ClearOutcome.class);

      statisticRegistry.registerCounter("Cache:HitCount", get, of(CacheOperationOutcomes.GetOutcome.HIT));
      statisticRegistry.registerCounter("Cache:MissCount", get, of(CacheOperationOutcomes.GetOutcome.MISS));
      statisticRegistry.registerCounter("Cache:ClearCount", clear, allOf(CacheOperationOutcomes.ClearOutcome.class));

      statisticRegistry.registerSize("Size", ValueStatisticDescriptor.descriptor("size", singleton("cache")));
    }
  }
}
