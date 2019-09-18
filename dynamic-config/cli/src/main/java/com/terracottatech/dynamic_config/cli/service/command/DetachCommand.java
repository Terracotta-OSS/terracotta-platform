/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.service.command;

import com.beust.jcommander.Parameters;
import com.terracottatech.dynamic_config.cli.common.Usage;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Node;

import java.net.InetSocketAddress;
import java.util.List;

import static java.util.stream.Collectors.joining;

/**
 * @author Mathieu Carbou
 */
@Parameters(commandNames = "detach", commandDescription = "Detach a node from an existing stripe or detach a stripe from a cluster")
@Usage("detach -t <node|stripe> -d HOST[:PORT] -s HOST1[:PORT1],HOST2[:PORT2],...")
public class DetachCommand extends TopologyCommand {
  @Override
  protected Cluster updateTopology(Target destination, List<Node> sources) {

    Cluster cluster = destination.getCluster().clone();

    switch (getType()) {

      case NODE: {
        logger.info(
            "Detaching nodes {} from stripe {}",
            sources.stream().map(Node::getNodeAddress).map(InetSocketAddress::toString).collect(joining(", ")),
            destination.getConfiguredNodeAddress()
        );
        sources.stream().map(Node::getNodeAddress).forEach(cluster::detachNode);
        break;
      }

      case STRIPE: {
        logger.info(
            "Detaching stripes containing nodes {} from cluster {}",
            sources.stream().map(Node::getNodeAddress).map(InetSocketAddress::toString).collect(joining(", ")),
            destination.getConfiguredNodeAddress()
        );
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
