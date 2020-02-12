/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.api.model.nomad;

import org.terracotta.dynamic_config.api.model.Node;

import java.util.Collection;

import static java.util.Objects.requireNonNull;

/**
 * @author Mathieu Carbou
 */
public abstract class PassiveNomadChange extends FilteredNomadChange {

  private final Collection<Node> nodes;

  public PassiveNomadChange(Collection<Node> nodes) {
    super(Applicability.cluster());
    this.nodes = requireNonNull(nodes);
  }

  public Collection<Node> getNodes() {
    return nodes;
  }
}
