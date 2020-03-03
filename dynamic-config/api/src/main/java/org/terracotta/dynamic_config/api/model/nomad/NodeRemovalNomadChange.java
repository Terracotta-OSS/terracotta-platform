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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;

import static java.util.Objects.requireNonNull;

/**
 * @author Mathieu Carbou
 */
@JsonTypeName("NodeRemovalNomadChange")
public class NodeRemovalNomadChange extends NodeNomadChange {

  @JsonCreator
  public NodeRemovalNomadChange(@JsonProperty(value = "cluster", required = true) Cluster cluster,
                                @JsonProperty(value = "stripeId", required = true) int stripeId,
                                @JsonProperty(value = "node", required = true) Node node) {
    super(cluster, stripeId, node);
  }

  @Override
  public Cluster apply(Cluster original) {
    requireNonNull(original);
    if (!original.containsNode(getStripeId(), getNode().getNodeName())) {
      throw new IllegalArgumentException("Node name: " + getNode().getNodeName() + " is not in stripe ID: " + getStripeId() + " in cluster: " + original);
    }
    if (!original.containsNode(getNodeAddress())) {
      throw new IllegalArgumentException("Node with address: " + getNodeAddress() + " is not in cluster: " + original);
    }
    Cluster updated = original.clone();
    updated.detachNode(getNodeAddress());
    return updated;
  }

  @Override
  public boolean canApplyAtRuntime() {
    return true;
  }

  @Override
  public String getSummary() {
    return "Detaching node: " + getNodeAddress() + " from stripe ID: " + getStripeId();
  }

  @Override
  public String toString() {
    return "NodeRemovalNomadChange{" + "" +
        "removedNode=" + getNodeAddress() +
        ", node=" + getNodeAddress() +
        ", cluster=" + getCluster().toShapeString() +
        '}';
  }
}
