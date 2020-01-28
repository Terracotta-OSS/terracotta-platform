/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.net.InetSocketAddress;
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
  private final Node node;

  @JsonCreator
  public NodeContext(@JsonProperty(value = "cluster", required = true) Cluster cluster,
                     @JsonProperty(value = "stripeId", required = true) int stripeId,
                     @JsonProperty(value = "nodeName", required = true) String nodeName) {
    this.cluster = requireNonNull(cluster);
    this.stripeId = stripeId;
    this.nodeName = requireNonNull(nodeName);
    if (stripeId < 1 || stripeId > cluster.getStripeCount()) {
      throw new IllegalArgumentException("Invalid stripe ID: " + stripeId);
    }
    this.node = cluster.getNode(stripeId, nodeName)
        .orElseThrow(() -> new IllegalArgumentException("Node " + nodeName + " in stripe ID " + stripeId + " not found"));
    this.nodeId = cluster.getNodeId(stripeId, nodeName)
        .orElseThrow(() -> new IllegalArgumentException("Node " + nodeName + " in stripe ID " + stripeId + " not found"));
  }

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  public NodeContext(Cluster cluster, InetSocketAddress nodeAddress) {
    requireNonNull(nodeAddress);
    this.cluster = requireNonNull(cluster);
    this.node = cluster.getNode(nodeAddress)
        .orElseThrow(() -> new IllegalArgumentException("Node " + nodeAddress + " not found"));
    this.nodeName = requireNonNull(node.getNodeName());
    this.stripeId = cluster.getStripeId(nodeAddress).getAsInt();
    this.nodeId = cluster.getNodeId(nodeAddress).getAsInt();
  }

  public NodeContext(Cluster cluster, int stripeId, int nodeId) {
    this.cluster = requireNonNull(cluster);
    this.stripeId = stripeId;
    this.nodeId = nodeId;
    this.node = cluster.getNode(stripeId, nodeId)
        .orElseThrow(() -> new IllegalArgumentException("Node ID " + nodeId + " in stripe ID " + stripeId + " not found"));
    this.nodeName = requireNonNull(node.getNodeName());
  }

  /**
   * Special flavor that is creating a node context of a single node cluster
   */
  public NodeContext(Node node) {
    this.node = requireNonNull(node);
    this.cluster = new Cluster(new Stripe(node));
    this.stripeId = 1;
    this.nodeId = 1;
    this.nodeName = requireNonNull(node.getNodeName());
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
    return node;
  }

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  @JsonIgnore
  public Stripe getStripe() {
    return cluster.getStripe(stripeId).get();
  }

  @Override
  @SuppressWarnings("MethodDoesntCallSuperMethod")
  @SuppressFBWarnings("CN_IDIOM_NO_SUPER_CALL")
  public NodeContext clone() {
    return new NodeContext(cluster.clone(), stripeId, nodeName);
  }

  @Override
  public String toString() {
    return "NodeContext{" + "cluster=" + cluster +
        ", stripeId=" + stripeId +
        ", nodeId=" + nodeId +
        ", nodeName='" + nodeName + '\'' +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof NodeContext)) return false;
    NodeContext that = (NodeContext) o;
    return getStripeId() == that.getStripeId() &&
        getNodeId() == that.getNodeId() &&
        getCluster().equals(that.getCluster()) &&
        getNodeName().equals(that.getNodeName());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getCluster(), getStripeId(), getNodeId(), getNodeName());
  }
}
