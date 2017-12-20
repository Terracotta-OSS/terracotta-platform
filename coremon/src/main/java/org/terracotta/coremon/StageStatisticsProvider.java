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
package org.terracotta.coremon;

import org.terracotta.management.model.context.Context;
import org.terracotta.management.registry.Named;
import org.terracotta.management.registry.RequiredContext;
import org.terracotta.management.registry.collect.StatisticProvider;
import org.terracotta.management.registry.collect.StatisticRegistry;
import org.terracotta.management.service.monitoring.registry.provider.AbstractExposedStatistics;
import org.terracotta.management.service.monitoring.registry.provider.AbstractStatisticsManagementProvider;

@Named("StageStatistics")
@RequiredContext({@Named("alias")})
@StatisticProvider
public class StageStatisticsProvider extends AbstractStatisticsManagementProvider<StageBinding> {

  public StageStatisticsProvider() {
    super(StageBinding.class);
  }

  @Override
  protected AbstractExposedStatistics<StageBinding> internalWrap(Context context, StageBinding managedObject, StatisticRegistry statisticRegistry) {
    return new StageStatisticsBindingExposedStatistics(context, managedObject, statisticRegistry);
  }

  private static class StageStatisticsBindingExposedStatistics extends AbstractExposedStatistics<StageBinding> {
    StageStatisticsBindingExposedStatistics(Context context, StageBinding binding, StatisticRegistry statisticRegistry) {
      super(context, binding, statisticRegistry);

      getRegistry().registerSize("Stage:CurrentQueueSize", binding::fetchCurrentQueueSize);
      getRegistry().registerCounter("Stage:TotalQueuedCount", binding::fetchTotalQueuedCount);
    }
  }
}
