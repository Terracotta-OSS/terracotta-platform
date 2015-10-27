package org.terracotta.management.stats;

/**
 * @author Ludovic Orban
 */
public interface Statistic<V, U> {

  String getName();

  V getValue();

  U getUnit();

}
