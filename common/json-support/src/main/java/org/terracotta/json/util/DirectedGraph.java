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
package org.terracotta.json.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static java.lang.System.lineSeparator;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Stream.empty;

public class DirectedGraph<T> {
  private final Map<T, Collection<T>> vertexEdges = new LinkedHashMap<>();

  public Collection<T> addVertex(T vertex) {
    return vertexEdges.computeIfAbsent(vertex, v -> new LinkedHashSet<>());
  }

  public void addEdge(T from, T to) {
    addVertex(from).add(to);
  }

  public Stream<T> depthFirstTraversal(final T root) {
    return depthFirstTraversal(root, new HashSet<>(vertexEdges.size()));
  }

  private Stream<T> depthFirstTraversal(final T current, final Set<T> visited) {
    if (visited.add(current)) {
      return Stream.concat(vertexEdges.getOrDefault(current, emptyList()).stream().flatMap(child -> depthFirstTraversal(child, visited)), Stream.of(current));
    } else {
      return empty();
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "\n - " + vertexEdges.entrySet().stream().map(Object::toString).collect(joining(lineSeparator() + " - "));
  }
}
