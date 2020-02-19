/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.api.model.nomad;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;

/**
 * @author Mathieu Carbou
 */
public abstract class NodeNomadChange extends TopologyNomadChange {

  public NodeNomadChange(Cluster updated) {
    super(updated, Applicability.cluster());
  }

  @JsonIgnore
  public abstract Node getNode();
}