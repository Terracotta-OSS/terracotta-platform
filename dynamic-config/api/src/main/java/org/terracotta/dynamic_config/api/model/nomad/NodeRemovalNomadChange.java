/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.api.model.nomad;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.net.InetSocketAddress;
import java.util.Collection;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

/**
 * @author Mathieu Carbou
 */
@JsonTypeName("NodeRemovalNomadChange")
public class NodeRemovalNomadChange extends FilteredNomadChange {

  private final Collection<InetSocketAddress> removedNodes;

  @JsonCreator
  public NodeRemovalNomadChange(@JsonProperty(value = "removedNodes", required = true) Collection<InetSocketAddress> removedNodes) {
    super(Applicability.cluster());
    this.removedNodes = requireNonNull(removedNodes);
  }

  public Collection<InetSocketAddress> getRemovedNodes() {
    return removedNodes;
  }

  @Override
  public String getSummary() {
    return "Detaching nodes: " + removedNodes.stream().map(InetSocketAddress::toString).collect(joining(", "));
  }

  @Override
  public String toString() {
    return "NodeRemovalNomadChange{" +
        "removedNodes=" + getRemovedNodes() +
        ", applicability=" + getApplicability() +
        '}';
  }
}
