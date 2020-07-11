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
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import org.terracotta.common.struct.Measure;
import org.terracotta.common.struct.TimeUnit;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.FailoverPriority;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.nomad.NodeAdditionNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.StripeAdditionNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.TopologyNomadChange;
import org.terracotta.dynamic_config.api.service.ClusterConfigMismatchException;
import org.terracotta.dynamic_config.api.service.MutualClusterValidator;
import org.terracotta.dynamic_config.cli.command.DeprecatedParameter;
import org.terracotta.dynamic_config.cli.command.DeprecatedUsage;
import org.terracotta.dynamic_config.cli.command.Usage;
import org.terracotta.dynamic_config.cli.converter.InetSocketAddressConverter;
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
@DeprecatedUsage("attach [-t node|stripe] -d <hostname[:port]> -s <hostname[:port]> [-f] [-W <restart-wait-time>] [-D <restart-delay>]")
@Usage("attach (-to-cluster <hostname[:port]> -stripe <hostname[:port]> | -to-stripe <hostname[:port]> -node <hostname[:port]>)" +
    "[-restart-wait-time <restart-wait-time>] [-restart-delay <restart-delay>] [-force]")
public class AttachCommand extends TopologyCommand {

  @Parameter(names = "-to-cluster", description = "Destination cluster", converter = InetSocketAddressConverter.class)
  protected InetSocketAddress toCluster;

  @Parameter(names = "-to-stripe", description = "Destination stripe", converter = InetSocketAddressConverter.class)
  protected InetSocketAddress toStripe;

  @Parameter(names = "-stripe", description = "Source stripe", converter = InetSocketAddressConverter.class)
  protected InetSocketAddress sourceStripe;

  @Parameter(names = "-node", description = "Source node", converter = InetSocketAddressConverter.class)
  protected InetSocketAddress sourceNode;

  @DeprecatedParameter(names = "-W", description = "Maximum time to wait for the nodes to restart. Default: 60s", converter = TimeUnitConverter.class)
  @Parameter(names = "-restart-wait-time", description = "Maximum time to wait for the nodes to restart. Default: 60s", converter = TimeUnitConverter.class)
  protected Measure<TimeUnit> restartWaitTime = Measure.of(60, TimeUnit.SECONDS);

  @DeprecatedParameter(names = "-D", description = "Delay before the server restarts itself. Default: 2s", converter = TimeUnitConverter.class)
  @Parameter(names = "-restart-delay", description = "Delay before the server restarts itself. Default: 2s", converter = TimeUnitConverter.class)
  protected Measure<TimeUnit> restartDelay = Measure.of(2, TimeUnit.SECONDS);

  // list of new nodes to add with their backup topology
  private final Map<InetSocketAddress, Cluster> newOnlineNodes = new LinkedHashMap<>();

  private Cluster sourceCluster;

