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

import java.util.Collection;
import java.util.Optional;

/**
 * @author Mathieu Carbou
 */
@Parameters(commandDescription = "Detach a node from an existing stripe or detach a stripe from a cluster")
public class DetachCommand extends TopologyChangeCommand {

  public DetachCommand(NodeAddressDiscovery nodeAddressDiscovery, MultiDiagnosticServiceConnectionFactory connectionFactory) {
    super(nodeAddressDiscovery, connectionFactory);
  }

  @Override
  public String name() {
    return "detach";
  }

  @Override
  protected Cluster updateTopology(Target destination, Collection<Node> sources) {

    Cluster cluster = destination.getCluster().clone();

    switch (getType()) {

      case NODE: {
        // removes the source nodes from the destination cluster (whatever the stripe is)
        sources.forEach(cluster::detach);
        break;
      }

      case STRIPE: {
        // removes all the stripes of destination cluster containing the source nodes
        sources.stream()
            .map(node -> cluster.getStripe(node.getNodeAddress()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .forEach(cluster::removeStripe);
        break;
      }

      default: {
        throw new UnsupportedOperationException(getType().name());
      }
    }

    return cluster;
  }
}
