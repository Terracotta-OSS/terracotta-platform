/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.cli.config_tool.command;

import com.beust.jcommander.Parameters;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.nomad.NodeNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.NodeRemovalNomadChange;
import org.terracotta.dynamic_config.cli.command.Usage;

import java.net.InetSocketAddress;
import java.util.Collection;

/**
 * @author Mathieu Carbou
 */
@Parameters(commandNames = "detach", commandDescription = "Detach a node from a stripe, or a stripe from a cluster")
@Usage("detach [-t node|stripe] -d <hostname[:port]> -s <hostname[:port]> [-f]")
public class DetachCommand extends TopologyCommand {

  @Override
  public void validate() {
    super.validate();

    Collection<InetSocketAddress> destinationPeers = destinationCluster.getNodeAddresses();

    if (destinationPeers.size() == 1) {
      throw new IllegalStateException("Unable to detach since destination cluster contains only 1 node");
    }

    if (!destinationCluster.containsNode(source)) {
      throw new IllegalStateException("Source node: " + source + " is not part of cluster at: " + destination);
    }
  }

  @Override
  protected Cluster updateTopology() {
    Cluster cluster = destinationCluster.clone();

    switch (operationType) {

      case NODE: {
        logger.info("Detaching node: {} from stripe: {}", source, destination);
        cluster.detachNode(source);
        break;
      }

      case STRIPE: {
        Stripe stripe = cluster.getStripe(source).get();
        logger.info("Detaching stripe containing nodes: {} from cluster: {}", toString(stripe.getNodeAddresses()), destination);
        cluster.detachStripe(stripe);
        break;
      }

      default: {
        throw new UnsupportedOperationException(operationType.name());
      }
    }

    return cluster;
  }

  @Override
  protected NodeNomadChange buildNomadChange(Cluster result) {
    switch (operationType) {
      case NODE:
        return new NodeRemovalNomadChange(result, sourceCluster.getNode(source).get());
      case STRIPE: {
        throw new UnsupportedOperationException("Topology modifications of whole stripes on an activated cluster is not yet supported");
      }
      default: {
        throw new UnsupportedOperationException(operationType.name());
      }
    }
  }
}
