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
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.UID;

import static java.util.Objects.requireNonNull;

/**
 * @author Mathieu Carbou
 */
public class NodeRemovalNomadChange extends NodeNomadChange {

  // For json
  private NodeRemovalNomadChange() {}

  public NodeRemovalNomadChange(Cluster cluster, UID stripeUID, Node node) {
    super(cluster, stripeUID, node);
  }

  @Override
  public Cluster apply(Cluster original) {
    requireNonNull(original);
    if (!original.containsNode(getNode().getName())) {
      throw new IllegalArgumentException("Node name: " + getNode().getName() + " is not in cluster: " + original);
    }
    if (!original.containsNode(getNode().getUID())) {
      throw new IllegalArgumentException("Node: " + getNode().getUID() + " is not in cluster: " + original);
    }
    Cluster updated = original.clone();
    updated.removeNode(getNode().getUID());
    return updated;
  }

  @Override
  public boolean canUpdateRuntimeTopology(NodeContext currentNode) {
    return true;
  }

  @Override
  public String getSummary() {
    return "Detaching node: " + getNode().getName() + " from stripe: " + getCluster().getStripe(getStripeUID()).get().getName();
  }

  @Override
  public String toString() {
    return "NodeRemovalNomadChange{" + "" +
        "removedNode=" + getNode().getName() +
        ", cluster=" + getCluster().toShapeString() +
        '}';
  }
}
