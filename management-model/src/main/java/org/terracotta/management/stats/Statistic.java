package org.terracotta.management.stats;

import java.io.Serializable;

/**
 * @author Ludovic Orban
 */
public interface Statistic<V extends Serializable, U extends Serializable> extends Serializable {

  V getValue();

  U getUnit();

}
