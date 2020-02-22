/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.server.nomad.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.nomad.ClusterActivationNomadChange;
import org.terracotta.nomad.server.NomadException;

import static java.util.Objects.requireNonNull;

public class ClusterActivationNomadChangeProcessor implements NomadChangeProcessor<ClusterActivationNomadChange> {
  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterActivationNomadChangeProcessor.class);

  private final int stripeId;
  private final String nodeName;

  public ClusterActivationNomadChangeProcessor(int stripeId, String nodeName) {
    this.stripeId = stripeId;
    this.nodeName = requireNonNull(nodeName);
  }

  @Override
  public void validate(NodeContext baseConfig, ClusterActivationNomadChange change) throws NomadException {
    LOGGER.info("Validating change: {}", change.getSummary());
    if (baseConfig != null) {
      throw new NomadException("Existing config must be null. Found: " + baseConfig);
    }
    if (!change.getCluster().containsNode(stripeId, nodeName)) {
      throw new NomadException("Node: " + nodeName + " in stripe ID: " + stripeId + " not found in cluster: " + change.getCluster());
    }
  }

  @Override
  public void apply(ClusterActivationNomadChange change) {
    // no-op
  }
}
