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
package org.terracotta.management.model.stats;

import org.terracotta.management.model.capabilities.descriptors.StatisticDescriptor;
import org.terracotta.statistics.StatisticType;
import org.terracotta.statistics.ValueStatistic;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BinaryOperator;
import java.util.function.LongSupplier;

import static java.util.stream.Collectors.toMap;

/***
 * @author Mathieu Carbou
 */
public class StatisticRegistry extends org.terracotta.statistics.registry.StatisticRegistry {

  public StatisticRegistry(Object contextObject, LongSupplier timeSource) {
    super(contextObject, timeSource);
  }

  public Collection<StatisticDescriptor> getDescriptors() {
    Set<StatisticDescriptor> descriptors = new HashSet<>(getStatistics().size());
    for (Map.Entry<String, ValueStatistic<? extends Serializable>> entry : getStatistics().entrySet()) {
      String fullStatName = entry.getKey();
      StatisticType type = entry.getValue().type();
      descriptors.add(new StatisticDescriptor(fullStatName, type.name()));
    }
    return descriptors;
  }

  @SuppressWarnings("rawtypes")
  public static Map<String, Statistic<? extends Serializable>> collect(StatisticRegistry registry, Collection<String> statisticNames, long since) {
    if (statisticNames == null || statisticNames.isEmpty()) {
      return registry.queryStatistics(since)
          .entrySet()
          .stream()
          .filter(e -> !e.getValue().isEmpty())
          .map(x -> new Map.Entry<String, Statistic<? extends Serializable>>() {
            @Override
            public String getKey() {
              return x.getKey();
            }

            @Override
            public Statistic<? extends Serializable> getValue() {
              return new DelegatingStatistic<>(x.getValue());
            }

            @Override
            public Statistic<? extends Serializable> setValue(Statistic<? extends Serializable> value) {
              throw new UnsupportedOperationException("Can't set value");
            }
          })
          .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, throwingMerger(), TreeMap::new));
    } else {
      return statisticNames.stream()
          .map(name -> new AbstractMap.SimpleEntry<>(name, registry.queryStatistic(name, since)))
          .filter(e -> e.getValue().isPresent() && !e.getValue().get().isEmpty())
          .collect(toMap(Map.Entry::getKey, e -> new DelegatingStatistic<>(e.getValue().get()), throwingMerger(), TreeMap::new));
    }
  }

  private static <T> BinaryOperator<T> throwingMerger() {
    return (u, v) -> {
      throw new IllegalStateException(String.format("Duplicate key %s", u));
    };
  }

}
