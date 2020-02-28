/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
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
