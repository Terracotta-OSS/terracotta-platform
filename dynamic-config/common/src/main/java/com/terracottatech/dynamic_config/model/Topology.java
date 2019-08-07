/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * @author Mathieu Carbou
 */
public class Topology {

  private final Cluster cluster;
  private final int stripeId;
  private final String nodeName;

  @JsonCreator
  public Topology(@JsonProperty("cluster") Cluster cluster,
                  @JsonProperty("stripeId") int stripeId,
                  @JsonProperty("nodeName") String nodeName) {
    this.cluster = requireNonNull(cluster);
    this.stripeId = stripeId;
    this.nodeName = requireNonNull(nodeName);
    // verify we can find the node
    getNode();
  }

  public Topology(Cluster cluster, Node node) {
    this.cluster = requireNonNull(cluster);
    this.stripeId = cluster.getStripeId(node)
        .orElseThrow(() -> new IllegalArgumentException("Node " + node + " not in cluster " + cluster));
    this.nodeName = node.getNodeName();
    // verify we can find the node
    getNode();
  }

  public Cluster getCluster() {
    return cluster;
  }

  public int getStripeId() {
    return stripeId;
  }

  public String getNodeName() {
    return nodeName;
  }

  @JsonIgnore
  public Node getNode() {
    return cluster.getNode(stripeId, nodeName)
        .orElseThrow(() -> new IllegalStateException("Node " + nodeName + " in stripe " + stripeId + " not found"));
  }

  @SuppressWarnings("CloneDoesntDeclareCloneNotSupportedException")
  @Override
  public Topology clone() {
    return new Topology(cluster.clone(), stripeId, nodeName);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("Topology{");
    sb.append("cluster=").append(cluster);
    sb.append(", stripeId=").append(stripeId);
    sb.append(", nodeName='").append(nodeName).append('\'');
    sb.append('}');
    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Topology)) return false;
    Topology that = (Topology) o;
    return getStripeId() == that.getStripeId() &&
        getCluster().equals(that.getCluster()) &&
        getNodeName().equals(that.getNodeName());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getCluster(), getStripeId(), getNodeName());
  }
}
