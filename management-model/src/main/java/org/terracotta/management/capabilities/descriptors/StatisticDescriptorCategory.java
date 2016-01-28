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
package org.terracotta.management.capabilities.descriptors;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Ludovic Orban
 */
public final class StatisticDescriptorCategory implements Descriptor, Serializable {

  private final String name;
  private final List<StatisticDescriptor> statistics;

  public StatisticDescriptorCategory(String name, List<StatisticDescriptor> statistics) {
    this.name = name;
    this.statistics = Collections.unmodifiableList(new ArrayList<StatisticDescriptor>(statistics));
  }

  public String getName() {
    return name;
  }

  public List<StatisticDescriptor> getStatistics() {
    return statistics;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    StatisticDescriptorCategory that = (StatisticDescriptorCategory) o;

    if (name != null ? !name.equals(that.name) : that.name != null) return false;
    return statistics.equals(that.statistics);

  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + statistics.hashCode();
    return result;
  }
}
