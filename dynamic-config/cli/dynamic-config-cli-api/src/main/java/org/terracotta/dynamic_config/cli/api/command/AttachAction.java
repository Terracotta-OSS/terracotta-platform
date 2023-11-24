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
package org.terracotta.dynamic_config.cli.api.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.common.struct.Measure;
import org.terracotta.common.struct.TimeUnit;
import org.terracotta.common.struct.Tuple2;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.FailoverPriority;
import org.terracotta.dynamic_config.api.model.LockTag;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Node.Endpoint;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.UID;
import org.terracotta.dynamic_config.api.model.nomad.NodeAdditionNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.StripeAdditionNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.TopologyNomadChange;
import org.terracotta.dynamic_config.api.service.NameGenerator;
import org.terracotta.inet.HostPort;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import static java.lang.System.lineSeparator;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.terracotta.dynamic_config.api.model.FailoverPriority.Type.CONSISTENCY;
import static org.terracotta.dynamic_config.cli.api.converter.OperationType.NODE;
import static org.terracotta.dynamic_config.cli.api.converter.OperationType.STRIPE;

/**
 * @author Mathieu Carbou
 */
public class AttachAction extends TopologyAction {

  private static final Logger LOGGER = LoggerFactory.getLogger(AttachAction.class);

  protected Measure<TimeUnit> restartWaitTime = Measure.of(120, TimeUnit.SECONDS);
  protected Measure<TimeUnit> restartDelay = Measure.of(2, TimeUnit.SECONDS);

  // list of new nodes to add with their backup topology
  protected final Map<Endpoint, NodeContext> sources = new LinkedHashMap<>();
  protected Stripe addedStripe;
  protected String optionalStripeName;
  protected Node addedNode;

  public void setSourceHostPort(HostPort sourceHostPort) {
    this.sources.clear();
    getUpcomingNodeContext(sourceHostPort).accept(sources::put);
  }

