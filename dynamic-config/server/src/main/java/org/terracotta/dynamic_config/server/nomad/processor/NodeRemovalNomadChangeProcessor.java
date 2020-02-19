/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.server.nomad.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.nomad.NodeRemovalNomadChange;
import org.terracotta.dynamic_config.api.service.ClusterValidator;
import org.terracotta.dynamic_config.api.service.DynamicConfigListener;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.nomad.server.NomadException;

import java.net.InetSocketAddress;

import static java.util.Objects.requireNonNull;

/**
 * @author Mathieu Carbou
 */
public class NodeRemovalNomadChangeProcessor implements NomadChangeProcessor<NodeRemovalNomadChange> {
  private static final Logger LOGGER = LoggerFactory.getLogger(NodeRemovalNomadChangeProcessor.class);

  private final TopologyService topologyService;
  private final int stripeId;
  private final String nodeName;
  private final DynamicConfigListener listener;

  public NodeRemovalNomadChangeProcessor(TopologyService topologyService, int stripeId, String nodeName, DynamicConfigListener listener) {
    this.topologyService = requireNonNull(topologyService);
    this.stripeId = stripeId;
    this.nodeName = requireNonNull(nodeName);
    this.listener = requireNonNull(listener);
  }

  @Override
  public NodeContext tryApply(NodeContext baseConfig, NodeRemovalNomadChange change) throws NomadException {
    if (baseConfig == null) {
      throw new NomadException("Existing config must not be null");
    }

    try {
      checkMBeanOperation();

      // apply the change
      Cluster existing = baseConfig.clone().getCluster();
      existing.detachNode(change.getNode().getNodeAddress());

      // validate
      new ClusterValidator(existing).validate();
      if (!change.getCluster().equals(existing)) {
        throw new NomadException("Expected: " + change.getCluster() + ", computed: " + existing);
      }

      return new NodeContext(existing, stripeId, nodeName);
    } catch (RuntimeException e) {
      throw new NomadException("Error when trying to apply: '" + change.getSummary() + "': " + e.getMessage(), e);
    }
  }

  @Override
  public final void apply(NodeRemovalNomadChange change) throws NomadException {
    Cluster cluster = topologyService.getRuntimeNodeContext().getCluster();

    if (!cluster.containsNode(change.getNode().getNodeAddress())) {
      return;
    }

    try {
      Node removedNode = change.getNode();
      InetSocketAddress address = removedNode.getNodeAddress();
      int stripeId = cluster.getStripeId(address).getAsInt();

      LOGGER.info("Removing node: {}", address);

      // try to apply the change on the runtime configuration
      cluster.detachNode(address);

      //TODO [DYNAMIC-CONFIG]: TDB-4835 - call MBean

      listener.onNodeRemoval(stripeId, removedNode);
    } catch (RuntimeException e) {
      throw new NomadException("Error when applying: '" + change.getSummary() + "': " + e.getMessage(), e);
    }
  }

  private void checkMBeanOperation() {
    boolean canCall = true;
    //TODO [DYNAMIC-CONFIG]: TDB-4835 - check if MBean can be called
//    boolean canCall = Stream
//        .of(mbeanServer.getMBeanInfo(TC_SERVER_INFO).getOperations())
//        .anyMatch(attr -> "RemoveNode".equals(attr.getName()));
    if (!canCall) {
      throw new IllegalStateException("Unable to invoke MBean operation to detach a node");
    }
  }
}