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
import org.terracotta.dynamic_config.api.model.Node.Endpoint;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.nomad.NodeAdditionNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.StripeAdditionNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.TopologyNomadChange;
import org.terracotta.dynamic_config.api.service.ClusterConfigMismatchException;
import org.terracotta.dynamic_config.api.service.MutualClusterValidator;
import org.terracotta.dynamic_config.api.service.NameGenerator;
import org.terracotta.dynamic_config.cli.command.Usage;
import org.terracotta.dynamic_config.cli.converter.InetSocketAddressConverter;
import org.terracotta.dynamic_config.cli.converter.TimeUnitConverter;

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

  @Parameter(required = true, names = {"-s"}, description = "Source node or stripe", converter = InetSocketAddressConverter.class)
  protected InetSocketAddress sourceAddress;

  // list of new nodes to add with their backup topology
  private final Map<Endpoint, Cluster> newOnlineNodes = new LinkedHashMap<>();

  protected Endpoint source;
  private Cluster sourceCluster;
  private Stripe addedStripe;
  private Node addedNode;

  @Override
  public void validate() {
    super.validate();

    source = getEndpoint(sourceAddress);
    sourceCluster = getUpcomingCluster(source);

    if (destination.getNodeUID().equals(source.getNodeUID())) {
      throw new IllegalArgumentException("The destination and the source endpoints must not be the same");
    }

    Collection<Endpoint> destinationPeers = destinationCluster.getSimilarEndpoints(destination);
    if (destinationPeers.contains(source)) {
      throw new IllegalArgumentException("Source node: " + source + " is already part of cluster: " + destinationCluster.toShapeString());
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

      Stripe destinationStripe = destinationCluster.getStripeByNode(destination.getNodeUID()).get();
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
      sourceCluster.getStripeByNode(source.getNodeUID()).get()
          .getSimilarEndpoints(source)
          .forEach(endpoint -> newOnlineNodes.put(endpoint, getUpcomingCluster(endpoint)));
    }
  }

  @Override
  protected Cluster updateTopology() {
    Cluster cluster = destinationCluster.clone();

    switch (operationType) {

      case NODE: {
        logger.info("Attaching node: {} to stripe: {}", source, cluster.getStripeByNode(destination.getNodeUID()).get().toShapeString());
        Stripe stripe = cluster.getStripeByNode(destination.getNodeUID()).get();
        Node node = sourceCluster.getNode(source.getNodeUID()).get();

        addedNode = node.clone();
        stripe.addNode(addedNode);

        // change the node UID
        addedNode.setUID(cluster.newUID());

        if (destinationClusterActivated) {
          NameGenerator.assignFriendlyNodeName(cluster, addedNode.getUID());
        }
        break;
      }

      case STRIPE: {
        Stripe stripe = sourceCluster.getStripeByNode(source.getNodeUID()).get();
        logger.info("Attaching a new stripe: {} to cluster: {}", stripe.toShapeString(), destinationCluster.getName());

        addedStripe = stripe.clone();
        cluster.addStripe(addedStripe);

        // change the stripe UID and all its nodes
        addedStripe.setUID(cluster.newUID());
        addedStripe.getNodes().forEach(n -> n.setUID(cluster.newUID()));

        if (destinationClusterActivated) {
          NameGenerator.assignFriendlyNames(cluster, addedStripe.getUID());
        } 
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
        return new NodeAdditionNomadChange(result, result.getStripeByNode(addedNode.getUID()).get().getUID(), addedNode);
      case STRIPE: {
        return new StripeAdditionNomadChange(result, addedStripe);
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
    newOnlineNodes.forEach((endpoint, cluster) -> {
      logger.info("Rollback topology of node: {}", endpoint);
      setUpcomingCluster(Collections.singletonList(endpoint), cluster);
    });
    throw error;
  }

  @Override
  protected Collection<Endpoint> getAllOnlineSourceNodes() {
    return newOnlineNodes.keySet();
  }

  AttachCommand setSourceAddress(InetSocketAddress sourceAddress) {
    this.sourceAddress = sourceAddress;
    return this;
  }
}
