/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.UID;

import static java.util.Objects.requireNonNull;

/**
 * @author Mathieu Carbou
 */
public class NodeAdditionNomadChange extends NodeNomadChange {

  // For json
  private NodeAdditionNomadChange() {}

  public NodeAdditionNomadChange(Cluster cluster, UID stripeUID, Node node) {
    super(cluster, stripeUID, node);

    Stripe stripe = cluster.getStripe(stripeUID)
        .orElseThrow(() -> new IllegalArgumentException("Invalid stripe UID: " + stripeUID + " in cluster " + cluster.toShapeString()));
    if (stripe.getNodes().stream().noneMatch(node::equals)) {
      throw new IllegalArgumentException("Node " + node.getName() + " is not part of stripe: " + stripe.getName() + " in cluster " + cluster.toShapeString());
    }
  }

  @Override
  public Cluster apply(Cluster original) {
    requireNonNull(original);
    original.getStripe(getStripeUID())
        .orElseThrow(() -> new IllegalArgumentException("Stripe UID: " + getStripeUID() + " does not exist in cluster: " + original.toShapeString()));
    if (original.containsNode(getNode().getName())) {
      throw new IllegalArgumentException("Node name: " + getNode().getName() + " already exists in cluster: " + original.toShapeString());
    }
    if (original.containsNode(getNode().getUID())) {
      throw new IllegalArgumentException("Node: " + getNode().getUID() + " already exists in cluster: " + original);
    }
    Cluster updated = original.clone();
    updated.getStripe(getStripeUID()).get().addNode(getNode().clone());
    return updated;
  }

  @Override
  public boolean canUpdateRuntimeTopology(NodeContext currentNode) {
    return true;
  }

  @Override
  public String getSummary() {
    return "Attaching node: " + getNode().toShapeString() + " to stripe: " + getCluster().getStripe(getStripeUID()).get().getName();
  }

  @Override
  public String toString() {
    return "NodeAdditionNomadChange{" +
        "stripeUID=" + getStripeUID() +
        ", node=" + getNode().getName() +
        ", cluster=" + getCluster().toShapeString() +
        '}';
  }
}
