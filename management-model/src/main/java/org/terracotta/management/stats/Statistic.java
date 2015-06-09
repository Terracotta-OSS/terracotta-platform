package org.terracotta.management.stats;

/**
 * @author Ludovic Orban
 */
public interface Statistic<V> {

  String getName();

  V getValue();

}
