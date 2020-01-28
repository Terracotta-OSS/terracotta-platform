/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.config_tool.command;

import com.beust.jcommander.Parameters;
import com.terracottatech.dynamic_config.api.model.Cluster;
import com.terracottatech.dynamic_config.api.model.Node;
import com.terracottatech.dynamic_config.api.model.NodeContext;
import com.terracottatech.dynamic_config.cli.command.Usage;

import java.net.InetSocketAddress;
import java.util.List;

import static java.util.stream.Collectors.joining;

/**
 * @author Mathieu Carbou
 */
@Parameters(commandNames = "detach", commandDescription = "Detach a node from a destination stripe or detach a stripe from a destination cluster")
@Usage("detach [-t node|stripe] -d <hostname[:port]> -s <hostname[:port]>,<hostname[:port]>...")
public class DetachCommand extends TopologyCommand {
  @Override
  protected Cluster updateTopology(NodeContext destination, List<Node> sources) {

    Cluster cluster = destination.getCluster().clone();

    switch (getAttachmentType()) {

      case NODE: {
        logger.info(
            "Detaching nodes {} from stripe {}",
            sources.stream().map(Node::getNodeAddress).map(InetSocketAddress::toString).collect(joining(", ")),
            destination.getNode().getNodeAddress()
        );
        sources.stream().map(Node::getNodeAddress).forEach(cluster::detachNode);
        break;
      }

      case STRIPE: {
        logger.info(
            "Detaching stripes containing nodes {} from cluster {}",
            sources.stream().map(Node::getNodeAddress).map(InetSocketAddress::toString).collect(joining(", ")),
            destination.getNode().getNodeAddress()
        );
        sources.stream().map(Node::getNodeAddress).forEach(address -> cluster.getStripe(address).ifPresent(cluster::detachStripe));
        break;
      }

      default: {
        throw new UnsupportedOperationException(getAttachmentType().name());
      }
    }

    return cluster;
  }
}
