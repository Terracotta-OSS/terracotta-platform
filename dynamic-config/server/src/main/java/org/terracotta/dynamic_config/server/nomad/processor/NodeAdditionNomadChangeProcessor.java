/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.server.nomad.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.nomad.NodeAdditionNomadChange;
import org.terracotta.dynamic_config.api.service.ClusterValidator;
import org.terracotta.dynamic_config.api.service.DynamicConfigListener;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.nomad.server.NomadException;

import static java.util.Objects.requireNonNull;

/**
 * @author Mathieu Carbou
 */
public class NodeAdditionNomadChangeProcessor extends TopologyNomadChangeProcessor<NodeAdditionNomadChange> {
  private static final Logger LOGGER = LoggerFactory.getLogger(NodeAdditionNomadChangeProcessor.class);

  private final DynamicConfigListener listener;

  public NodeAdditionNomadChangeProcessor(TopologyService topologyService, int stripeId, String nodeName, DynamicConfigListener listener) {
    super(topologyService, stripeId, nodeName);
    this.listener = requireNonNull(listener);
  }

  @Override
  protected Cluster tryUpdateTopology(Cluster existing, NodeAdditionNomadChange change) throws NomadException {
    try {
      checkMBeanOperation();

      // apply the change
      Stripe stripe = existing.getStripe(change.getStripeId()).get();
      stripe.attachNode(change.getNode());

      // validate
      new ClusterValidator(existing).validate();
      if (!change.getCluster().equals(existing)) {
        throw new IllegalStateException("Expected: " + change.getCluster() + ", computed: " + existing);
      }

      return existing;
    } catch (Exception e) {
      throw new NomadException("Error when trying to apply: '" + change.getSummary() + "': " + e.getMessage(), e);
    }
  }

  @Override
  public void applyAtRuntime(Cluster cluster, NodeAdditionNomadChange change) throws NomadException {
    if (cluster.containsNode(change.getAddress())) {
      return;
    }

    try {
      Node newNode = change.getNode();
      int stripeId = change.getStripeId();

      LOGGER.info("Adding node: {} to stripe ID: {}", newNode.getNodeAddress(), stripeId);

      // apply the change on the runtime configuration
      cluster.getStripe(stripeId).get().attachNode(newNode);

      //TODO [DYNAMIC-CONFIG]: TDB-4835 - call MBean

      listener.onNodeAddition(stripeId, newNode);
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
