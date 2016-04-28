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

import com.tc.classloader.CommonComponent;

import java.io.Closeable;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author Mathieu Carbou
 */
@CommonComponent
public interface IMonitoringConsumer extends Closeable {

  /**
   * Reads the value for the node located at the given path.
   *
   * @param path The node name path leading to a specific node, starting at the root.
   * @param type The type of the value, for runtime type safety.
   * @return The value set for the node or null, if it wasn't found.
   * @throws ClassCastException If the type given does not match the type in the node (beware different class loaders).
   */
  <T> Optional<T> getValueForNode(String[] path, Class<T> type) throws ClassCastException;

  default <T> Optional<T> getValueForNode(String[] parents, String nodeName, Class<T> type) throws ClassCastException {
    return getValueForNode(Utils.concat(parents, nodeName), type);
  }

  /**
   * Gets the names of the children of a node located at the given path.
   *
   * @param path The node name path leading to a specific node, starting at the root.
   * @return The names of all immediate children of this node or null, if it wasn't found.
   */
  Optional<Collection<String>> getChildNamesForNode(String[] path);

  default Optional<Collection<String>> getChildNamesForNode(String[] parent, String nodeName) {
    return getChildNamesForNode(Utils.concat(parent, nodeName));
  }

  /**
   * Get the stream of mutations of the monitoring tree. They are ordered by creation time.
   * The stream returned is always the same and consumed messages cannot be consumed again.
   * This is a little like a 'read' method.
   * <p>
   * The stream ends when there is not more mutations to read.
   * <p>
   * There is also one stream per consumer. So mutations read by this consumer will still be available if other consumers are also reading.
   */
  Stream<Mutation> readMutations();

  /**
   * closes this consumer
   */
  void close();
}
