/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.api.model.nomad;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;

import java.net.InetSocketAddress;

/**
 * @author Mathieu Carbou
 */
public abstract class NodeNomadChange extends TopologyNomadChange {

  private final int stripeId;
  private final Node node;

  public NodeNomadChange(Cluster updated, int stripeId, Node node) {
    super(updated, Applicability.stripe(stripeId));
    this.stripeId = stripeId;
    this.node = node;
  }

  public int getStripeId() {
    return stripeId;
  }

  @JsonIgnore
  public InetSocketAddress getNodeAddress() {
    return getNode().getNodeAddress();
  }

  public Node getNode() {
    return node;
  }
}
