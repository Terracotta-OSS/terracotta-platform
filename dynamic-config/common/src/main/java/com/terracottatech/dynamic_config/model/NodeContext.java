/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * @author Mathieu Carbou
 */
public class NodeContext implements Cloneable {

  private final Cluster cluster;
  private final int stripeId;
  private final int nodeId;
  private final String nodeName;

  @JsonCreator
  public NodeContext(@JsonProperty(value = "cluster", required = true) Cluster cluster,
                     @JsonProperty(value = "stripeId", required = true) int stripeId,
                     @JsonProperty(value = "nodeName", required = true) String nodeName) {
    this.cluster = requireNonNull(cluster);
    this.stripeId = stripeId;
    this.nodeName = requireNonNull(nodeName);
    if (stripeId < 1 || stripeId > cluster.getStripeCount()) {
      throw new IllegalArgumentException("Invalid Stripe Id: " + stripeId);
    }
    // verify we can find the node
    getNode();
    this.nodeId = cluster.getStripes().get(stripeId - 1).getNodeId(nodeName).get();
  }

  public NodeContext(Cluster cluster, Node node) {
    this.cluster = requireNonNull(cluster);
    this.stripeId = cluster.getStripeId(node)
        .orElseThrow(() -> new IllegalArgumentException("Node " + node + " not in cluster " + cluster));
    this.nodeName = node.getNodeName();
    // verify we can find the node
    getNode();
    this.nodeId = cluster.getStripes().get(stripeId - 1).getNodeId(nodeName).get();
  }

  public Cluster getCluster() {
    return cluster;
  }

  public int getStripeId() {
    return stripeId;
  }

  public int getNodeId() {
    return nodeId;
  }

  public String getNodeName() {
    return nodeName;
  }

  @JsonIgnore
  public Node getNode() {
    return cluster.getNode(stripeId, nodeName)
        .orElseThrow(() -> new IllegalStateException("Node " + nodeName + " in stripe " + stripeId + " not found"));
  }

  @JsonIgnore
  public Stripe getStripe() {
    return cluster.getStripes().get(stripeId - 1);
  }

  @Override
  @SuppressWarnings("MethodDoesntCallSuperMethod")
  @SuppressFBWarnings("CN_IDIOM_NO_SUPER_CALL")
  public NodeContext clone() {
    return new NodeContext(cluster.clone(), stripeId, nodeName);
  }

  @Override
  public String toString() {
    return "NodeContext{" +
        "nodeName='" + nodeName + '\'' +
        ", stripeId=" + stripeId +
        ", cluster=" + cluster +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof NodeContext)) return false;
    NodeContext that = (NodeContext) o;
    return getStripeId() == that.getStripeId() &&
        getCluster().equals(that.getCluster()) &&
        getNodeName().equals(that.getNodeName());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getCluster(), getStripeId(), getNodeName());
  }
}
