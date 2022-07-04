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
package org.terracotta.dynamic_config.api.model.nomad;

import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.nomad.client.change.NomadChange;

/**
 * @author Mathieu Carbou
 */
public interface DynamicConfigNomadChange extends NomadChange {

  /**
   * Returns the updated cluster to use for the next configuration
   *
   * @param original Original cluster on the node. Might be null;
   * @return updated cluster, must not be null
   */
  Cluster apply(Cluster original);

  /**
   * Check if this change can be applied at runtime on this node
   */
  boolean canUpdateRuntimeTopology(NodeContext currentNode);

  default DynamicConfigNomadChange unwrap() {
    return this;
  }
}
