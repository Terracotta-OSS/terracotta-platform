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
package org.terracotta.management.stats;

/**
 * @author Ludovic Orban
 */
public abstract class AbstractMeasurableStatistic<V, U> extends AbstractStatistic<V> implements MeasurableStatistic<V, U> {

  private final U unit;

  public AbstractMeasurableStatistic(String name, V value, U unit) {
    super(name, value);
    this.unit = unit;
  }

  @Override
  public U getUnit() {
    return unit;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o != null && !o.getClass().equals(getClass())) return false;
    if (!super.equals(o)) return false;

    AbstractMeasurableStatistic<?, ?> that = (AbstractMeasurableStatistic<?, ?>) o;

    return !(unit != null ? !unit.equals(that.unit) : that.unit != null);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (unit != null ? unit.hashCode() : 0);
    return result;
  }
}
