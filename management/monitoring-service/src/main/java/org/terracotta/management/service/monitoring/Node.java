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
package org.terracotta.management.service.monitoring;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Mathieu Carbou
 */
class Node {

  private final Serializable value;
  private final ConcurrentMap<String, Node> children = new ConcurrentHashMap<>(0);

  Node() {
    this(null);
  }

  Node(Serializable value) {
    this.value = value;
  }

  Serializable getValue() {
    return value;
  }

  Node addChild(String name, Node child) {
    return this.children.put(name, child);
  }

  void removeChildren() {
    children.clear();
  }

  Node removeChild(String name) {
    return this.children.remove(name);
  }

  Node getChild(String name) {
    return this.children.get(name);
  }

  Set<String> getChildNames() {
    return this.children.keySet();
  }

  Map<String, Serializable> getChildValues() {
    Map<String, Serializable> copy = new HashMap<>(children.size());
    for (Map.Entry<String, Node> entry : this.children.entrySet()) {
      copy.put(entry.getKey(), entry.getValue().getValue());
    }
    return copy;
  }

  @Override
  public String toString() {
    return String.valueOf(getValue()) + " " + getChildNames();
  }

  boolean hasChild() {
    return !children.isEmpty();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Node node = (Node) o;

    if (getValue() != null ? !getValue().equals(node.getValue()) : node.getValue() != null) return false;
    return children.equals(node.children);

  }

  @Override
  public int hashCode() {
    int result = getValue() != null ? getValue().hashCode() : 0;
    result = 31 * result + children.hashCode();
    return result;
  }

  Map<String, Node> getChildren() {
    return children;
  }
}
