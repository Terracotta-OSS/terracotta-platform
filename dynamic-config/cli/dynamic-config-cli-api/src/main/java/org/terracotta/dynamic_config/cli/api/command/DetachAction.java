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
import org.terracotta.dynamic_config.api.model.Identifier;
import org.terracotta.dynamic_config.api.model.LockTag;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Node.Endpoint;
import org.terracotta.dynamic_config.api.model.PropertyHolder;
import org.terracotta.dynamic_config.api.model.Scope;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.UID;
import org.terracotta.dynamic_config.api.model.nomad.NodeRemovalNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.StripeRemovalNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.TopologyNomadChange;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.lang.System.lineSeparator;
import static org.terracotta.dynamic_config.api.model.FailoverPriority.Type.CONSISTENCY;
import static org.terracotta.dynamic_config.cli.api.converter.OperationType.NODE;
import static org.terracotta.dynamic_config.cli.api.converter.OperationType.STRIPE;

/**
 * @author Mathieu Carbou
 */
public class DetachAction extends TopologyAction {

  private static final Logger LOGGER = LoggerFactory.getLogger(DetachAction.class);

  protected Measure<TimeUnit> stopWaitTime = Measure.of(120, TimeUnit.SECONDS);
  protected Measure<TimeUnit> stopDelay = Measure.of(2, TimeUnit.SECONDS);
  protected Identifier sourceIdentifier;

  protected final Collection<Endpoint> onlineNodesToRemove = new ArrayList<>(1);

  protected PropertyHolder source;
  protected Stripe stripeToDetach;

  public void setStopWaitTime(Measure<TimeUnit> stopWaitTime) {
    this.stopWaitTime = stopWaitTime;
  }

  public void setStopDelay(Measure<TimeUnit> stopDelay) {
    this.stopDelay = stopDelay;
  }

  public void setSourceIdentifier(Identifier sourceIdentifier) {
    this.sourceIdentifier = sourceIdentifier;
  }

