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
import org.terracotta.management.model.stats.StatisticType;

import java.io.Serializable;

/**
 * @author Ludovic Orban
 * @author Mathieu Carbou
 */
public final class StatisticDescriptor implements Descriptor, Serializable {

  private final String name;
  private final StatisticType type;

  public StatisticDescriptor(String name, StatisticType type) {
    this.name = Objects.requireNonNull(name);
    this.type = Objects.requireNonNull(type);
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

    if (!name.equals(that.name)) return false;
    return type == that.type;

  }

  @Override
  public int hashCode() {
    int result = name.hashCode();
    result = 31 * result + type.hashCode();
    return result;
  }
}
