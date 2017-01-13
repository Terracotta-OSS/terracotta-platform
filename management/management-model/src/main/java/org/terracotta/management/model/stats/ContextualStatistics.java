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
import org.terracotta.management.model.context.Contextual;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * This class holds the statistics queried on a specific cluster element (context
 *
 * @author Mathieu Carbou
 */
public final class ContextualStatistics implements Contextual {

  private static final long serialVersionUID = 1;

  private final Map<String, Number> statistics;
  private final String capability;
  private Context context;

  public ContextualStatistics(String capability, Context context, Map<String, Number> statistics) {
    this.statistics = new HashMap<String, Number>(Objects.requireNonNull(statistics));
    this.context = Objects.requireNonNull(context);
    this.capability = Objects.requireNonNull(capability);
  }

  public String getCapability() {
    return capability;
  }

  public int size() {return statistics.size();}

  public boolean isEmpty() {return statistics.isEmpty();}

  public Map<String, Number> getStatistics() {
    return statistics;
  }

  public boolean hasStatistic(String name) {
    return statistics.containsKey(name);
  }

  /**
   * Returns the statistic for a specific name
   *
   * @param name The name of the statistic to return
   * @return The statistic found
   * @throws NoSuchElementException If there is 0 or more than 1 statistic for given type
   */
  public Number getStatistic(String name) throws NoSuchElementException {
    Number stat = statistics.get(name);
    if (stat == null) {
      throw new NoSuchElementException(name);
    }
    return stat;
  }

  @Override
  public void setContext(Context context) {
    this.context = Objects.requireNonNull(context);
  }

  @Override
  public Context getContext() {
    return context;
  }

  @Override
  public String toString() {
    return "ContextualStatistics{" +
        "capability='" + capability + '\'' +
        ", context=" + context +
        ", statistics=" + statistics.size() +
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
