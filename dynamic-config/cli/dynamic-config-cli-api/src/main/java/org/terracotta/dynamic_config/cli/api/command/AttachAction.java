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
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.FailoverPriority;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Node.Endpoint;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.nomad.NodeAdditionNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.StripeAdditionNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.TopologyNomadChange;
import org.terracotta.dynamic_config.api.service.NameGenerator;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.lang.System.lineSeparator;
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
  protected InetSocketAddress sourceAddress;

  // list of new nodes to add with their backup topology
  protected final Map<Endpoint, Cluster> newOnlineNodes = new LinkedHashMap<>();

  protected Endpoint source;
  protected Cluster sourceCluster;
  protected Stripe addedStripe;
  protected Node addedNode;

  public void setSourceAddress(InetSocketAddress sourceAddress) {
    this.sourceAddress = sourceAddress;
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

    source = getEndpoint(sourceAddress);
    sourceCluster = getUpcomingCluster(source);

    if (destination.getNodeUID().equals(source.getNodeUID())) {
      throw new IllegalArgumentException("The destination and the source endpoints must not be the same");
    }

    // we prevent attaching nodes if some nodes must be restarted
    for (Endpoint endpoint : destinationOnlineNodes.keySet()) {
      // prevent any topology change if a configuration change has been made through Nomad, requiring a restart, but nodes were not restarted yet
      validateLogOrFail(
          () -> !mustBeRestarted(endpoint),
          "Impossible to do any topology change. Node: " + endpoint + " is waiting to be restarted to apply some pending changes. Please refer to the Troubleshooting Guide for more help.");
    }

    Collection<Endpoint> destinationPeers = destinationCluster.determineEndpoints(destination);
    if (destinationPeers.contains(source)) {
      throw new IllegalArgumentException("Source node: " + source + " is already part of cluster: " + destinationCluster.toShapeString());
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
    Stripe destinationStripe = destinationCluster.getStripeByNode(destination.getNodeUID()).get();
    if (operationType == NODE) {
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

    if (operationType == STRIPE) {
      validateLogOrFail(
          () -> sourceCluster.getStripeCount() == 1,
          "Source stripe from node: " + source + " is part of a cluster containing more than 1 stripes. " +
              "It must be detached first before being attached to a new cluster. " +
              "Please refer to the Troubleshooting Guide for more help.");
    }

    // make sure nodes to attach are online
    // building the list of nodes
    if (operationType == NODE) {
      // we attach only a node
      newOnlineNodes.put(source, sourceCluster);
    } else {
      // we attach a whole stripe
      sourceCluster.getStripeByNode(source.getNodeUID()).get().getNodes().stream()
          .map(node -> node.determineEndpoint(source))
          .forEach(endpoint -> newOnlineNodes.put(endpoint, getUpcomingCluster(endpoint)));
    }

    switch (operationType) {
      case NODE: {
        addedNode = sourceCluster.getNode(source.getNodeUID()).get().clone();
        addedNode.setUID(destinationCluster.newUID());

        if (destinationClusterActivated) {
          NameGenerator.assignFriendlyNodeName(destinationCluster, destinationStripe, addedNode);
        }
        break;
      }
      case STRIPE: {
        addedStripe = sourceCluster.getStripeByNode(source.getNodeUID()).get().clone();
        addedStripe.setUID(destinationCluster.newUID());
        addedStripe.getNodes().forEach(n -> n.setUID(destinationCluster.newUID()));

        if (destinationClusterActivated) {
          NameGenerator.assignFriendlyNames(destinationCluster, addedStripe);
        }
        break;
      }
    }
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
    LOGGER.error("An error occurred during the attach transaction." + lineSeparator() +
        "The node/stripe information may still be added to the destination cluster: you will need to run the diagnostic / export command to check the state of the transaction." + lineSeparator() +
        "The node/stripe to attach won't be activated and restarted, and their topology will be rolled back to their initial value."
    );
    newOnlineNodes.forEach((endpoint, cluster) -> {
      try {
        output.info("Rollback topology of node: {}", endpoint);
        setUpcomingCluster(Collections.singletonList(endpoint), cluster);
      } catch (RuntimeException e) {
        LOGGER.warn("Unable to rollback configuration on node: {}. Error: {}", endpoint, e.getMessage(), e);
      }
    });
    throw error;
  }

  @Override
  protected Collection<Endpoint> getAllOnlineSourceNodes() {
    return newOnlineNodes.keySet();
  }

}
