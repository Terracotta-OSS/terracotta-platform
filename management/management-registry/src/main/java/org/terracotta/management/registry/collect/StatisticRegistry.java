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

import org.terracotta.management.model.capabilities.descriptors.StatisticDescriptor;
import org.terracotta.statistics.SampledStatistic;
import org.terracotta.statistics.StatisticType;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/***
 * @author Mathieu Carbou
 */
public class StatisticRegistry extends org.terracotta.statistics.registry.StatisticRegistry {

  public StatisticRegistry(Object contextObject, Supplier<Long> timeSource) {
    super(contextObject, timeSource);
  }

  public StatisticRegistry(Object contextObject) {
    super(contextObject);
  }

  public Collection<StatisticDescriptor> getDescriptors() {
    Set<StatisticDescriptor> descriptors = new HashSet<>(getStatistics().size());
    for (Map.Entry<String, SampledStatistic<? extends Serializable>> entry : getStatistics().entrySet()) {
      String fullStatName = entry.getKey();
      StatisticType type = entry.getValue().type();
      descriptors.add(new StatisticDescriptor(fullStatName, type.name()));
    }
    return descriptors;
  }
}
