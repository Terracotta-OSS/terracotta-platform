package org.terracotta.management.stats;

import java.util.List;

/**
 * @author Mathieu Carbou
 */
public interface StatisticHistory<V, U> extends Statistic<List<Sample<V>>, U> {
}