  /**
   * For each node acting as a direct source for the command, we connect to the node
   * to determine the endpoint and fetch its topology
   */
  public void setStripeFromShape(Collection<HostPort> sourceHostPorts, String optionalStripeName) {
    this.sources.clear();
    sourceHostPorts.forEach(hostPort -> getUpcomingNodeContext(hostPort).accept(sources::put));
    this.optionalStripeName = optionalStripeName != null ? optionalStripeName : sources.values().stream()
        .map(nodeContext -> nodeContext.getCluster().getStripeByNode(nodeContext.getNodeUID()).get().getName())
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  /**
   * For a node part of a stripe, we connect to the node to get the stripe topology, then we
   * fetch all the nodes of this stripe
   */
  public void setStripeFromSource(HostPort sourceStripeHostPort) {
    final Tuple2<Endpoint, NodeContext> context = getUpcomingNodeContext(sourceStripeHostPort);
    final Stripe stripe = context.t2.getStripe();
    this.optionalStripeName = stripe.getName();
    this.sources.clear();
    stripe.getNodes()
        .forEach(node -> sources.put(
            node.determineEndpoint(context.t1),
            new NodeContext(context.t2.getCluster(), node.getUID())));
  }

  public void setRestartWaitTime(Measure<TimeUnit> restartWaitTime) {
    this.restartWaitTime = restartWaitTime;
  }

  public void setRestartDelay(Measure<TimeUnit> restartDelay) {
    this.restartDelay = restartDelay;
  }

  @Override
  protected void validate() {
    super.validate();

    if (operationType == NODE) {
      if (sources.size() > 1) {
        throw new UnsupportedOperationException("Cannot attach more than 1 node at a time");
      }
    }

    for (Endpoint source : sources.keySet()) {
      if (destination.getNodeUID().equals(source.getNodeUID())) {
        throw new IllegalArgumentException("The destination and the source endpoints must not be the same");
      }
    }

    // we prevent attaching nodes if some nodes must be restarted
    for (Endpoint endpoint : destinationOnlineNodes.keySet()) {
      // prevent any topology change if a configuration change has been made through Nomad, requiring a restart, but nodes were not restarted yet
      validateLogOrFail(
          () -> !mustBeRestarted(endpoint),
          "Impossible to do any topology change. Node: " + endpoint + " is waiting to be restarted to apply some pending changes. Please refer to the Troubleshooting Guide for more help.");
    }

    for (Endpoint source : sources.keySet()) {
      Collection<Endpoint> destinationPeers = destinationCluster.determineEndpoints(destination);
      if (destinationPeers.contains(source)) {
        throw new IllegalArgumentException("Source node: " + source + " is already part of cluster: " + destinationCluster.toShapeString());
      }

      if (isActivated(source)) {
        throw new IllegalArgumentException("Source node: " + source + " cannot be attached since it is part of an existing cluster with name: " + getRuntimeCluster(source).getName());
      }
    }

    /*
     We can only attach some nodes if these conditions are met:
     - either the source node is alone in its own cluster
     - either the source node is part of a 1-stripe cluster, and we attach the whole stripe

     We shouldn't allow the user to attach to another "cluster B" a node that is already part of a "cluster A" (containing several nodes),
     except if all the nodes of this "cluster A" are attached to "cluster B".

     The goal is to not have some leftover nodes potentially having wrong information about their topology.
     If that happens, the user must first detach the nodes, then re-attach them somewhere else. The detach process will
     update the topology of "cluster A" so that the detached nodes won't be there anymore and can be attached somewhere else.
     "cluster A" could then be activated without impacting the "cluster B" used in the attach command.
     */
    Stripe destinationStripe = destinationCluster.getStripeByNode(destination.getNodeUID()).get();
    if (operationType == NODE) {
      for (Map.Entry<Endpoint, NodeContext> entry : sources.entrySet()) {
        Endpoint source = entry.getKey();
        Cluster sourceCluster = entry.getValue().getCluster();

        validateLogOrFail(
            () -> sourceCluster.getNodeCount() == 1,
            "Source node: " + source + " is part of a stripe containing more than 1 nodes. " +
                "It must be detached first before being attached to a new stripe. " +
                "Please refer to the Troubleshooting Guide for more help.");

        FailoverPriority failoverPriority = destinationCluster.getFailoverPriority().orElse(null);
        if (failoverPriority != null && failoverPriority.getType() == CONSISTENCY) {
          int voterCount = failoverPriority.getVoters();
          int nodeCount = destinationStripe.getNodes().size();
          int sum = voterCount + nodeCount;
          if (sum % 2 != 0) {
            LOGGER.warn(lineSeparator() +
                "===================================================================================" + lineSeparator() +
                "IMPORTANT: The sum (" + sum + ") of voter count (" + voterCount + ") and number of nodes " +
                "(" + nodeCount + ") in this stripe " + lineSeparator() +
                "is an odd number, which will become even with the addition of node " + source + "." + lineSeparator() +
                "An even-numbered configuration is more likely to experience split-brain situations." + lineSeparator() +
                "===================================================================================" + lineSeparator());
          }
        }
      }
    }

    if (operationType == STRIPE) {
      for (Map.Entry<Endpoint, NodeContext> entry : sources.entrySet()) {
        Endpoint source = entry.getKey();
        Cluster sourceCluster = entry.getValue().getCluster();
        validateLogOrFail(
            () -> sourceCluster.getStripeCount() == 1,
            "Source stripe from node: " + source + " is part of a cluster containing more than 1 stripes. " +
                "It must be detached first before being attached to a new cluster. " +
                "Please refer to the Troubleshooting Guide for more help.");
      }

      // we can attach stripes in 2 ways:
      // 1. by providing -stripe parameter and point to a stripe (its nodes will be discovered)
      // 2. by providing -stripe-shape and pointing to some nodes alone, and the stripe will be formed
      // In all cases, we must verify that the list of sources we have contains all the nodes of all the known stripes.
      // In case 2), nodes are supposed to be alone in their own stripes and in case 1) all the sources should be part of the targeted stripe.
      Collection<UID> allNodesUID = sources.keySet().stream().map(Endpoint::getNodeUID).collect(toSet());
      for (Map.Entry<Endpoint, NodeContext> entry : sources.entrySet()) {
        final Collection<UID> nodesInStripe = entry.getValue().getStripe().getNodes().stream().map(Node::getUID).collect(toSet());
        nodesInStripe.removeAll(allNodesUID);
        if (!nodesInStripe.isEmpty()) {
          throw new IllegalArgumentException("Source node: " + entry.getKey() + " points to a stripe with more than one node and the following nodes were not marked to be attached: " + toString(nodesInStripe));
        }
      }

      validateLogOrFail(
          () -> !destinationClusterActivated || !findScalingVetoer(destinationOnlineNodes).isPresent(),
          "Scaling operation cannot be performed. Please refer to the Troubleshooting Guide for more help.");
    }

    switch (operationType) {
      case NODE: {
        // we only support 1 node for attach at the moment
        addedNode = sources.values().iterator().next().getNode()
            .clone()
            .setUID(destinationCluster.newUID());

        if (destinationClusterActivated) {
          NameGenerator.assignFriendlyNodeName(destinationCluster, destinationStripe, addedNode);
        }
        break;
      }
      case STRIPE: {
        addedStripe = new Stripe()
            .setUID(destinationCluster.newUID())
            .setName(optionalStripeName)
            .setNodes(sources.values()
                .stream()
                .map(NodeContext::getNode)
                .map(Node::clone)
                .map(node -> node.setUID(destinationCluster.newUID()))
                .collect(toList()));

        if (destinationClusterActivated) {
          NameGenerator.assignFriendlyNames(destinationCluster, addedStripe);
        }
        break;
      }
    }
  }

  @Override
  protected Consumer<RuntimeException> onExecuteError() {
    return isScaleInOrOut() ? e -> denyScaleOut(destinationCluster, destinationOnlineNodes) : e -> {};
  }

  @Override
  protected String buildLockTag() {
    return operationType == STRIPE ?
        LockTag.SCALE_OUT_PREFIX + addedStripe.getUID() :
        LockTag.NODE_ADD_PREFIX + addedNode.getUID();
  }

  @Override
  protected Cluster updateTopology() {
    Cluster cluster = destinationCluster.clone();

    switch (operationType) {

      case NODE: {
        output.info("Attaching node: {} to stripe: {}", addedNode.toShapeString(), cluster.getStripeByNode(destination.getNodeUID()).get().toShapeString());
        cluster.getStripeByNode(destination.getNodeUID()).orElseThrow(AssertionError::new).addNode(addedNode);
        break;
      }

      case STRIPE: {
        output.info("Attaching a new stripe: {} to cluster: {}", addedStripe.toShapeString(), destinationCluster.getName());
        cluster.addStripe(addedStripe);
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
    setUpcomingCluster(sources.keySet(), nomadChange.getCluster());
  }

  @Override
  protected void onNomadChangeSuccess(TopologyNomadChange nomadChange) {
    tryFinally(() -> activate(nomadChange), () -> {
      if (isUnlockRequired()) {
        unlock(nomadChange);
      }
    });
  }

  protected final void activate(TopologyNomadChange nomadChange) {
    Cluster result = nomadChange.getCluster();
    switch (operationType) {
      case NODE:
        activateNodes(sources.keySet(), result, null, restartDelay, restartWaitTime);
        break;
      case STRIPE:
        activateStripe(sources.keySet(), result, destination, restartDelay, restartWaitTime);
        break;
      default:
        throw new UnsupportedOperationException(operationType.name());
    }
  }

  @Override
  protected void onNomadChangeFailure(TopologyNomadChange nomadChange, RuntimeException error) {
    LOGGER.error("An error occurred during the attach transaction." + lineSeparator() +
        "The node/stripe information may still be added to the destination cluster: you will need to run the diagnostic / export command to check the state of the transaction." + lineSeparator() +
        "The node/stripe to attach won't be activated and restarted, and their topology will be rolled back to their initial value."
    );
    sources.forEach((endpoint, nodeContext) -> {
      try {
        output.info("Rollback topology of node: {}", endpoint);
        setUpcomingCluster(singleton(endpoint), nodeContext.getCluster());
      } catch (RuntimeException e) {
        LOGGER.warn("Unable to rollback configuration on node: {}. Error: {}", endpoint, e.getMessage(), e);
      }
    });
    throw error;
  }

  @Override
  protected Collection<Endpoint> getAllOnlineSourceNodes() {
    return sources.keySet();
  }
}
