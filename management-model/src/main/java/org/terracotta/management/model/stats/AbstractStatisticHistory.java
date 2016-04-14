package org.terracotta.management.model.stats;

import org.terracotta.management.model.Objects;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * @author Mathieu Carbou
 */
public abstract class AbstractStatisticHistory<V extends Serializable, U extends Serializable> implements StatisticHistory<V, U>, Serializable {

  private final Sample<V>[] values;
  private final U unit;

  @SuppressWarnings("unchecked")
  public AbstractStatisticHistory(List<Sample<V>> values, U unit) {
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

}
