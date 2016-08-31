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

/**
 * @author Ludovic Orban
 * @author Mathieu Carbou
 */
public abstract class AbstractStatistic<V, U> implements Statistic<V, U>, Serializable {

  private static final long serialVersionUID = 1;

  private final V value;
  private final U unit;

  public AbstractStatistic(V value, U unit) {
    this.value = Objects.requireNonNull(value);
    this.unit = Objects.requireNonNull(unit);
  }

  @Override
  public final U getUnit() {
    return unit;
  }

  @Override
  public final V getValue() {
    return value;
  }

  @Override
  public String toString() {
    return "{" + "type='" + getClass().getSimpleName() + '\'' + ", value=" + getValue() + ", unit=" + getUnit() + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AbstractStatistic<?, ?> that = (AbstractStatistic<?, ?>) o;
    if (!value.equals(that.value)) return false;
    return unit.equals(that.unit);
  }

  @Override
  public int hashCode() {
    int result = value.hashCode();
    result = 31 * result + unit.hashCode();
    return result;
  }
}
