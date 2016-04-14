package org.terracotta.management.model.stats;

import java.io.Serializable;

/**
 * @author Mathieu Carbou
 */
public interface StatisticHistory<V extends Serializable, U extends Serializable> extends Statistic<Sample<V>[], U> {

}