  @Override
  public void validate() {
    if (toCluster != null && toStripe != null) {
      throw new ParameterException("-to-cluster and -to-stripe cannot be specified together");
    }
    if (sourceStripe != null && sourceNode != null) {
      throw new ParameterException("-node and -stripe cannot be specified together");
    }
    if (toCluster != null && sourceNode != null) {
      throw new ParameterException("-to-cluster and -node cannot be specified together");
    }
    if (toStripe != null && sourceNode != null) {
      throw new ParameterException("-to-stripe and -stripe cannot be specified together");
    }

    // Translate the new options to the deprecated options
    destination = toCluster != null ? toCluster : toStripe;
    source = sourceNode != null ? sourceNode : sourceStripe;
    operationType = toCluster != null ? STRIPE : NODE;

    super.validate();

    sourceCluster = getUpcomingCluster(source);

    Collection<InetSocketAddress> destinationPeers = destinationCluster.getNodeAddresses();
    if (InetSocketAddressUtils.contains(destinationPeers, source)) {
      throw new IllegalArgumentException("Source node: " + source + " is already part of cluster at: " + destination);
    }

    if (isActivated(source)) {
      throw new IllegalArgumentException("Source node: " + source + " cannot be attached since it is part of an existing cluster with name: " + getRuntimeCluster(source).getName());
    }

    MutualClusterValidator mutualClusterValidator = new MutualClusterValidator(destinationCluster, sourceCluster);
    try {
      mutualClusterValidator.validate();
    } catch (ClusterConfigMismatchException e) {
      validateLogOrFail(() -> false, e.getMessage());
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
              "You can run the command with the force option to force the attachment, but at the risk of breaking the cluster from where the node is taken.");

      Stripe destinationStripe = destinationCluster.getStripe(destination).get();
      FailoverPriority failoverPriority = destinationCluster.getFailoverPriority();
      if (failoverPriority.equals(consistency())) {
        int voterCount = failoverPriority.getVoters();
        int nodeCount = destinationStripe.getNodes().size();
        int sum = voterCount + nodeCount;
        if (sum % 2 != 0) {
          logger.warn(lineSeparator() +
              "===================================================================================" + lineSeparator() +
              "IMPORTANT: The sum (" + sum + ") of voter count (" + voterCount + ") and number of nodes " +
              "(" + nodeCount + ") in this stripe " + lineSeparator() +
              "is an odd number, which will become even with the addition of node " + source + "." + lineSeparator() +
              "An even-numbered configuration is more likely to experience split-brain situations." + lineSeparator() +
              "===================================================================================" + lineSeparator());
        }
      }
    }

    if (operationType == STRIPE) {
      validateLogOrFail(
          () -> sourceCluster.getStripeCount() == 1,
          "Source stripe from node: " + source + " is part of a cluster containing more than 1 stripes. " +
              "It must be detached first before being attached to a new cluster. " +
              "You can run the command with the force option to force the attachment, but at the risk of breaking the cluster from where the node is taken.");
    }

    // make sure nodes to attach are online
    // building the list of nodes
    if (operationType == NODE) {
      // we attach only a node
      newOnlineNodes.put(source, sourceCluster);
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

        Node clone = node.clone();
        stripe.addNode(clone);

        // change the node UID
        clone.setUID(cluster.newUID());

        break;
      }

      case STRIPE: {
        Stripe stripe = sourceCluster.getStripe(source).get();
        logger.info("Attaching a new stripe formed with nodes: {} to cluster: {}", toString(stripe.getNodeAddresses()), destination);

        Stripe clone = stripe.clone();
        cluster.addStripe(clone);

        // change the stripe UID and all its nodes
        clone.setUID(cluster.newUID());
        clone.getNodes().forEach(n -> n.setUID(cluster.newUID()));

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
  protected TopologyNomadChange buildNomadChange(Cluster result) {
    switch (operationType) {
      case NODE:
        return new NodeAdditionNomadChange(result, result.getStripeId(destination).getAsInt(), result.getNode(source).get());
      case STRIPE: {
        return new StripeAdditionNomadChange(result, result.getStripe(source).get());
      }
      default: {
        throw new UnsupportedOperationException(operationType.name());
      }
    }
  }

  @Override
  protected void onNomadChangeReady(TopologyNomadChange nomadChange) {
    setUpcomingCluster(newOnlineNodes.keySet(), nomadChange.getCluster());
  }

  @Override
  protected void onNomadChangeSuccess(TopologyNomadChange nomadChange) {
    Cluster result = nomadChange.getCluster();
    switch (operationType) {
      case NODE:
        activateNodes(newOnlineNodes.keySet(), result, null, restartDelay, restartWaitTime);
        break;
      case STRIPE:
        activateStripe(newOnlineNodes.keySet(), result, destination, restartDelay, restartWaitTime);
        break;
      default:
        throw new UnsupportedOperationException(operationType.name());
    }
  }

  @Override
  protected void onNomadChangeFailure(TopologyNomadChange nomadChange, RuntimeException error) {
    logger.error("An error occurred during the attach transaction." + lineSeparator() +
        "The node/stripe information may still be added to the destination cluster: you will need to run the diagnostic / export command to check the state of the transaction." + lineSeparator() +
        "The node/stripe to attach won't be activated and restarted, and their topology will be rolled back to their initial value."
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
