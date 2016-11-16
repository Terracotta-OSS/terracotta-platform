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

import org.terracotta.context.extended.RegisteredStatistic;
import org.terracotta.context.extended.StatisticsRegistry;
import org.terracotta.management.model.capabilities.descriptors.Descriptor;
import org.terracotta.management.model.capabilities.descriptors.StatisticDescriptor;
import org.terracotta.management.model.stats.MemoryUnit;
import org.terracotta.management.model.stats.NumberUnit;
import org.terracotta.management.model.stats.Sample;
import org.terracotta.management.model.stats.Statistic;
import org.terracotta.management.model.stats.StatisticType;
import org.terracotta.management.model.stats.history.AverageHistory;
import org.terracotta.management.model.stats.history.CounterHistory;
import org.terracotta.management.model.stats.history.DurationHistory;
import org.terracotta.management.model.stats.history.RateHistory;
import org.terracotta.management.model.stats.history.RatioHistory;
import org.terracotta.management.model.stats.history.SizeHistory;
import org.terracotta.statistics.archive.Timestamped;
import org.terracotta.statistics.extended.SampleType;
import org.terracotta.statistics.extended.SampledStatistic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author Mathieu Carbou
 */
public class StatisticsRegistryMetadata {

  private static final Map<String, SampleType> COMPOUND_SUFFIXES = new HashMap<String, SampleType>();

  static {
    COMPOUND_SUFFIXES.put("Count", SampleType.COUNTER);
    COMPOUND_SUFFIXES.put("Rate", SampleType.RATE);
    COMPOUND_SUFFIXES.put("LatencyMinimum", SampleType.LATENCY_MIN);
    COMPOUND_SUFFIXES.put("LatencyMaximum", SampleType.LATENCY_MAX);
    COMPOUND_SUFFIXES.put("LatencyAverage", SampleType.LATENCY_AVG);
  }

  private final StatisticsRegistry statisticsRegistry;

  public StatisticsRegistryMetadata(StatisticsRegistry statisticsRegistry) {
    this.statisticsRegistry = statisticsRegistry;
  }

  public Statistic<?, ?> queryStatistic(String fullStatisticName, long since) {
    if (statisticsRegistry != null) {

      // first search for a non-compound stat
      SampledStatistic<? extends Number> statistic = statisticsRegistry.findSampledStatistic(fullStatisticName);

      // if not found, it can be a compound stat, so search for it
      if (statistic == null) {
        for (Iterator<Map.Entry<String, SampleType>> it = COMPOUND_SUFFIXES.entrySet().iterator(); it.hasNext() && statistic == null; ) {
          Map.Entry<String, SampleType> entry = it.next();
          if (fullStatisticName.endsWith(entry.getKey())) {
            String statisticName = fullStatisticName.substring(0, fullStatisticName.length() - entry.getKey().length());
            statistic = statisticsRegistry.findSampledCompoundStatistic(statisticName, entry.getValue());
          }
        }
      }

      if (statistic != null) {
        List<? extends Timestamped<? extends Number>> history = statistic.history(since);
        switch (statistic.type()) {
          case COUNTER: return new CounterHistory(buildSamples(history, Long.class), NumberUnit.COUNT);
          case RATE: return new RateHistory(buildSamples(history, Double.class), TimeUnit.SECONDS);
          case LATENCY_MIN: return new DurationHistory(buildSamples(history, Long.class), TimeUnit.NANOSECONDS);
          case LATENCY_MAX: return new DurationHistory(buildSamples(history, Long.class), TimeUnit.NANOSECONDS);
          case LATENCY_AVG: return new AverageHistory(buildSamples(history, Double.class), TimeUnit.NANOSECONDS);
          case RATIO: return new RatioHistory(buildSamples(history, Double.class), NumberUnit.RATIO);
          case SIZE: return new SizeHistory(buildSamples(history, Long.class), MemoryUnit.B);
          default: throw new UnsupportedOperationException(statistic.type().name());
        }
      }
    }
    throw new IllegalArgumentException("No registered statistic named '" + fullStatisticName + "'");
  }

  private <T extends Number> List<Sample<T>> buildSamples(List<? extends Timestamped<? extends Number>> history, Class<T> type) {
    List<Sample<T>> samples = new ArrayList<Sample<T>>(history.size());
    for (Timestamped<? extends Number> t : history) {
      samples.add(new Sample<T>(t.getTimestamp(), type.cast(t.getSample())));
    }
    return samples;
  }

  public Collection<Descriptor> getDescriptors() {
    Set<Descriptor> capabilities = new HashSet<Descriptor>();

    if (statisticsRegistry != null) {
      Map<String, RegisteredStatistic> registrations = statisticsRegistry.getRegistrations();
      for (Map.Entry<String, RegisteredStatistic> entry : registrations.entrySet()) {
        String statisticName = entry.getKey();
        RegisteredStatistic registeredStatistic = registrations.get(statisticName);
        switch (registeredStatistic.getType()) {
          case COUNTER:
            capabilities.add(new StatisticDescriptor(statisticName, StatisticType.COUNTER_HISTORY));
            break;
          case RATIO:
            capabilities.add(new StatisticDescriptor(entry.getKey(), StatisticType.RATIO_HISTORY));
            break;
          case SIZE:
            capabilities.add(new StatisticDescriptor(statisticName, StatisticType.SIZE_HISTORY));
            break;
          case COMPOUND:
            capabilities.add(new StatisticDescriptor(entry.getKey() + "Count", StatisticType.COUNTER_HISTORY));
            capabilities.add(new StatisticDescriptor(entry.getKey() + "Rate", StatisticType.RATE_HISTORY));
            capabilities.add(new StatisticDescriptor(entry.getKey() + "LatencyMinimum", StatisticType.DURATION_HISTORY));
            capabilities.add(new StatisticDescriptor(entry.getKey() + "LatencyMaximum", StatisticType.DURATION_HISTORY));
            capabilities.add(new StatisticDescriptor(entry.getKey() + "LatencyAverage", StatisticType.AVERAGE_HISTORY));
            break;
          default:
            throw new UnsupportedOperationException(registeredStatistic.getType().name());
        }
      }
    }

    return capabilities;
  }

}
