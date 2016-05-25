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
package org.terracotta.management.model.stats;

import org.terracotta.management.model.Objects;
import org.terracotta.management.model.context.Context;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * This class holds the {@link Statistic} list quered from a specific context
 *
 * @author Mathieu Carbou
 */
public final class ContextualStatistics implements Iterable<Statistic<?, ?>>, Serializable {

  private final Map<String, Statistic<?, ?>> statistics;
  private final String capability;
  private final Context context;

  public ContextualStatistics(String capability, Context context, Map<String, Statistic<?, ?>> statistics) {
    this.statistics = new HashMap<String, Statistic<?, ?>>(Objects.requireNonNull(statistics));
    this.context = Objects.requireNonNull(context);
    this.capability = Objects.requireNonNull(capability);
  }

  public String getCapability() {
    return capability;
  }

  public int size() {return statistics.size();}

  public boolean isEmpty() {return statistics.isEmpty();}

  @Override
  public Iterator<Statistic<?, ?>> iterator() {
    return statistics.values().iterator();
  }

  public Map<String, Statistic<?, ?>> getStatistics() {
    return statistics;
  }

  /**
   * Returns the only possible statistic for a specific type
   *
   * @param type The type of the statistic to return
   * @param <T>  The {@link Statistic} type
   * @return The statistic found
   * @throws NoSuchElementException If there is 0 or more than 1 statistic for given type
   */
  public <T extends Statistic<?, ?>> T getStatistic(Class<T> type) throws NoSuchElementException {
    Map<String, T> filtered = getStatistics(type);
    if (filtered.size() != 1) {
      throw new NoSuchElementException(type.getName());
    }
    return filtered.values().iterator().next();
  }

  /**
   * Returns the only possible statistic for a specific type and name
   *
   * @param type The type of the statistic to return
   * @param name The name of the statistic to return
   * @param <T>  The {@link Statistic} type
   * @return The statistic found
   * @throws NoSuchElementException If there is 0 or more than 1 statistic for given type
   */
  public <T extends Statistic<?, ?>> T getStatistic(Class<T> type, String name) throws NoSuchElementException {
    Map<String, T> filtered = getStatistics(type);
    T stat = filtered.get(name);
    if (stat == null) {
      throw new NoSuchElementException(name + ":" + type.getName());
    }
    return stat;
  }

  public <T extends Statistic<?, ?>> Map<String, T> getStatistics(Class<T> type) {
    Map<String, T> filtered = new LinkedHashMap<String, T>();
    for (Map.Entry<String, Statistic<?, ?>> entry : statistics.entrySet()) {
      if (type.isInstance(entry.getValue())) {
        filtered.put(entry.getKey(), type.cast(entry.getValue()));
      }
    }
    return filtered;
  }

  public Context getContext() {
    return context;
  }

  @Override
  public String toString() {
    return "ContextualStatistics{" +
        "capability='" + capability + '\'' +
        ", context=" + context +
        ", statistics=" + statistics +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ContextualStatistics that = (ContextualStatistics) o;

    if (!statistics.equals(that.statistics)) return false;
    if (!capability.equals(that.capability)) return false;
    return context.equals(that.context);

  }

  @Override
  public int hashCode() {
    int result = statistics.hashCode();
    result = 31 * result + capability.hashCode();
    result = 31 * result + context.hashCode();
    return result;
  }

}
