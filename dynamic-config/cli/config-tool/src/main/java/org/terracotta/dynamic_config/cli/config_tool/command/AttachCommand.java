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
import org.terracotta.common.struct.TimeUnit;
import org.terracotta.common.struct.Tuple2;
import org.terracotta.diagnostic.client.connection.DiagnosticServices;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.nomad.NodeAdditionNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.NodeNomadChange;
import org.terracotta.dynamic_config.cli.command.Usage;
import org.terracotta.inet.InetSocketAddressUtils;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Collection;

import static java.util.Collections.singletonList;
import static org.terracotta.dynamic_config.cli.config_tool.converter.OperationType.NODE;
import static org.terracotta.dynamic_config.cli.config_tool.converter.OperationType.STRIPE;

/**
 * @author Mathieu Carbou
 */
@Parameters(commandNames = "attach", commandDescription = "Attach a node to a stripe, or a stripe to a cluster")
@Usage("attach [-t node|stripe] -d <hostname[:port]> -s <hostname[:port]> [-f] [-W <restart-wait-time>] [-D <restart-delay>]")
public class AttachCommand extends TopologyCommand {

  @Override
  public void validate() {
    super.validate();

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
  protected void beforeNomadChange(Cluster result) {
    Collection<InetSocketAddress> newNodes = getNewNodes();

    setUpcomingCluster(newNodes, result);

    logger.info("Activating nodes to attach: {}", toString(newNodes));
    try (DiagnosticServices diagnosticServices = multiDiagnosticServiceProvider.fetchOnlineDiagnosticServices(newNodes)) {
      // we are preparing the Nomad system only on the new nodes
      // we activate the passive node without any license: the license will be synced from active and installed
      dynamicConfigServices(diagnosticServices)
          .map(Tuple2::getT2)
          .forEach(service -> service.activate(result, null));
    }

    // we are running a cluster activation only on the new nodes
    runClusterActivation(newNodes, result);
    logger.debug("Configuration repositories have been created for nodes: {}", toString(newNodes));
  }

  @Override
  protected void afterNomadChange(Cluster result) {
    Collection<InetSocketAddress> newNodes = getNewNodes();

    logger.info("Restarting nodes: {}", toString(newNodes));
    restartNodes(
        newNodes,
        Duration.ofMillis(restartWaitTime.getQuantity(TimeUnit.MILLISECONDS)),
        Duration.ofMillis(restartDelay.getQuantity(TimeUnit.MILLISECONDS)));
    logger.info("All nodes came back up");
  }

  private Collection<InetSocketAddress> getNewNodes() {
    return operationType == NODE ? singletonList(source) : sourceCluster.getStripe(source).get().getNodeAddresses();
  }
}
