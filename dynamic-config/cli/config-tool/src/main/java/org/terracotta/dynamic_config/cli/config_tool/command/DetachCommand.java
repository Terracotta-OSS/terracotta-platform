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

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.terracotta.common.struct.Measure;
import org.terracotta.common.struct.TimeUnit;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.FailoverPriority;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.nomad.NodeRemovalNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.StripeRemovalNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.TopologyNomadChange;
import org.terracotta.dynamic_config.cli.command.Usage;
import org.terracotta.dynamic_config.cli.converter.TimeUnitConverter;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;

import static java.lang.System.lineSeparator;
import static org.terracotta.dynamic_config.api.model.FailoverPriority.consistency;
import static org.terracotta.dynamic_config.cli.config_tool.converter.OperationType.NODE;
import static org.terracotta.dynamic_config.cli.config_tool.converter.OperationType.STRIPE;

/**
 * @author Mathieu Carbou
 */
@Parameters(commandNames = "detach", commandDescription = "Detach a node from a stripe, or a stripe from a cluster")
@Usage("detach [-t node|stripe] -d <hostname[:port]> -s <hostname[:port]> [-f] [-W <stop-wait-time>] [-D <stop-delay>]")
public class DetachCommand extends TopologyCommand {

  @Parameter(names = {"-W"}, description = "Maximum time to wait for the nodes to stop. Default: 60s", converter = TimeUnitConverter.class)
  protected Measure<TimeUnit> stopWaitTime = Measure.of(60, TimeUnit.SECONDS);

  @Parameter(names = {"-D"}, description = "Delay before the server stops itself. Default: 2s", converter = TimeUnitConverter.class)
  protected Measure<TimeUnit> stopDelay = Measure.of(2, TimeUnit.SECONDS);

  private final Collection<InetSocketAddress> onlineNodesToRemove = new ArrayList<>(1);

  @Override
  public void validate() {
    super.validate();

    if (destinationCluster.getNodeCount() == 1) {
      throw new IllegalStateException("Unable to detach since destination cluster contains only 1 node");
    }

    if (operationType == NODE) {
      if (!destinationCluster.containsNode(source)) {
        throw new IllegalStateException("Source node: " + source + " is not part of cluster at: " + destination);
      }

      if (destinationCluster.getStripeId(source).getAsInt() != destinationCluster.getStripeId(destination).getAsInt()) {
        throw new IllegalStateException("Source node: " + source + " is not present in the same stripe as destination: " + destination);
      }

      Stripe destinationStripe = destinationCluster.getStripe(destination).get();
      if (destinationStripe.getNodeCount() == 1) {
        throw new IllegalStateException("Unable to detach since destination stripe contains only 1 node");
      }

      FailoverPriority failoverPriority = destinationCluster.getFailoverPriority();
      if (failoverPriority.equals(consistency()) && destinationClusterActivated) {
        int voterCount = failoverPriority.getVoters();
        int nodeCount = destinationStripe.getNodes().size();
        int sum = voterCount + nodeCount;
        if (sum % 2 != 0) {
          logger.warn(lineSeparator() +
              "==============================================================================" + lineSeparator() +
              "WARNING: The sum (" + sum + ") of voter count (" + voterCount + ") and number of nodes " +
              "(" + nodeCount + ") in this stripe " + lineSeparator() +
              "is an odd number, but will become even with the removal of node " + source + lineSeparator() +
              "==============================================================================" + lineSeparator());
        }
      }

      // when we want to detach a node
      onlineNodesToRemove.add(source);
    } else {
      if (!destinationCluster.containsNode(source)) {
        throw new IllegalStateException("Source stripe: " + source + " is not part of cluster at: " + destination);
      }

      if (destinationCluster.getStripeId(source).getAsInt() == destinationCluster.getStripeId(destination).getAsInt()) {
        throw new IllegalStateException("Source node: " + source + " and destination node: " + destination + " are part of the same stripe");
      }

      if (destinationClusterActivated) {
        if (destinationCluster.getStripeId(source).getAsInt() == 1) {
          throw new IllegalStateException("Removing the leading stripe is not allowed");
        }
      }

      // when we want to detach a stripe, we detach all the nodes of the stripe
      onlineNodesToRemove.addAll(destinationCluster.getStripe(source).get().getNodeAddresses());
    }

    // compute the list of online nodes to detach if requested
    onlineNodesToRemove.retainAll(destinationOnlineNodes.keySet());

    // When the operation type is node, the nodes being detached should be stopped first manually
    // But if the operation type is stripe, the stripes being detached are stopped automatically after they're removed
    if (operationType == NODE) {
      if (!onlineNodesToRemove.isEmpty() && areAllNodesActivated(onlineNodesToRemove)) {
        validateLogOrFail(onlineNodesToRemove::isEmpty, "Nodes to be detached: " + toString(onlineNodesToRemove) + " are online. " +
            "Safely shutdown the nodes first, or use the force option to reset and stop the target nodes before detaching them");
      }
    }
  }

