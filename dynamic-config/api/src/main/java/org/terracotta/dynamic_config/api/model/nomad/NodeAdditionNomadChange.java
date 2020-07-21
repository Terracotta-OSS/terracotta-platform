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
import org.terracotta.dynamic_config.api.model.Stripe;

import static java.util.Objects.requireNonNull;

/**
 * @author Mathieu Carbou
 */
public class NodeAdditionNomadChange extends NodeNomadChange {

  public NodeAdditionNomadChange(Cluster cluster,
                                 int stripeId,
                                 Node node) {
    super(cluster, stripeId, node);

    Stripe stripe = cluster.getStripe(stripeId)
        .orElseThrow(() -> new IllegalArgumentException("Invalid stripe ID " + stripeId + " in cluster " + cluster.toShapeString()));
    if (stripe.getNodes().stream().noneMatch(node::equals)) {
      throw new IllegalArgumentException("Node " + node.getName() + " is not part of stripe ID " + stripe + " in cluster " + cluster.toShapeString());
    }
  }

  @Override
  public Cluster apply(Cluster original) {
    requireNonNull(original);
    if (original.containsNode(getStripeId(), getNode().getName())) {
      throw new IllegalArgumentException("Node name: " + getNode().getName() + " already exists in stripe ID: " + getStripeId() + " in cluster: " + original.toShapeString());
    }
    if (original.containsNode(getNodeAddress())) {
      throw new IllegalArgumentException("Node with address: " + getNodeAddress() + " already exists in cluster: " + original);
    }
    Cluster updated = original.clone();
    updated.getStripe(getStripeId()).get().addNode(getNode().clone());
    return updated;
  }

  @Override
  public boolean canApplyAtRuntime() {
    return true;
  }

  @Override
  public String getSummary() {
    return "Attaching node: " + getNodeAddress() + " to stripe ID: " + getStripeId();
  }

  @Override
  public String toString() {
    return "NodeAdditionNomadChange{" +
        "stripeId=" + getStripeId() +
        ", node=" + getNodeAddress() +
        ", cluster=" + getCluster().toShapeString() +
        '}';
  }
}
