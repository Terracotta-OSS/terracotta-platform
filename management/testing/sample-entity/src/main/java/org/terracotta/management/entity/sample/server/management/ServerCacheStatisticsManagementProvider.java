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
import org.terracotta.context.extended.StatisticsRegistry;
import org.terracotta.management.entity.sample.CacheOperationOutcomes;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.registry.action.Named;
import org.terracotta.management.registry.action.RequiredContext;
import org.terracotta.management.registry.collect.StatisticConfiguration;
import org.terracotta.management.service.monitoring.registry.provider.AbstractExposedStatistics;
import org.terracotta.management.service.monitoring.registry.provider.AbstractStatisticsManagementProvider;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.EnumSet.allOf;
import static java.util.EnumSet.of;
import static org.terracotta.context.extended.ValueStatisticDescriptor.descriptor;

@Named("ServerCacheStatistics")
@RequiredContext({@Named("consumerId"), @Named("type"), @Named("alias")})
class ServerCacheStatisticsManagementProvider extends AbstractStatisticsManagementProvider<ServerCacheBinding> {

  ServerCacheStatisticsManagementProvider(StatisticConfiguration statisticConfiguration) {
    super(ServerCacheBinding.class, statisticConfiguration);
  }

  @Override
  protected AbstractExposedStatistics<ServerCacheBinding> internalWrap(Context context, ServerCacheBinding managedObject, StatisticsRegistry statisticsRegistry) {
    return new ServerCacheExposedStatistics(context, managedObject, statisticsRegistry);
  }

  private static class ServerCacheExposedStatistics extends AbstractExposedStatistics<ServerCacheBinding> {

    ServerCacheExposedStatistics(Context context, ServerCacheBinding binding, StatisticsRegistry statisticsRegistry) {
      super(context, binding, statisticsRegistry);

      EnumSet<CacheOperationOutcomes.GetOutcome> hit = of(CacheOperationOutcomes.GetOutcome.HIT);
      EnumSet<CacheOperationOutcomes.GetOutcome> miss = of(CacheOperationOutcomes.GetOutcome.MISS);
      OperationStatisticDescriptor<CacheOperationOutcomes.GetOutcome> getCacheStatisticDescriptor = OperationStatisticDescriptor.descriptor("get", singleton("cluster"), CacheOperationOutcomes.GetOutcome.class);

      statisticsRegistry.registerCompoundOperations("Cluster:Hit", getCacheStatisticDescriptor, hit);
      statisticsRegistry.registerCompoundOperations("Cluster:Miss", getCacheStatisticDescriptor, miss);
      statisticsRegistry.registerCompoundOperations("Cluster:Clear", OperationStatisticDescriptor.descriptor("clear", singleton("cluster"), CacheOperationOutcomes.ClearOutcome.class), allOf(CacheOperationOutcomes.ClearOutcome.class));

      statisticsRegistry.registerRatios("Cluster:HitRatio", getCacheStatisticDescriptor, hit, allOf(CacheOperationOutcomes.GetOutcome.class));
      statisticsRegistry.registerRatios("Cluster:MissRatio", getCacheStatisticDescriptor, miss, allOf(CacheOperationOutcomes.GetOutcome.class));

      statisticsRegistry.registerSize("Size", descriptor("size", singleton("cluster")));
    }

    @Override
    public Context getContext() {
      return super.getContext().with("type", "ServerCache");
    }

  }

  private static Set<String> tags(String... tags) {return new HashSet<>(asList(tags));}

}
