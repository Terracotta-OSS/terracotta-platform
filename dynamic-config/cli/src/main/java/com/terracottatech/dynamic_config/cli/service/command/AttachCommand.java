/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.service.command;

import com.beust.jcommander.Parameters;
import com.terracottatech.dynamic_config.cli.common.Usage;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.dynamic_config.model.Stripe;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

/**
 * @author Mathieu Carbou
 */
@Parameters(commandNames = "attach", commandDescription = "Attach a node to an existing stripe or attach a node to a new stripe")
@Usage("attach -t <node|stripe> -d HOST[:PORT] -s HOST1[:PORT1],HOST2[:PORT2],...")
public class AttachCommand extends TopologyChangeCommand {
  @Override
  protected Cluster updateTopology(Target destination, List<Node> sources) {

    Cluster cluster = destination.getCluster().clone();

    switch (getType()) {

      case NODE: {
        logger.info(
            "Attaching nodes {} to stripe {}",
            sources.stream().map(Node::getNodeAddress).map(InetSocketAddress::toString).collect(joining(", ")),
            destination.getConfiguredNodeAddress()
        );
        Collection<InetSocketAddress> duplicates = cluster.getNodeAddresses();
        duplicates.retainAll(sources.stream().map(Node::getNodeAddress).collect(Collectors.toSet()));
        if (!duplicates.isEmpty()) {
          throw new IllegalArgumentException("Cluster already contains nodes: " + duplicates.stream().map(InetSocketAddress::toString).collect(joining(", ")) + ".");
        }
        Stripe stripe = cluster.getStripe(destination.getConfiguredNodeAddress()).get();
        sources.forEach(stripe::attachNode);
        break;
      }

      case STRIPE: {
        logger.info(
            "Attaching a new stripe containing nodes {} to cluster {}",
            sources.stream().map(Node::getNodeAddress).map(InetSocketAddress::toString).collect(joining(", ")),
            destination.getConfiguredNodeAddress()
        );
        cluster.attachStripe(new Stripe(sources));
        break;
      }

      default: {
        throw new UnsupportedOperationException(getType().name());
      }
    }

    return cluster;
  }
}
