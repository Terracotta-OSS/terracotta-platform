/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.api.model.nomad;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.terracotta.dynamic_config.api.model.Node;

import java.net.InetSocketAddress;
import java.util.Collection;

import static java.util.stream.Collectors.joining;

/**
 * @author Mathieu Carbou
 */
@JsonTypeName("NodeAdditionNomadChange")
public class NodeAdditionNomadChange extends PassiveNomadChange {

  private final int stripeId;

  @JsonCreator
  public NodeAdditionNomadChange(@JsonProperty(value = "stripeId", required = true) int stripeId,
                                 @JsonProperty(value = "nodes", required = true) Collection<Node> nodes) {
    super(nodes);
    this.stripeId = stripeId;
  }

  public int getStripeId() {
    return stripeId;
  }

  @Override
  public String getSummary() {
    return "Attaching nodes: "
        + getNodes().stream().map(Node::getNodeAddress).map(InetSocketAddress::toString).collect(joining(", "))
        + " to stripe ID: "
        + stripeId;
  }

  @Override
  public String toString() {
    return "NodeAdditionNomadChange{" +
        "stripeId=" + stripeId +
        "nodes=" + getNodes() +
        ", applicability=" + getApplicability() +
        '}';
  }
}
