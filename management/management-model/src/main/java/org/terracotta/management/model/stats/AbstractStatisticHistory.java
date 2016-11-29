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

import org.terracotta.management.model.Objects;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * @author Mathieu Carbou
 */
public abstract class AbstractStatisticHistory<V, U> implements StatisticHistory<V, U>, Serializable {

  private static final long serialVersionUID = 1;

  private final Sample<V>[] values;
  private final U unit;

  @SuppressWarnings("unchecked")
  public AbstractStatisticHistory(U unit, List<Sample<V>> values) {
    this.values = Objects.requireNonNull(values).toArray(new Sample[values.size()]);
    this.unit = Objects.requireNonNull(unit);
  }

  public AbstractStatisticHistory(U unit, Sample<V>... values) {
    this.values = Objects.requireNonNull(values);
    this.unit = Objects.requireNonNull(unit);
  }

  @Override
  public final U getUnit() {
    return unit;
  }

  @Override
  public final Sample<V>[] getValue() {
    return values;
  }

  @Override
  public String toString() {
    return "{" + "type='" + getClass().getSimpleName() + '\'' + ", value=" + Arrays.toString(values) + ", unit=" + getUnit() + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AbstractStatisticHistory<?, ?> that = (AbstractStatisticHistory<?, ?>) o;
    if (!Arrays.equals(values, that.values)) return false;
    return unit.equals(that.unit);
  }

  @Override
  public int hashCode() {
    int result = Arrays.hashCode(values);
    result = 31 * result + unit.hashCode();
    return result;
  }

  @Override
  public Sample<V> getLast() {
    return values == null || values.length == 0 ? null : values[values.length - 1];
  }

  @Override
  public Sample<V> getFirst() {
    return values == null || values.length == 0 ? null : values[0];
  }
}
