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
package org.terracotta.dynamic_config.cli.api.restart;

import org.terracotta.diagnostic.model.LogicalServerState;
import org.terracotta.dynamic_config.api.model.Node;

import java.time.Duration;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * @author Mathieu Carbou
 */
public interface RestartProgress {
  /**
   * Await indefinitely for all nodes to be restarted.
   */
  void await() throws InterruptedException;

  /**
   * Await for all nodes to be restarted for a maximum amount of time.
   * <p>
   * If the timeout expires, returns the list of all nodes that have been detected as restarted.
   * <p>
   * If all nodes are restarted before the timeout expires, the list will contain all the nodes that were asked to restart
   */
  Map<Node.Endpoint, LogicalServerState> await(Duration duration) throws InterruptedException;

  /**
   * Register a callback that will be called when a node has been restarted
   */
  void onRestarted(BiConsumer<Node.Endpoint, LogicalServerState> c);

  /**
   * Get all nodes for which we have failed to ask for a restart
   */
  Map<Node.Endpoint, Exception> getErrors();
}
