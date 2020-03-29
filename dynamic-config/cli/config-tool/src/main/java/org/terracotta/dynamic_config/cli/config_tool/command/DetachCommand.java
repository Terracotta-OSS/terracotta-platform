/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.dynamic_config.cli.config_tool.command;

import com.beust.jcommander.Parameters;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.nomad.NodeNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.NodeRemovalNomadChange;
import org.terracotta.dynamic_config.cli.command.Usage;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;

import static java.lang.System.lineSeparator;
import static org.terracotta.dynamic_config.cli.config_tool.converter.OperationType.NODE;

/**
 * @author Mathieu Carbou
 */
@Parameters(commandNames = "detach", commandDescription = "Detach a node from a stripe, or a stripe from a cluster")
@Usage("detach [-t node|stripe] -d <hostname[:port]> -s <hostname[:port]> [-f]ø")
public class DetachCommand extends TopologyCommand {

  boolean sourceNodeUnreachable;

  @Override
  public void validate() {
    super.validate();

    if (destinationCluster.getNodeCount() == 1) {
      throw new IllegalStateException("Unable to detach since destination cluster contains only 1 node");
    }

    if (!destinationCluster.containsNode(source)) {
      throw new IllegalStateException("Source node: " + source + " is not part of cluster at: " + destination);
    }

    sourceNodeUnreachable = !destinationOnlineNodes.containsKey(source);

    validateLogOrFail(() -> !sourceNodeUnreachable, "Node to detach: " + source + " is not reachable. " +
        "Use -f to force the node removal in destination cluster. " +
        "The detached node will reset when it will restart.");
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
        return new NodeRemovalNomadChange(result, destinationCluster.getStripeId(source).getAsInt(), destinationCluster.getNode(source).get());
      case STRIPE: {
        throw new UnsupportedOperationException("Topology modifications of whole stripes on an activated cluster is not yet supported");
      }
      default: {
        throw new UnsupportedOperationException(operationType.name());
      }
    }
  }

  @Override
  protected void onNomadChangeSuccess(NodeNomadChange nomadChange) {
    Collection<InetSocketAddress> removedNodes = getNodesToResetAndRestart();

    RuntimeException all = null;
    for (InetSocketAddress removedNode : removedNodes) {
      try {
        logger.info("Node: {} will reset and restart in 5 seconds", removedNode);
        resetAndRestart(removedNode);
      } catch (RuntimeException e) {
        logger.warn("Failed to reset and restart node {}: {}", removedNode, e.getMessage());
        if (all == null) {
          all = e;
        } else {
          all.addSuppressed(e);
        }
      }
    }
    if (all != null) {
      throw all;
    }
  }

  @Override
  protected void onNomadChangeFailure(NodeNomadChange nomadChange, RuntimeException error) {
    logger.error("An error occurred during the detach transaction." + lineSeparator() +
        "The node to detach will not be restarted." + lineSeparator() +
        "You you will need to run the diagnostic command to check the configuration state and restart the node to detach manually.");
    throw error;
  }

  private Collection<InetSocketAddress> getNodesToResetAndRestart() {
    Collection<InetSocketAddress> nodes = new ArrayList<>();
    if (operationType == NODE) {
      // when we want to detach a node
      if (sourceNodeUnreachable) {
        logger.warn("Node: {} is not reachable. It will be removed from the cluster when it will be restarted", source);
      } else {
        nodes.add(source);
      }
    } else {
      // when we want tp detach a stripe
      nodes.addAll(destinationCluster.getStripe(source).get().getNodeAddresses());
    }
    return nodes;
  }
}
