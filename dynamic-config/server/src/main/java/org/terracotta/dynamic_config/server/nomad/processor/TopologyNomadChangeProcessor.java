/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.server.nomad.processor;

import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.nomad.TopologyNomadChange;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.nomad.server.NomadException;

import static java.util.Objects.requireNonNull;

public abstract class TopologyNomadChangeProcessor<T extends TopologyNomadChange> implements NomadChangeProcessor<T> {

  protected final int stripeId;
  protected final String nodeName;
  protected final TopologyService topologyService;

  public TopologyNomadChangeProcessor(TopologyService topologyService, int stripeId, String nodeName) {
    this.topologyService = requireNonNull(topologyService);
    this.stripeId = stripeId;
    this.nodeName = requireNonNull(nodeName);
  }

  @Override
  public final NodeContext tryApply(NodeContext baseConfig, T change) throws NomadException {
    Cluster cluster;
    if (baseConfig == null) {
      cluster = tryCreateTopology(change);
    } else {
      cluster = tryUpdateTopology(baseConfig.clone().getCluster(), change);
    }
    return new NodeContext(cluster, stripeId, nodeName);
  }

  @Override
  public final void apply(T change) throws NomadException {
    Cluster cluster = topologyService.getRuntimeNodeContext().getCluster();
    applyAtRuntime(cluster, change);
  }

  protected void applyAtRuntime(Cluster cluster, T change) throws NomadException {
  }

  protected Cluster tryCreateTopology(T change) throws NomadException {
    // special case when the node restarts after being added to a stripe: it will sync its append log by
    // completely resetting it and starting the sync since the last topology change
    return change.getCluster();
  }

  protected abstract Cluster tryUpdateTopology(Cluster existing, T change) throws NomadException;
}
