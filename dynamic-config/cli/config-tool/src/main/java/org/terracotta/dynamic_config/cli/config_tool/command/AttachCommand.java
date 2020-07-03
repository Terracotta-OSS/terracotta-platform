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
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.nomad.NodeAdditionNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.NodeNomadChange;
import org.terracotta.dynamic_config.cli.command.Usage;
import org.terracotta.dynamic_config.cli.converter.TimeUnitConverter;
import org.terracotta.inet.InetSocketAddressUtils;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.lang.System.lineSeparator;
import static org.terracotta.dynamic_config.api.model.FailoverPriority.consistency;
import static org.terracotta.dynamic_config.cli.config_tool.converter.OperationType.NODE;
import static org.terracotta.dynamic_config.cli.config_tool.converter.OperationType.STRIPE;

/**
 * @author Mathieu Carbou
 */
@Parameters(commandNames = "attach", commandDescription = "Attach a node to a stripe, or a stripe to a cluster")
@Usage("attach [-t node|stripe] -d <hostname[:port]> -s <hostname[:port]> [-f] [-W <restart-wait-time>] [-D <restart-delay>]")
public class AttachCommand extends TopologyCommand {

  @Parameter(names = {"-W"}, description = "Maximum time to wait for the nodes to restart. Default: 60s", converter = TimeUnitConverter.class)
  protected Measure<TimeUnit> restartWaitTime = Measure.of(60, TimeUnit.SECONDS);

  @Parameter(names = {"-D"}, description = "Delay before the server restarts itself. Default: 2s", converter = TimeUnitConverter.class)
  protected Measure<TimeUnit> restartDelay = Measure.of(2, TimeUnit.SECONDS);

  // list of new nodes to add with their backup topology
  private final Map<InetSocketAddress, Cluster> newOnlineNodes = new LinkedHashMap<>();

  private Cluster sourceCluster;

  @Override
  public void validate() {
    super.validate();

    validateAddress(source);

    sourceCluster = getUpcomingCluster(source);

    Collection<InetSocketAddress> destinationPeers = destinationCluster.getNodeAddresses();
    if (InetSocketAddressUtils.contains(destinationPeers, source)) {
      throw new IllegalArgumentException("Source node: " + source + " is already part of cluster at: " + destination);
    }

    if (isActivated(source)) {
      throw new IllegalArgumentException("Source node: " + source + " cannot be attached since it is part of an existing cluster with name: " + getRuntimeCluster(source).getName());
    }

    /*
     We can only attach some nodes if these conditions are met:
     - either the source node is alone in its own cluster
     - either the source node is part of a 1-stripe cluster and we attach the whole stripe

     We should't allow the user to attach to another "cluster B" a node that is already part of a "cluster A" (containing several nodes),
     except if all the nodes of this "cluster A" are attached to "cluster B".

     The goal is to not have some leftover nodes potentially having wrong information about their topology.
     If that happens, the user must first detach the nodes, then re-attach them somewhere else. The detach process will
     update the topology of "cluster A" so that the detached nodes won't be there anymore and can be attached somewhere else.
     "cluster A" could then be activated without impacting the "cluster B" used in the attach command.
     */
    if (operationType == NODE) {
      validateLogOrFail(
          () -> sourceCluster.getNodeCount() == 1,
          "Source node: " + source + " is part of a stripe containing more than 1 nodes. " +
              "It must be detached first before being attached to a new stripe. " +
              "You can run the command with -f option to force the attachment at the risk of breaking the cluster from where the node is taken.");
    }

    if (operationType == STRIPE) {
      validateLogOrFail(
          () -> sourceCluster.getStripeCount() == 1,
          "Source stripe from node: " + source + " is part of a cluster containing more than 1 stripes. " +
              "It must be detached first before being attached to a new cluster. " +
              "You can run the command with -f option to force the attachment at the risk of breaking the cluster from where the node is taken.");
    }

    // make sure nodes to attach are online
    // building the list of nodes
    if (operationType == NODE) {
      // we attach only a node
      newOnlineNodes.put(source, sourceCluster);
      FailoverPriority failoverPriority = destinationCluster.getFailoverPriority();
      if (failoverPriority.equals(consistency()) && destinationClusterActivated) {
        int voterCount = failoverPriority.getVoters();
        int nodeCount = destinationCluster.getNodes().size();
        int sum = voterCount + nodeCount;
        if (sum > 1 && sum % 2 != 0) {
          logger.warn("WARNING: The sum of voter count ({}) and number of nodes ({}) in this stripe is an odd number," +
              " but will become even with the addition of node {}", voterCount, nodeCount, source);
        }
      }
    } else {
      // we attach a whole stripe
      sourceCluster.getStripe(source).get().getNodeAddresses().forEach(addr -> newOnlineNodes.put(addr, getUpcomingCluster(addr)));
    }
  }

  @Override
  protected Cluster updateTopology() {
    Cluster cluster = destinationCluster.clone();

    switch (operationType) {

      case NODE: {
        logger.info("Attaching node: {} to stripe: {}", source, destination);
        Stripe stripe = cluster.getStripe(destination).get();
        Node node = sourceCluster.getNode(this.source).get();
        stripe.attachNode(node);
        break;
      }

      case STRIPE: {
        Stripe stripe = sourceCluster.getStripe(source).get();
        logger.info("Attaching a new stripe formed with nodes: {} to cluster: {}", toString(stripe.getNodeAddresses()), destination);
        cluster.attachStripe(stripe);
        break;
      }

      default: {
        throw new UnsupportedOperationException(operationType.name());
      }
    }

    return cluster;
  }

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  @Override
  protected NodeNomadChange buildNomadChange(Cluster result) {
    switch (operationType) {
      case NODE:
        return new NodeAdditionNomadChange(result, result.getStripeId(destination).getAsInt(), result.getNode(source).get());
      case STRIPE: {
        throw new UnsupportedOperationException("Topology modifications of whole stripes on an activated cluster is not yet supported");
      }
      default: {
        throw new UnsupportedOperationException(operationType.name());
      }
    }
  }

  @Override
  protected void onNomadChangeReady(NodeNomadChange nomadChange) {
    setUpcomingCluster(newOnlineNodes.keySet(), nomadChange.getCluster());
  }

  @Override
  protected void onNomadChangeSuccess(NodeNomadChange nomadChange) {
    Cluster result = nomadChange.getCluster();
    activate(newOnlineNodes.keySet(), result, null, restartDelay, restartWaitTime);
  }

  @Override
  protected void onNomadChangeFailure(NodeNomadChange nomadChange, RuntimeException error) {
    logger.error("An error occurred during the attach transaction." + lineSeparator() +
        "The node information may still be added to the destination cluster: you will need to run the diagnostic / export command to check the state of the transaction." + lineSeparator() +
        "The nodes to attach won't be activated and restarted, and their topology will be rolled back to their initial value."
    );
    newOnlineNodes.forEach((addr, cluster) -> {
      logger.info("Rollback topology of node: {}", addr);
      setUpcomingCluster(Collections.singletonList(addr), cluster);
    });
    throw error;
  }

  @Override
  protected Collection<InetSocketAddress> getAllOnlineSourceNodes() {
    return newOnlineNodes.keySet();
  }
}
