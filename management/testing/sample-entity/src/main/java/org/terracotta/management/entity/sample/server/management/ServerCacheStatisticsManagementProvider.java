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
package org.terracotta.management.entity.sample.server.management;

import org.terracotta.context.ContextManager;
import org.terracotta.context.TreeNode;
import org.terracotta.context.query.Matchers;
import org.terracotta.context.query.Query;
import org.terracotta.management.entity.sample.CacheOperationOutcomes;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.stats.StatisticRegistry;
import org.terracotta.management.registry.Named;
import org.terracotta.management.registry.RequiredContext;
import org.terracotta.management.registry.collect.StatisticProvider;
import org.terracotta.management.service.monitoring.registry.provider.AbstractExposedStatistics;
import org.terracotta.management.service.monitoring.registry.provider.AbstractStatisticsManagementProvider;
import org.terracotta.statistics.OperationStatistic;
import org.terracotta.statistics.StatisticType;
import org.terracotta.statistics.Table;
import org.terracotta.statistics.derived.OperationResultFilter;
import org.terracotta.statistics.derived.latency.MaximumLatencyHistory;
import org.terracotta.statistics.registry.OperationStatisticDescriptor;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.singleton;
import static java.util.EnumSet.allOf;
import static java.util.EnumSet.of;
import static org.terracotta.context.query.Matchers.attributes;
import static org.terracotta.context.query.Matchers.context;
import static org.terracotta.context.query.Matchers.hasAttribute;
import static org.terracotta.context.query.QueryBuilder.queryBuilder;
import static org.terracotta.statistics.registry.ValueStatisticDescriptor.descriptor;

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

      MaximumLatencyHistory latencyHistory = new MaximumLatencyHistory(100, 100, TimeUnit.MILLISECONDS, System::currentTimeMillis);
      OperationStatistic<CacheOperationOutcomes.GetOutcome> getOp = findOperationStatisticOnChildren(binding.getValue(), CacheOperationOutcomes.GetOutcome.class, "get");
      getOp.addDerivedStatistic(new OperationResultFilter<>(EnumSet.of(CacheOperationOutcomes.GetOutcome.HIT, CacheOperationOutcomes.GetOutcome.MISS), latencyHistory));

      getStatisticRegistry().registerStatistic("Cluster:HitCount", get, of(CacheOperationOutcomes.GetOutcome.HIT));
      getStatisticRegistry().registerStatistic("Cluster:MissCount", get, of(CacheOperationOutcomes.GetOutcome.MISS));
      getStatisticRegistry().registerStatistic("Cluster:PutCount", put, of(CacheOperationOutcomes.PutOutcome.SUCCESS));
      getStatisticRegistry().registerStatistic("Cluster:ClearCount", clear, allOf(CacheOperationOutcomes.ClearOutcome.class));

      getStatisticRegistry().registerStatistic("Cluster:GetLatency", latencyHistory);

      getStatisticRegistry().registerStatistic("Size", descriptor("size", singleton("cluster")));

      getStatisticRegistry().registerTable("Cluster:CacheEntryLength", () -> {
        Map<String, String> snapshot = binding.getValue().getData();
        return Table.newBuilder("KeyLength", "ValueLength")
            .withRows(snapshot.keySet(), (key, rowBuilder) -> rowBuilder
                .setStatistic("KeyLength", StatisticType.GAUGE, key.length())
                .setStatistic("ValueLength", StatisticType.COUNTER, snapshot.get(key).length()))
            .build();
      });
    }
  }

  /**
   * Find an operation statistic attached (as a children) to this context that matches the statistic name and type
   *
   * @param context  the context of the query
   * @param type     type of the operation statistic
   * @param statName statistic name
   * @param <T>      type of the operation statistic content
   * @return the operation statistic searched for
   * @throws RuntimeException if 0 or more than 1 result is found
   */
  static <T extends Enum<T>> OperationStatistic<T> findOperationStatisticOnChildren(Object context, Class<T> type, String statName) {
    Query query = queryBuilder()
        .children()
        .filter(context(attributes(Matchers.allOf(hasAttribute("name", statName), hasAttribute("type", type)))))
        .build();

    Set<TreeNode> result = query.execute(Collections.singleton(ContextManager.nodeFor(context)));
    if (result.size() > 1) {
      throw new RuntimeException("result must be unique");
    }
    if (result.isEmpty()) {
      throw new RuntimeException("result must not be null");
    }
    @SuppressWarnings("unchecked")
    OperationStatistic<T> statistic = (OperationStatistic<T>) result.iterator().next().getContext().attributes().get("this");
    return statistic;
  }

}
