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
package org.terracotta.management.service.monitoring.registry;

import org.terracotta.context.extended.StatisticsRegistry;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.registry.action.Named;
import org.terracotta.management.registry.action.RequiredContext;
import org.terracotta.management.service.monitoring.registry.provider.AbstractExposedStatistics;
import org.terracotta.management.service.monitoring.registry.provider.AbstractStatisticsManagementProvider;

import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.terracotta.context.extended.ValueStatisticDescriptor.descriptor;

@Named("OffHeapResourceStatistics")
@RequiredContext({@Named("consumerId"), @Named("type"), @Named("alias")})
public class OffHeapResourceStatisticsManagementProvider extends AbstractStatisticsManagementProvider<OffHeapResourceBinding> {

  public OffHeapResourceStatisticsManagementProvider() {
    super(OffHeapResourceBinding.class);
  }

  @Override
  protected AbstractExposedStatistics<OffHeapResourceBinding> internalWrap(Context context, OffHeapResourceBinding managedObject, StatisticsRegistry statisticsRegistry) {
    return new OffHeapResourceBindingExposedStatistics(context, managedObject, statisticsRegistry);
  }

  private static class OffHeapResourceBindingExposedStatistics extends AbstractExposedStatistics<OffHeapResourceBinding> {

    OffHeapResourceBindingExposedStatistics(Context context, OffHeapResourceBinding binding, StatisticsRegistry statisticsRegistry) {
      super(context, binding, statisticsRegistry);

      statisticsRegistry.registerSize("AllocatedMemory", descriptor("allocatedMemory", tags("tier", "OffHeapResource")));
    }

    @Override
    public Context getContext() {
      return super.getContext().with("type", "OffHeapResource");
    }

  }

  private static Set<String> tags(String... tags) {return new HashSet<>(asList(tags));}

}