  @Override
  protected Cluster updateTopology() {
    Cluster cluster = destinationCluster.clone();

    switch (operationType) {

      case NODE: {
        logger.info("Detaching node: {} from stripe: {}", source, destination);
        cluster.removeNode(source);
        break;
      }

      case STRIPE: {
        Stripe stripe = cluster.getStripe(source).get();
        logger.info("Detaching stripe containing nodes: {} from cluster: {}", toString(stripe.getNodeAddresses()), destination);
        cluster.removeStripe(stripe);
        break;
      }

      default: {
        throw new UnsupportedOperationException(operationType.name());
      }
    }

    return cluster;
  }

  @Override
  protected TopologyNomadChange buildNomadChange(Cluster result) {
    switch (operationType) {
      case NODE:
        return new NodeRemovalNomadChange(result, destinationCluster.getStripeId(source).getAsInt(), destinationCluster.getNode(source).get());
      case STRIPE: {
        return new StripeRemovalNomadChange(result, destinationCluster.getStripe(source).get());
      }
      default: {
        throw new UnsupportedOperationException(operationType.name());
      }
    }
  }

  @Override
  protected void onNomadChangeReady(TopologyNomadChange nomadChange) {
    // When the operation type is node, the nodes being detached should be stopped first manually
    // But if the operation type is stripe, the stripes being detached are stopped automatically after they're removed
    if (operationType == NODE) {
      resetAndStopNodesToRemove();
    }
  }

  @Override
  protected void onNomadChangeSuccess(TopologyNomadChange nomadChange) {
    // When the operation type is node, the nodes being detached should be stopped first manually
    // But if the operation type is stripe, the stripes being detached are stopped automatically after they're removed
    if (operationType == STRIPE) {
      resetAndStopNodesToRemove();
    }
  }

  @Override
  protected Collection<InetSocketAddress> getAllOnlineSourceNodes() {
    return onlineNodesToRemove;
  }

  private void resetAndStopNodesToRemove() {
    if (!onlineNodesToRemove.isEmpty()) {

      logger.info("Reset nodes: {}", toString(onlineNodesToRemove));

      for (InetSocketAddress address : onlineNodesToRemove) {
        try {
          reset(address);
        } catch (RuntimeException e) {
          logger.warn("Error during reset of node: {}: {}", address, e.getMessage(), e);
        }
      }

      logger.info("Stopping nodes: {}", toString(onlineNodesToRemove));

      stopNodes(
          onlineNodesToRemove,
          Duration.ofMillis(stopWaitTime.getQuantity(TimeUnit.MILLISECONDS)),
          Duration.ofMillis(stopDelay.getQuantity(TimeUnit.MILLISECONDS)));

      // if we have stopped some nodes, we need to update the list of nodes online
      destinationOnlineNodes.keySet().removeAll(onlineNodesToRemove);

      // if a failover happened, make sure we get the new server states
      destinationOnlineNodes.entrySet().forEach(e -> e.setValue(getState(e.getKey())));
    }
  }
}
