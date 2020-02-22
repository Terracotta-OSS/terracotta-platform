/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.server.nomad.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.nomad.NodeAdditionNomadChange;
import org.terracotta.dynamic_config.api.service.ClusterValidator;
import org.terracotta.dynamic_config.api.service.DynamicConfigListener;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.nomad.server.NomadException;

import static java.util.Objects.requireNonNull;

/**
 * @author Mathieu Carbou
 */
public class NodeAdditionNomadChangeProcessor implements NomadChangeProcessor<NodeAdditionNomadChange> {
  private static final Logger LOGGER = LoggerFactory.getLogger(NodeAdditionNomadChangeProcessor.class);

  private final TopologyService topologyService;
  private final DynamicConfigListener listener;

  public NodeAdditionNomadChangeProcessor(TopologyService topologyService, DynamicConfigListener listener) {
    this.topologyService = requireNonNull(topologyService);
    this.listener = requireNonNull(listener);
  }

  @Override
  public void validate(NodeContext baseConfig, NodeAdditionNomadChange change) throws NomadException {
    if (baseConfig == null) {
      throw new NomadException("Existing config must not be null");
    }
    try {
      checkMBeanOperation();
      Cluster updated = change.apply(baseConfig.getCluster());
      new ClusterValidator(updated).validate();
    } catch (RuntimeException e) {
      throw new NomadException("Error when trying to apply: '" + change.getSummary() + "': " + e.getMessage(), e);
    }
  }

  @Override
  public final void apply(NodeAdditionNomadChange change) throws NomadException {
    Cluster runtime = topologyService.getRuntimeNodeContext().getCluster();
    if (runtime.containsNode(change.getNodeAddress())) {
      return;
    }

    try {
      LOGGER.info("Adding node: {} to stripe ID: {}", change.getNodeAddress(), change.getStripeId());

      //TODO [DYNAMIC-CONFIG]: TDB-4835 - call MBean

      listener.onNodeAddition(change.getStripeId(), change.getNode());
    } catch (RuntimeException e) {
      throw new NomadException("Error when applying: '" + change.getSummary() + "': " + e.getMessage(), e);
    }
  }

  private void checkMBeanOperation() {
    boolean canCall = true;
    //TODO [DYNAMIC-CONFIG]: TDB-4835 - check if MBean can be called
//    boolean canCall = Stream
//        .of(mbeanServer.getMBeanInfo(TC_SERVER_INFO).getOperations())
//        .anyMatch(attr -> "AddNode".equals(attr.getName()));
    if (!canCall) {
      throw new IllegalStateException("Unable to invoke MBean operation to attach a node");
    }
  }
}
