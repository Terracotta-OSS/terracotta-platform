package org.terracotta.management.stats;

import java.io.Serializable;

/**
 * @author Ludovic Orban
 */
public interface Statistic<V, U> extends Serializable {

  String getName();

  V getValue();

  U getUnit();

}
