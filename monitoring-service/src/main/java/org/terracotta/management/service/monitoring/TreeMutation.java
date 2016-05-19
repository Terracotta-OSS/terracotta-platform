/*
 * Copyright Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.terracotta.management.service.monitoring;

import org.terracotta.management.sequence.Sequence;

import java.util.Objects;

/**
 * @author Mathieu Carbou
 */
class TreeMutation implements Mutation, Comparable<TreeMutation> {

  private final Object[] parentValues;
  private final Object oldValue;
  private final Object newValue;
  private final byte[] sequence;
  private final long timestamp;
  private final Type type;
  private final String[] parents;
  private final String name;
  private final boolean valueChanged;
  private final String[] path;
  private final long index;

  TreeMutation(long index, Sequence sequence, Type type, String[] parents, String name, Object oldValue, Object newValue, Object[] parentValues) {
    this.index = index;
    this.sequence = sequence.toBytes();
    this.timestamp = sequence.getTimestamp();
    this.type = type;
    this.parents = parents;
    this.name = name;
    this.oldValue = oldValue;
    this.newValue = newValue;
    this.parentValues = parentValues;
    this.valueChanged = !Objects.equals(oldValue, newValue);
    this.path = Utils.concat(parents, name);
  }

  @Override
  public boolean isValueChanged() {
    return valueChanged;
  }

  @Override
  public Object getNewValue() {
    return newValue;
  }

  @Override
  public boolean pathMatches(String... pathPatterns) {
    if (pathPatterns.length != parents.length + 1) {
      return false;
    }
    for (int i = 0; i < parents.length; i++) {
      if (!isWildcard(pathPatterns[i]) && !pathPatterns[i].equals(parents[i])) {
        return false;
      }
    }
    return isWildcard(pathPatterns[parents.length]) || pathPatterns[parents.length].equals(getName());
  }

  @Override
  public Object getOldValue() {
    return oldValue;
  }

  @Override
  public String[] getParents() {
    return parents;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String[] getPath() {
    return path;
  }

  @Override
  public Object[] getParentValues() {
    return parentValues;
  }

  @Override
  public Object getParentValue(int i) {
    return parentValues[i];
  }

  @Override
  public byte[] getSequence() {
    return sequence;
  }

  public long getTimestamp() {
    return timestamp;
  }

  @Override
  public long getIndex() {
    return index;
  }

  @Override
  public Type getType() {
    return type;
  }

  @Override
  public boolean isAnyType(Type... types) {
    for (Type t : types) {
      if (type == t) {
        return true;
      }
    }
    return false;
  }

  @Override
  public int compareTo(TreeMutation o) {
    return (int) (index - o.index);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("TreeMutation{");
    sb.append("index=").append(index);
    sb.append(", timestamp=").append(timestamp);
    sb.append(", type=").append(type);
    sb.append(", path=").append(String.join("/", path));
    sb.append('}');
    return sb.toString();
  }

  private static boolean isWildcard(String s) {
    return s == null || s.equals("*");
  }
}
