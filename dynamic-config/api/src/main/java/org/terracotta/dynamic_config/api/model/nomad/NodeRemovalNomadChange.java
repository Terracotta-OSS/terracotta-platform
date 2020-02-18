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

/**
 * @author Mathieu Carbou
 */
@JsonTypeName("NodeRemovalNomadChange")
public class NodeRemovalNomadChange extends NodeNomadChange {

  private final Node removedNode;

  @JsonCreator
  public NodeRemovalNomadChange(@JsonProperty(value = "cluster", required = true) Cluster cluster,
                                @JsonProperty(value = "removedNode", required = true) Node removedNode) {
    super(cluster);
    this.removedNode = removedNode;
  }

  @Override
  public Node getNode() {
    return removedNode;
  }

  @Override
  public String getSummary() {
    return "Detaching node: " + removedNode.getNodeAddress();
  }

  @Override
  public String toString() {
    return "NodeRemovalNomadChange{" + "removedNode=" + removedNode +
        ", applicability=" + getApplicability() +
        ", cluster=" + getCluster() +
        '}';
  }
}
