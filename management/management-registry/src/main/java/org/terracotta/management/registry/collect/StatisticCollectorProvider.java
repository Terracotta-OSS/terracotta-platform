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
package org.terracotta.management.registry.collect;

import org.terracotta.management.model.context.Context;
import org.terracotta.management.registry.action.AbstractActionManagementProvider;
import org.terracotta.management.registry.action.Exposed;
import org.terracotta.management.registry.action.ExposedObject;
import org.terracotta.management.registry.action.Named;

import java.util.Collection;

/**
 * @author Mathieu Carbou
 */
@Named("StatisticCollectorCapability")
public class StatisticCollectorProvider<T extends StatisticCollector> extends AbstractActionManagementProvider<T> {

  private final Context context;

  public StatisticCollectorProvider(Class<? extends T> type, Context context) {
    super(type);
    this.context = context;
  }

  @Override
  protected ExposedObject<T> wrap(T managedObject) {
    return new ExposedStatisticCollector<T>(managedObject, context);
  }

  public static class ExposedStatisticCollector<T extends StatisticCollector> implements ExposedObject<T> {

    private final T collectorService;
    private final Context context;

    public ExposedStatisticCollector(T collectorService, Context context) {
      this.collectorService = collectorService;
      this.context = context;
    }

    @Exposed
    public void updateCollectedStatistics(@Named("capabilityName") String capabilityName,
                                          @Named("statisticNames") Collection<String> statisticNames) {
      collectorService.updateCollectedStatistics(capabilityName, statisticNames);
    }

    @Exposed
    public void stopStatisticCollector() {
      collectorService.stopStatisticCollector();
    }

    @Exposed
    public void startStatisticCollector() {
      collectorService.startStatisticCollector();
    }

    @Override
    public T getTarget() {
      return collectorService;
    }

    @Override
    public ClassLoader getClassLoader() {
      return collectorService.getClass().getClassLoader();
    }

    @Override
    public boolean matches(Context context) {
      return context.contains(this.context);
    }
  }

}
