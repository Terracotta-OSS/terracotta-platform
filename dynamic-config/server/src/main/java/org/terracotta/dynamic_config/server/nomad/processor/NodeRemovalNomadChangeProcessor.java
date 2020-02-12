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
import java.util.ArrayList;
import java.util.Collection;

import static java.util.Objects.requireNonNull;

/**
 * @author Mathieu Carbou
 */
public class NodeRemovalNomadChangeProcessor implements NomadChangeProcessor<NodeRemovalNomadChange> {
  private static final Logger LOGGER = LoggerFactory.getLogger(NodeRemovalNomadChangeProcessor.class);

  private final TopologyService topologyService;
  private final DynamicConfigListener listener;

  public NodeRemovalNomadChangeProcessor(TopologyService topologyService, DynamicConfigListener listener) {
    this.topologyService = requireNonNull(topologyService);
    this.listener = requireNonNull(listener);
  }

  @Override
  public NodeContext tryApply(NodeContext baseConfig, NodeRemovalNomadChange change) throws NomadException {
    try {
      checkMBeanOperation();
      Cluster cluster = baseConfig.getCluster();
      change.getNodes().stream().map(Node::getNodeAddress).forEach(cluster::detachNode);
      new ClusterValidator(cluster).validate();
      return baseConfig;
    } catch (Exception e) {
      throw new NomadException("Error when trying to apply: '" + change.getSummary() + "': " + e.getMessage(), e);
    }
  }

  @Override
  public void apply(NodeRemovalNomadChange change) throws NomadException {
    try {
      Collection<Node> removedNodes = change.getNodes();

      LOGGER.info("Removing nodes: {}", removedNodes);

      // try to apply the change on the runtime configuration
      NodeContext nodeContext = topologyService.getRuntimeNodeContext();
      Cluster cluster = nodeContext.getCluster();

      Collection<Node> removed = new ArrayList<>(removedNodes.size());
      for (Node removedNode : removedNodes) {
        InetSocketAddress removedNodeAddress = removedNode.getNodeAddress();
        cluster.getNode(removedNodeAddress).ifPresent(node -> {
          if (cluster.detachNode(removedNodeAddress)) {
            removed.add(node);
          }
        });
      }

      //TODO [DYNAMIC-CONFIG]: TDB-4835 - call MBean

      listener.onNodeRemoval(nodeContext, removed);
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
