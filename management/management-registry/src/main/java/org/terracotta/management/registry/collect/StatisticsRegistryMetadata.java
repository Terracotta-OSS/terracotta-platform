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
import org.terracotta.management.model.capabilities.descriptors.StatisticDescriptor;
import org.terracotta.management.model.stats.MemoryUnit;
import org.terracotta.management.model.stats.NumberUnit;
import org.terracotta.management.model.stats.Sample;
import org.terracotta.management.model.stats.Statistic;
import org.terracotta.management.model.stats.StatisticType;
import org.terracotta.management.model.stats.history.CounterHistory;
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
    //TODO: remove "Rate" suffix when rates will be computed externally (https://github.com/ehcache/ehcache3/issues/1684)
    COMPOUND_SUFFIXES.put("Rate", SampleType.RATE);
    //TODO: cleanup unused compound and sample types for https://github.com/Terracotta-OSS/terracotta-platform/issues/217
    //COMPOUND_SUFFIXES.put("LatencyMinimum", SampleType.LATENCY_MIN);
    //COMPOUND_SUFFIXES.put("LatencyMaximum", SampleType.LATENCY_MAX);
    //COMPOUND_SUFFIXES.put("LatencyAverage", SampleType.LATENCY_AVG);
  }

  private final StatisticsRegistry statisticsRegistry;

  public StatisticsRegistryMetadata(StatisticsRegistry statisticsRegistry) {
    this.statisticsRegistry = statisticsRegistry;
  }

  //TODO: Directly query Statistic context tree instead of stat registry (cyclic buffer): https://github.com/Terracotta-OSS/terracotta-platform/issues/217
  public Statistic<?, ?> queryStatistic(String fullStatisticName, long since) {
    if (statisticsRegistry != null) {

      // first search for a non-compound stat
      SampledStatistic<? extends Number> statistic = statisticsRegistry.findSampledStatistic(fullStatisticName);

      // TODO: remove compound since we won't need it after https://github.com/Terracotta-OSS/terracotta-platform/issues/217
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
          case COUNTER: return new CounterHistory(buildNaturalSamples(history), NumberUnit.COUNT);
          case RATE: return new RateHistory(buildDecimalSamples(history), TimeUnit.SECONDS);
          case RATIO: return new RatioHistory(buildDecimalSamples(history), NumberUnit.RATIO);
          case SIZE: return new SizeHistory(buildNaturalSamples(history), MemoryUnit.B);
          default: throw new UnsupportedOperationException(statistic.type().name());
        }
      }
    }
    throw new IllegalArgumentException("No registered statistic named '" + fullStatisticName + "'");
  }

  private List<Sample<Double>> buildDecimalSamples(List<? extends Timestamped<? extends Number>> history) {
    List<Sample<Double>> samples = new ArrayList<Sample<Double>>(history.size());
    for (Timestamped<? extends Number> t : history) {
      samples.add(new Sample<Double>(t.getTimestamp(), t.getSample() == null ? null : t.getSample().doubleValue()));
    }
    return samples;
  }

  private List<Sample<Long>> buildNaturalSamples(List<? extends Timestamped<? extends Number>> history) {
    List<Sample<Long>> samples = new ArrayList<Sample<Long>>(history.size());
    for (Timestamped<? extends Number> t : history) {
      samples.add(new Sample<Long>(t.getTimestamp(), t.getSample() == null ? null : t.getSample().longValue()));
    }
    return samples;
  }

  public Collection<StatisticDescriptor> getDescriptors() {
    Set<StatisticDescriptor> capabilities = new HashSet<StatisticDescriptor>();

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
            // TODO: remove compound since we won't need it after https://github.com/Terracotta-OSS/terracotta-platform/issues/217
          case COMPOUND:
            capabilities.add(new StatisticDescriptor(entry.getKey() + "Count", StatisticType.COUNTER_HISTORY));
            //TODO: remove "Rate" suffix when rates will be computed externally (https://github.com/ehcache/ehcache3/issues/1684)
            capabilities.add(new StatisticDescriptor(entry.getKey() + "Rate", StatisticType.RATE_HISTORY));
            //TODO: cleanup unused compound and sample types for https://github.com/Terracotta-OSS/terracotta-platform/issues/217
            //capabilities.add(new StatisticDescriptor(entry.getKey() + "LatencyMinimum", StatisticType.DURATION_HISTORY));
            //capabilities.add(new StatisticDescriptor(entry.getKey() + "LatencyMaximum", StatisticType.DURATION_HISTORY));
            //capabilities.add(new StatisticDescriptor(entry.getKey() + "LatencyAverage", StatisticType.AVERAGE_HISTORY));
            break;
          default:
            throw new UnsupportedOperationException(registeredStatistic.getType().name());
        }
      }
    }

    return capabilities;
  }

}
