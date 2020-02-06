/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.cli.config_tool.command;

import com.beust.jcommander.Parameters;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.nomad.NodeRemovalNomadChange;
import org.terracotta.dynamic_config.cli.command.Usage;
import org.terracotta.nomad.client.change.NomadChange;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * @author Mathieu Carbou
 */
@Parameters(commandNames = "detach", commandDescription = "Detach a node from a destination stripe or detach a stripe from a destination cluster")
@Usage("detach [-t node|stripe] -d <hostname[:port]> -s <hostname[:port]>,<hostname[:port]>... [-f]")
public class DetachCommand extends TopologyCommand {

  @Override
  public void validate() {
    super.validate();

    Collection<InetSocketAddress> destinationPeers = destinationCluster.getNodeAddresses();

    if (destinationPeers.size() == 1) {
      throw new IllegalStateException("Unable to detach: destination cluster only contains 1 node");
    }

    Collection<InetSocketAddress> missing = sourceClusters.keySet()
        .stream()
        .filter(addr -> !destinationPeers.contains(addr))
        .collect(Collectors.toList());

    if (!missing.isEmpty()) {
      throw new IllegalStateException("Source nodes: " + toString(missing) + " are not part of cluster at: " + destination);
    }
  }

  @Override
  protected Cluster updateTopology() {
    Cluster cluster = destinationCluster.clone();

    switch (operationType) {

      case NODE: {
        logger.info("Detaching nodes {} from stripe {}", toString(sources), destination);
        sources.forEach(cluster::detachNode);
        break;
      }

      case STRIPE: {
        logger.info("Detaching stripes containing nodes {} from cluster {}", toString(sources), destination);
        sources.forEach(address -> cluster.getStripe(address).ifPresent(cluster::detachStripe));
        break;
      }

      default: {
        throw new UnsupportedOperationException(operationType.name());
      }
    }

    return cluster;
  }

  @Override
  protected NomadChange buildNomadChange(Cluster result) {
    switch (operationType) {
      case NODE:
        return new NodeRemovalNomadChange(sources);
      case STRIPE: {
        throw new UnsupportedOperationException("Topology modifications of whole stripes on an activated cluster is not yet supported");
      }
      default: {
        throw new UnsupportedOperationException(operationType.name());
      }
    }
  }
}
