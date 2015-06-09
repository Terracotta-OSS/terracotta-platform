package org.terracotta.management.stats;

/**
 * @author Ludovic Orban
 */
public interface MeasurableStatistic<V, U> extends Statistic<V> {

  U getUnit();

}
