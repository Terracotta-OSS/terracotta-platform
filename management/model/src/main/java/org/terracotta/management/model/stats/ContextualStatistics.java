/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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

import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.context.Contextual;
import org.terracotta.statistics.Sample;
import org.terracotta.statistics.registry.Statistic;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.util.stream.Collectors.toMap;

/**
 * This class holds the statistics queried on a specific cluster element (context
 *
 * @author Mathieu Carbou
 */
public final class ContextualStatistics implements Contextual {

  private static final long serialVersionUID = 1;

  private final Map<String, Statistic<? extends Serializable>> statistics;
  private final String capability;
  private Context context;

  public ContextualStatistics(String capability, Context context, Map<String, Statistic<? extends Serializable>> statistics) {
    this.statistics = new HashMap<>(Objects.requireNonNull(statistics));
    this.context = Objects.requireNonNull(context);
    this.capability = Objects.requireNonNull(capability);
  }

  public String getCapability() {
    return capability;
  }

  public int size() {return statistics.size();}

  public boolean isEmpty() {return statistics.isEmpty();}

  public Map<String, Statistic<? extends Serializable>> getStatistics() {
    return statistics;
  }

  public Map<String, ? extends Serializable> getLatestSampleValues() {
    return statistics.entrySet()
        .stream()
        .filter(e -> !e.getValue().isEmpty())
        .collect(toMap(Map.Entry::getKey, e -> e.getValue().getLatestSampleValue().get()));
  }

  public Map<String, Sample<? extends Serializable>> getLatestSamples() {
    return statistics.entrySet()
        .stream()
        .filter(e -> !e.getValue().isEmpty())
        .collect(toMap(Map.Entry::getKey, e -> e.getValue().getLatestSample().get()));
  }

  public boolean hasStatistic(String name) {
    return statistics.containsKey(name);
  }

  /**
   * Returns the statistic for a specific name
   *
   * @param name The name of the statistic to return
   * @return The statistic found
   */
  @SuppressWarnings("unchecked")
  public <T extends Serializable> Optional<Statistic<T>> getStatistic(String name) {
    return Optional.ofNullable((Statistic<T>) statistics.get(name));
  }

  public <T extends Serializable> Optional<T> getLatestSampleValue(String name) {
    return this.<T>getLatestSample(name).map(Sample::getSample);
  }

  public <T extends Serializable> Optional<Sample<T>> getLatestSample(String name) {
    return this.<T>getStatistic(name).flatMap(Statistic::getLatestSample);
  }

  @Override
  public void setContext(Context context) {
    this.context = Objects.requireNonNull(context);
  }

  @Override
  public Context getContext() {
    return context;
  }

  @Override
  public String toString() {
    return "ContextualStatistics{" +
        "capability='" + capability + '\'' +
        ", context=" + context +
        ", statistics=" + statistics.size() +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ContextualStatistics that = (ContextualStatistics) o;

    if (!statistics.equals(that.statistics)) return false;
    if (!capability.equals(that.capability)) return false;
    return context.equals(that.context);

  }

  @Override
  public int hashCode() {
    int result = statistics.hashCode();
    result = 31 * result + capability.hashCode();
    result = 31 * result + context.hashCode();
    return result;
  }

}
