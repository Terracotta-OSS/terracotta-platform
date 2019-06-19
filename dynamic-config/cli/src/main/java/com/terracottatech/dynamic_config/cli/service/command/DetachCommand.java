/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.service.command;

import com.beust.jcommander.Parameters;
import com.terracottatech.dynamic_config.cli.common.Usage;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Node;

import java.util.Collection;

/**
 * @author Mathieu Carbou
 */
@Parameters(commandNames = "detach", commandDescription = "Detach a node from an existing stripe or detach a stripe from a cluster")
@Usage("detach -t <node|stripe> -d HOST[:PORT] -s HOST1[:PORT1],HOST2[:PORT2],...")
public class DetachCommand extends TopologyChangeCommand {
  @Override
  protected Cluster updateTopology(Target destination, Collection<Node> sources) {

    Cluster cluster = destination.getCluster().clone();

    switch (getType()) {

      case NODE: {
        // removes the source nodes from the destination cluster (whatever the stripe is)
        sources.stream().map(Node::getNodeAddress).forEach(cluster::detachNode);
        break;
      }

      case STRIPE: {
        // removes all the stripes of destination cluster containing the source nodes
        sources.stream().map(Node::getNodeAddress).forEach(address -> cluster.getStripe(address).ifPresent(cluster::detachStripe));
        break;
      }

      default: {
        throw new UnsupportedOperationException(getType().name());
      }
    }

    return cluster;
  }
}