  @Override
  protected void validate() {
    super.validate();

    if (destinationCluster.getNodeCount() == 1) {
      throw new IllegalStateException("Unable to detach since destination cluster contains only 1 node");
    }

    if (operationType == NODE) {

      source = sourceIdentifier.findObject(destinationCluster, Scope.NODE)
          .orElseThrow(() -> new IllegalStateException("Source: " + sourceIdentifier + " is not part of cluster: " + destinationCluster.toShapeString()));

      if (destination.getNodeUID().equals(source.getUID())) {
        throw new IllegalArgumentException("The destination and the source nodes must not be the same");
      }

      if (!destinationCluster.inSameStripe(source.getUID(), destination.getNodeUID()).isPresent()) {
        throw new IllegalStateException("Source node: " + sourceIdentifier + " is not present in the same stripe as destination: " + destination);
      }

      Stripe destinationStripe = destinationCluster.getStripeByNode(destination.getNodeUID()).get();
      if (destinationStripe.getNodeCount() == 1) {
        throw new IllegalStateException("Unable to detach since destination stripe contains only 1 node");
      }

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
              "is an odd number, which will become even with the removal of node " + sourceIdentifier + "." + lineSeparator() +
              "An even-numbered configuration is more likely to experience split-brain situations." + lineSeparator() +
              "===================================================================================" + lineSeparator());
        }
      }

      // we only prevent detaching nodes if some remaining nodes must be restarted
      for (Endpoint endpoint : destinationOnlineNodes.keySet()) {
        if (!endpoint.getNodeUID().equals(source.getUID())) {
          // prevent any topology change if a configuration change has been made through Nomad, requiring a restart, but nodes were not restarted yet.
          // we only check the remaining nodes, not the departing nodes.
          validateLogOrFail(
              () -> !mustBeRestarted(endpoint),
              "Impossible to do any topology change. Node: " + endpoint + " is waiting to be restarted to apply some pending changes. Please refer to the Troubleshooting Guide for more help.");
        }
      }

      // when we want to detach a node
      markNodeForRemoval(source.getUID());
    } else {

      source = sourceIdentifier.findObject(destinationCluster, Scope.STRIPE)
          .orElseThrow(() -> new IllegalStateException("Source: " + sourceIdentifier + " is not part of cluster: " + destinationCluster.toShapeString()));

      stripeToDetach = destinationCluster.getStripe(source.getUID()).get();
      if (stripeToDetach.containsNode(destination.getNodeUID())) {
        throw new IllegalStateException("Source: " + sourceIdentifier + " and destination: " + destination + " are part of the same stripe: " + stripeToDetach.toShapeString());
      }

      if (destinationClusterActivated) {
        if (destinationCluster.getStripeId(source.getUID()).getAsInt() == 1) {
          throw new IllegalStateException("Removing the leading stripe is not allowed");
        }
      }

      // we only prevent detaching nodes if some remaining nodes must be restarted
      for (Endpoint endpoint : destinationOnlineNodes.keySet()) {
        if (!stripeToDetach.containsNode(endpoint.getNodeUID())) {
          // prevent any topology change if a configuration change has been made through Nomad, requiring a restart, but nodes were not restarted yet.
          // we only check the remaining nodes, not the departing nodes.
          validateLogOrFail(
              () -> !mustBeRestarted(endpoint),
              "Impossible to do any topology change. Node: " + endpoint + " is waiting to be restarted to apply some pending changes. Please refer to the Troubleshooting Guide for more help.");
        }
      }

      validateLogOrFail(
          () -> !destinationClusterActivated || !findScalingVetoer(destinationOnlineNodes).isPresent(),
          "Scaling operation cannot be performed. Please refer to the Troubleshooting Guide for more help.");

      // when we want to detach a stripe, we detach all the nodes of the stripe
      stripeToDetach.getNodes().stream().map(Node::getUID).forEach(this::markNodeForRemoval);
    }

    // When the operation type is node, the nodes being detached should be stopped first manually
    // But if the operation type is stripe, the stripes being detached are stopped automatically after they're removed
    if (operationType == NODE) {
      if (!onlineNodesToRemove.isEmpty() && areAllNodesActivated(onlineNodesToRemove)) {
        validateLogOrFail(onlineNodesToRemove::isEmpty, "Nodes to be detached: " + toString(onlineNodesToRemove) + " are online. " +
            "Nodes must be safely shutdown first. Please refer to the Troubleshooting Guide for more help.");
      }
    }
  }

  @Override
  protected Consumer<RuntimeException> onExecuteError() {
    return isScaleInOrOut() ? e -> denyScaleIn(destinationCluster, destinationOnlineNodes) : e -> {};
  }

  @Override
  protected String buildLockTag() {
    return operationType == STRIPE ?
        (LockTag.SCALE_IN_PREFIX + stripeToDetach.getUID()) :
        (LockTag.NODE_DEL_PREFIX + onlineNodesToRemove.stream().map(Endpoint::getNodeUID).map(UID::toString).collect(Collectors.joining(":")));
  }

  @Override
  protected Cluster updateTopology() {
    Cluster cluster = destinationCluster.clone();

    switch (operationType) {

      case NODE: {
        output.info("Detaching node: {} from cluster: {}", source.getName(), destinationCluster.getName());
        cluster.removeNode(source.getUID());
        break;
      }

      case STRIPE: {
        output.info("Detaching stripe: {} from cluster: {}", source.getName(), destinationCluster.getName());
        cluster.removeStripe(source.getUID());
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
        return new NodeRemovalNomadChange(
            result,
            destinationCluster.getStripeByNode(source.getUID()).get().getUID(),
            destinationCluster.getNode(source.getUID()).get());
      case STRIPE: {
        return new StripeRemovalNomadChange(result, stripeToDetach);
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
      // we need to update the list of runtime peers:
      // - to remove the node that is gone from this list
      // - to check the server states again because:
      //    * a failover could happen after removal
      //    * a passive blocked could not become active in consistency mode
      destinationOnlineNodes.keySet().removeAll(onlineNodesToRemove);
      destinationOnlineNodes = getLogicalServerStates(destinationOnlineNodes.keySet());
    }
  }

  @Override
  protected void onNomadChangeSuccess(TopologyNomadChange nomadChange) {
    tryFinally(() -> {
      if (isUnlockRequired()) {
        unlock(nomadChange);
      }
    }, () -> {
      // When the operation type is node, the nodes being detached should be stopped first manually
      // But if the operation type is stripe, the stripes being detached are stopped automatically after they're removed
      if (operationType == STRIPE) {
        resetAndStopNodesToRemove();
      }
    });
  }

  @Override
  protected Collection<Endpoint> getAllOnlineSourceNodes() {
    return onlineNodesToRemove;
  }

  private void resetAndStopNodesToRemove() {
    if (!onlineNodesToRemove.isEmpty()) {

      output.info("Reset nodes: {}", toString(onlineNodesToRemove));

      for (Endpoint endpoint : onlineNodesToRemove) {
        try {
          reset(endpoint);
        } catch (RuntimeException e) {
          LOGGER.warn("Error during reset of node: {}: {}", endpoint, e.getMessage(), e);
        }
      }

      output.info("Stopping nodes: {}", toString(onlineNodesToRemove));

      stopNodes(
          onlineNodesToRemove,
          Duration.ofMillis(stopWaitTime.getQuantity(TimeUnit.MILLISECONDS)),
          Duration.ofMillis(stopDelay.getQuantity(TimeUnit.MILLISECONDS)));

      // if we have stopped some nodes, we need to update the list of nodes online
      destinationOnlineNodes.keySet().removeAll(onlineNodesToRemove);

      // if a failover happened, make sure we get the new server states
      destinationOnlineNodes.entrySet().forEach(e -> e.setValue(getLogicalServerState(e.getKey())));
    }
  }

  private void markNodeForRemoval(UID nodeUID) {
    // search if this node is online, if yes, mark it for removal
    // "onlineNodesToRemove" keeps track of the nodes to connect to,
    // to update their topology
    destinationOnlineNodes.keySet()
        .stream()
        .filter(endpoint -> endpoint.getNodeUID().equals(nodeUID))
        .findAny()
        .ifPresent(onlineNodesToRemove::add);
  }
}
