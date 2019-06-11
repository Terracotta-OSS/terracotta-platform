/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.command;


import com.beust.jcommander.Parameters;
import com.terracottatech.diagnostic.client.connection.MultiDiagnosticServiceConnectionFactory;
import com.terracottatech.dynamic_config.cli.connect.NodeAddressDiscovery;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.dynamic_config.model.Stripe;

import java.util.Collection;

/**
 * @author Mathieu Carbou
 */
@Parameters(commandDescription = "Attach a node to an existing stripe or attach a node to a new stripe")
public class AttachCommand extends TopologyChangeCommand {

  public AttachCommand(NodeAddressDiscovery nodeAddressDiscovery, MultiDiagnosticServiceConnectionFactory connectionFactory) {
    super(nodeAddressDiscovery, connectionFactory);
  }

  @Override
  public String name() {
    return "attach";
  }

  @Override
  protected Cluster updateTopology(Target destination, Collection<Node> sources) {

    Cluster cluster = destination.getCluster().clone();

    switch (getType()) {

      case NODE: {
        Stripe stripe = cluster.getStripe(destination.getNode().getNodeAddress())
            // should NEVER happen
            .orElseThrow(() -> new IllegalStateException("Node " + destination.getNode().getNodeAddress() + " not anymore in cluster " + cluster));
        // add the source nodes to the destination stripe
        sources.forEach(stripe::attach);
        break;
      }

      case STRIPE: {
        // add a new stripe in destination cluster containing all the source nodes
        Stripe stripe = new Stripe();
        sources.forEach(stripe::attach);
        cluster.addStripe(stripe);
        break;
      }

      default: {
        throw new UnsupportedOperationException(getType().name());
      }
    }

    return cluster;
  }
}
