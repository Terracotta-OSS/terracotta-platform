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
package org.terracotta.management.model.capabilities.descriptors;

import org.terracotta.management.model.Objects;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * @author Ludovic Orban
 * @author Mathieu Carbou
 */
public final class StatisticDescriptorCategory implements Descriptor, Serializable {

  private static final long serialVersionUID = 1;

  private final String name;
  private final List<StatisticDescriptor> statistics;

  public StatisticDescriptorCategory(String name, StatisticDescriptor... statistics) {
    this(name, Arrays.asList(statistics));
  }

  public StatisticDescriptorCategory(String name, List<StatisticDescriptor> statistics) {
    this.name = Objects.requireNonNull(name);
    this.statistics = Objects.requireNonNull(statistics);
  }

  public String getName() {
    return name;
  }

  public List<StatisticDescriptor> getStatistics() {
    return statistics;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("StatisticDescriptorCategory{");
    sb.append("name='").append(name).append('\'');
    sb.append(", statistics=").append(statistics);
    sb.append('}');
    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    StatisticDescriptorCategory that = (StatisticDescriptorCategory) o;

    if (!name.equals(that.name)) return false;
    return statistics.equals(that.statistics);

  }

  @Override
  public int hashCode() {
    int result = name.hashCode();
    result = 31 * result + statistics.hashCode();
    return result;
  }
}
