/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.config_tool.command;

import com.beust.jcommander.Parameters;
import com.terracottatech.dynamic_config.api.model.Cluster;
import com.terracottatech.dynamic_config.api.model.Node;
import com.terracottatech.dynamic_config.api.model.NodeContext;
import com.terracottatech.dynamic_config.api.model.Stripe;
import com.terracottatech.dynamic_config.cli.command.Usage;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

/**
 * @author Mathieu Carbou
 */
@Parameters(commandNames = "attach", commandDescription = "Attach a node to a destination stripe or attach a stripe to a destination cluster")
@Usage("attach [-t node|stripe] -d <hostname[:port]> -s <hostname[:port]>,<hostname[:port]>...")
public class AttachCommand extends TopologyCommand {
  @Override
  protected Cluster updateTopology(NodeContext destination, List<Node> sources) {

    Cluster cluster = destination.getCluster().clone();

    switch (getAttachmentType()) {

      case NODE: {
        logger.info(
            "Attaching nodes {} to stripe {}",
            sources.stream().map(Node::getNodeAddress).map(InetSocketAddress::toString).collect(joining(", ")),
            destination.getNode().getNodeAddress()
        );
        Collection<InetSocketAddress> duplicates = cluster.getNodeAddresses();
        duplicates.retainAll(sources.stream().map(Node::getNodeAddress).collect(Collectors.toSet()));
        if (!duplicates.isEmpty()) {
          throw new IllegalArgumentException("Cluster already contains nodes: " + duplicates.stream().map(InetSocketAddress::toString).collect(joining(", ")) + ".");
        }
        Stripe stripe = cluster.getStripe(destination.getNode().getNodeAddress()).get();
        sources.forEach(stripe::attachNode);
        break;
      }

      case STRIPE: {
        logger.info(
            "Attaching a new stripe containing nodes {} to cluster {}",
            sources.stream().map(Node::getNodeAddress).map(InetSocketAddress::toString).collect(joining(", ")),
            destination.getNode().getNodeAddress()
        );
        cluster.attachStripe(new Stripe(sources));
        break;
      }

      default: {
        throw new UnsupportedOperationException(getAttachmentType().name());
      }
    }

    return cluster;
  }
}
