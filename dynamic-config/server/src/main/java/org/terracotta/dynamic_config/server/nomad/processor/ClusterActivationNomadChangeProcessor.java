/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.server.nomad.processor;

import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.nomad.ClusterActivationNomadChange;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.nomad.server.NomadException;

public class ClusterActivationNomadChangeProcessor extends TopologyNomadChangeProcessor<ClusterActivationNomadChange> {
  public ClusterActivationNomadChangeProcessor(TopologyService topologyService, int stripeId, String nodeName) {
    super(topologyService, stripeId, nodeName);
  }

  @Override
  protected Cluster tryUpdateTopology(Cluster existing, ClusterActivationNomadChange change) throws NomadException {
    throw new NomadException("Existing config must be null. Found: " + existing);
  }
}
