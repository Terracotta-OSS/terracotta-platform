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
package org.terracotta.management.model.stats;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.terracotta.management.model.stats.StatisticType.convert;

public class DelegatingStatistic<T extends Serializable> implements Statistic<T> {
  private static final long serialVersionUID = 1L;

  private final org.terracotta.statistics.registry.Statistic<T> delegate;

  public DelegatingStatistic(org.terracotta.statistics.registry.Statistic<T> statistic) {
    this.delegate = statistic;
  }

  public DelegatingStatistic(StatisticType type) {
    this.delegate = new org.terracotta.statistics.registry.Statistic<>(convert(type));
  }

  public DelegatingStatistic(StatisticType type, Sample<T> sample) {
    this.delegate = new org.terracotta.statistics.registry.Statistic<>(convert(type),
        new org.terracotta.statistics.Sample<>(sample.getTimestamp(), sample.getSample()));
  }

  public DelegatingStatistic(StatisticType type, List<Sample<T>> samples) {
    this.delegate = new org.terracotta.statistics.registry.Statistic<>(convert(type), list(samples));
  }

  @Override
  public StatisticType getType() {
    return convert(delegate.getType());
  }

  @Override
  public boolean isEmpty() {
    return delegate.isEmpty();
  }

  @Override
  public List<Sample<T>> getSamples() {
    return delegate.getSamples().stream().map(x -> new DelegatingSample<>(x.getTimestamp(), x.getSample())).collect(Collectors.toList());
  }

  @Override
  public Optional<T> getLatestSampleValue() {
    return delegate.getLatestSampleValue();
  }

  @Override
  public Optional<Sample<T>> getLatestSample() {
    return delegate.getLatestSample().map(x -> new DelegatingSample<>(x.getTimestamp(), x.getSample()));
  }

  private static <R extends Serializable> List<org.terracotta.statistics.Sample<R>> list(List<Sample<R>> samples) {
    return samples.stream().map(x -> new org.terracotta.statistics.Sample<>(x.getTimestamp(), x.getSample())).collect(Collectors.toList());
  }

  public static <U extends Serializable> org.terracotta.statistics.registry.Statistic<U> convertStats(Statistic<U> statistic) {
    return new org.terracotta.statistics.registry.Statistic<>(convert(statistic.getType()), list(statistic.getSamples()));
  }

}
