package org.terracotta.management.stats;

import java.util.List;

/**
 * @author Mathieu Carbou
 */
public abstract class AbstractStatisticHistory<V, U> extends AbstractStatistic<List<Sample<V>>, U> implements StatisticHistory<V, U> {
  public AbstractStatisticHistory(String name, List<Sample<V>> value, U unit) {
    super(name, value, unit);
  }
}
