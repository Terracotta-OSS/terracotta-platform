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
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.nomad.NodeAdditionNomadChange;
import org.terracotta.dynamic_config.api.service.ClusterValidator;
import org.terracotta.dynamic_config.api.service.DynamicConfigListener;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.nomad.server.NomadException;

import java.net.InetSocketAddress;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

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
  public NodeContext tryApply(NodeContext baseConfig, NodeAdditionNomadChange change) throws NomadException {
    try {
      checkMBeanOperation();
      Cluster cluster = baseConfig.getCluster();
      Stripe stripe = cluster.getStripe(change.getStripeId()).get();
      change.getNodes().forEach(stripe::attachNode);
      new ClusterValidator(cluster).validate();
      return baseConfig;
    } catch (Exception e) {
      throw new NomadException("Error when trying to apply: '" + change.getSummary() + "': " + e.getMessage(), e);
    }
  }

  @Override
  public void apply(NodeAdditionNomadChange change) throws NomadException {
    try {
      List<Node> newNodes = change.getNodes();
      int stripeId = change.getStripeId();

      LOGGER.info("Adding nodes: {} to stripe ID: {}",
          newNodes.stream().map(Node::getNodeAddress).map(InetSocketAddress::toString).collect(joining(", ")),
          stripeId);

      // try to apply the change on the runtime configuration
      NodeContext nodeContext = topologyService.getRuntimeNodeContext();
      Cluster cluster = nodeContext.getCluster();
      Stripe stripe = cluster.getStripe(stripeId).orElse(null);

      if (stripe != null) { // should not be null in theory, but the apply method should NEVER fail (COMMIT).
        newNodes.forEach(stripe::attachNode);

        //TODO [DYNAMIC-CONFIG]: TDB-4835 - call MBean

        listener.onNodeAddition(nodeContext, stripeId, newNodes);
      }
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
