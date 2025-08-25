/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
package org.terracotta.dynamic_config.cli.api.stop;

import org.terracotta.dynamic_config.api.model.Node;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author Mathieu Carbou
 */
public interface StopProgress {
  /**
   * Await indefinitely for all nodes to be stopped.
   */
  void await() throws InterruptedException;

  /**
   * Await for all nodes to be stopped for a maximum amount of time.
   * <p>
   * If the timeout expires, returns the list of all nodes that have been detected as stopped.
   * <p>
   * If all nodes are stopped before the timeout expires, the list will contain all the nodes that were asked to stop
   */
  Collection<Node.Endpoint> await(Duration duration) throws InterruptedException;

  /**
   * Register a callback that will be called when a node has been stopped
   */
  void onStopped(Consumer<Node.Endpoint> c);

  /**
   * Get all nodes for which we have failed to ask for a stop
   */
  Map<Node.Endpoint, Exception> getErrors();
}
