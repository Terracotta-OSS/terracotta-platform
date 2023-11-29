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
package org.terracotta.management.service.monitoring.registry.provider;

import org.terracotta.management.model.capabilities.descriptors.Descriptor;
import org.terracotta.management.model.capabilities.descriptors.StatisticDescriptor;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.stats.Statistic;
import org.terracotta.management.model.stats.StatisticRegistry;
import org.terracotta.management.registry.ExposedObject;
import org.terracotta.management.registry.Named;
import org.terracotta.management.registry.RequiredContext;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RequiredContext({@Named("consumerId")})
public abstract class AbstractStatisticsManagementProvider<T extends AliasBinding> extends AliasBindingManagementProvider<T> {

  public AbstractStatisticsManagementProvider(Class<? extends T> type) {
    super(type);
  }

  @Override
  protected void dispose(ExposedObject<T> exposedObject) {
    ((AbstractExposedStatistics<T>) exposedObject).close();
  }

  @SuppressWarnings("unchecked")
  @Override
  public final Collection<? extends Descriptor> getDescriptors() {
    // To keep ordering because these objects end up in an immutable
    // topology so this is easier for testing to compare with json payloads
    List<StatisticDescriptor> list = new ArrayList<>((Collection<? extends StatisticDescriptor>) super.getDescriptors());
    list.sort(STATISTIC_DESCRIPTOR_COMPARATOR);
    return list;
  }

  @Override
  public Map<String, Statistic<? extends Serializable>> collectStatistics(Context context, Collection<String> statisticNames, long since) {
    AbstractExposedStatistics<T> exposedObject = (AbstractExposedStatistics<T>) findExposedObject(context);
    if (exposedObject == null) {
      return Collections.emptyMap();
    }
    return StatisticRegistry.collect(exposedObject.getStatisticRegistry(), statisticNames, since);
  }

  @Override
  protected AbstractExposedStatistics<T> wrap(T managedObject) {
    Context context = Context.empty()
        .with("consumerId", String.valueOf(getMonitoringService().getConsumerId()))
        .with("alias", managedObject.getAlias());
    StatisticRegistry statisticRegistry = getStatisticRegistry(managedObject);
    return internalWrap(context, managedObject, statisticRegistry);
  }

  protected StatisticRegistry getStatisticRegistry(T managedObject) {
    return new StatisticRegistry(managedObject.getValue(), getTimeSource()::getTimestamp);
  }

  @Override
  protected final AbstractExposedStatistics<T> internalWrap(Context context, T managedObject) {
    throw new UnsupportedOperationException();
  }

  protected abstract AbstractExposedStatistics<T> internalWrap(Context context, T managedObject, StatisticRegistry statisticsRegistry);

}
