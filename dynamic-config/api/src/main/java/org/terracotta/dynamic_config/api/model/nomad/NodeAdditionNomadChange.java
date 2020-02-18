/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.api.model.nomad;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Stripe;

import java.net.InetSocketAddress;

/**
 * @author Mathieu Carbou
 */
@JsonTypeName("NodeAdditionNomadChange")
public class NodeAdditionNomadChange extends NodeNomadChange {

  private final int stripeId;
  private final InetSocketAddress address;

  @JsonCreator
  public NodeAdditionNomadChange(@JsonProperty(value = "cluster", required = true) Cluster cluster,
                                 @JsonProperty(value = "stripeId", required = true) int stripeId,
                                 @JsonProperty(value = "address", required = true) InetSocketAddress address) {
    super(cluster);
    this.stripeId = stripeId;
    this.address = address;

    Stripe stripe = cluster.getStripe(stripeId)
        .orElseThrow(() -> new IllegalArgumentException("Invalid stripe ID " + stripeId + " in cluster " + cluster));
    stripe.getNode(address)
        .orElseThrow(() -> new IllegalArgumentException("Node " + address + " is not part of stripe ID " + stripe + " in cluster " + cluster));
  }

  public int getStripeId() {
    return stripeId;
  }

  public InetSocketAddress getAddress() {
    return address;
  }

  @Override
  @JsonIgnore
  public Node getNode() {
    return getCluster().getStripe(stripeId).flatMap(stripe -> stripe.getNode(address)).orElse(null);
  }

  @Override
  public String getSummary() {
    return "Attaching node: " + address + " to stripe ID: " + stripeId;
  }

  @Override
  public String toString() {
    return "NodeAdditionNomadChange{" +
        "stripeId=" + stripeId +
        ", address=" + address +
        ", cluster=" + getCluster() +
        ", applicability=" + getApplicability() +
        '}';
  }
}
