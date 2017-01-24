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
package org.terracotta.management.entity.sample.server.management;

import org.terracotta.context.extended.OperationStatisticDescriptor;
import org.terracotta.management.entity.sample.CacheOperationOutcomes;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.registry.Named;
import org.terracotta.management.registry.RequiredContext;
import org.terracotta.management.registry.collect.StatisticProvider;
import org.terracotta.management.registry.collect.StatisticRegistry;
import org.terracotta.management.service.monitoring.registry.provider.AbstractExposedStatistics;
import org.terracotta.management.service.monitoring.registry.provider.AbstractStatisticsManagementProvider;

import static java.util.Collections.singleton;
import static java.util.EnumSet.allOf;
import static java.util.EnumSet.of;
import static org.terracotta.context.extended.ValueStatisticDescriptor.descriptor;

@Named("ServerCacheStatistics")
@RequiredContext({@Named("consumerId"), @Named("type"), @Named("alias")})
@StatisticProvider
class ServerCacheStatisticsManagementProvider extends AbstractStatisticsManagementProvider<ServerCacheBinding> {

  ServerCacheStatisticsManagementProvider() {
    super(ServerCacheBinding.class);
  }

  @Override
  protected AbstractExposedStatistics<ServerCacheBinding> internalWrap(Context context, ServerCacheBinding managedObject, StatisticRegistry statisticRegistry) {
    return new ServerCacheExposedStatistics(context, managedObject, statisticRegistry);
  }

  private static class ServerCacheExposedStatistics extends AbstractExposedStatistics<ServerCacheBinding> {

    ServerCacheExposedStatistics(Context context, ServerCacheBinding binding, StatisticRegistry statisticRegistry) {
      super(context.with("type", "ServerCache"), binding, statisticRegistry);

      OperationStatisticDescriptor<CacheOperationOutcomes.GetOutcome> get = OperationStatisticDescriptor.descriptor("get", singleton("cluster"), CacheOperationOutcomes.GetOutcome.class);
      OperationStatisticDescriptor<CacheOperationOutcomes.PutOutcome> put = OperationStatisticDescriptor.descriptor("put", singleton("cluster"), CacheOperationOutcomes.PutOutcome.class);
      OperationStatisticDescriptor<CacheOperationOutcomes.ClearOutcome> clear = OperationStatisticDescriptor.descriptor("clear", singleton("cluster"), CacheOperationOutcomes.ClearOutcome.class);

      getRegistry().registerCounter("Cluster:HitCount", get, of(CacheOperationOutcomes.GetOutcome.HIT));
      getRegistry().registerCounter("Cluster:MissCount", get, of(CacheOperationOutcomes.GetOutcome.MISS));
      getRegistry().registerCounter("Cluster:PutCount", put, of(CacheOperationOutcomes.PutOutcome.SUCCESS));
      getRegistry().registerCounter("Cluster:ClearCount", clear, allOf(CacheOperationOutcomes.ClearOutcome.class));

      getRegistry().registerSize("Size", descriptor("size", singleton("cluster")));
    }
  }

}
