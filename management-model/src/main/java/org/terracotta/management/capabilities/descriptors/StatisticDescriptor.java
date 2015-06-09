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

import org.terracotta.management.stats.StatisticType;

/**
 * @author Ludovic Orban
 */
public class StatisticDescriptor implements Descriptor {

  private final String name;
  private final StatisticType type;

  public StatisticDescriptor(String name, StatisticType type) {
    this.name = name;
    this.type = type;
  }

  public String getName() {
    return name;
  }

  public StatisticType getType() {
    return type;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    StatisticDescriptor that = (StatisticDescriptor) o;

    if (name != null ? !name.equals(that.name) : that.name != null) return false;
    return !(type != null ? !type.equals(that.type) : that.type != null);

  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (type != null ? type.hashCode() : 0);
    return result;
  }
}
