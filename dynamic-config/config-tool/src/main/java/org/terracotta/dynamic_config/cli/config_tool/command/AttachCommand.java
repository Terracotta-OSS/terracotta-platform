/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.cli.config_tool.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.terracotta.common.struct.Measure;
import org.terracotta.common.struct.TimeUnit;
import org.terracotta.common.struct.Tuple2;
import org.terracotta.diagnostic.client.connection.DiagnosticServices;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.nomad.ClusterActivationNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.NodeAdditionNomadChange;
import org.terracotta.dynamic_config.cli.command.Usage;
import org.terracotta.dynamic_config.cli.config_tool.converter.OperationType;
import org.terracotta.dynamic_config.cli.converter.TimeUnitConverter;
import org.terracotta.nomad.client.change.NomadChange;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * @author Mathieu Carbou
 */
@Parameters(commandNames = "attach", commandDescription = "Attach a node to a destination stripe or attach a stripe to a destination cluster")
@Usage("attach [-t node|stripe] -d <hostname[:port]> -s <hostname[:port]>,<hostname[:port]>... [-f] [-W <wait-time>] [-D <restart-delay>]")
public class AttachCommand extends TopologyCommand {

  @Parameter(names = {"-W"}, description = "Maximum time to wait for the nodes to restart. Default: 60s", converter = TimeUnitConverter.class)
  private Measure<TimeUnit> restartWaitTime = Measure.of(60, TimeUnit.SECONDS);

  @Parameter(names = {"-D"}, description = "Restart delay. Default: 2s", converter = TimeUnitConverter.class)
  private Measure<TimeUnit> restartDelay = Measure.of(2, TimeUnit.SECONDS);

  @Override
  public void validate() {
    super.validate();

    Collection<InetSocketAddress> destinationPeers = destinationCluster.getNodeAddresses();
    sourceClusters.forEach((addr, cluster) -> {
      if (destinationPeers.contains(addr)) {
        throw new IllegalStateException("Source node: " + addr + " is already part of cluster at: " + destination);
      }
      if (isActivated(addr)) {
        throw new IllegalStateException("Source node: " + addr + " cannot be attached since it has been activated and is part of an existing cluster");
      }
      validateLogOrFail(
          () -> cluster.getNodeCount() == 1,
          "Source node: " + addr + " points to a cluster with more than 1 node. " +
              "It must be properly detached first before being attached to a new stripe. " +
              "You can run the command with -f option to force the attachment but at the risk of breaking the cluster from where the node is taken.");
    });
  }

  @Override
  protected Cluster updateTopology() {
    Cluster cluster = destinationCluster.clone();

    switch (operationType) {

      case NODE: {
        logger.info("Attaching nodes {} to stripe {}", toString(sources), destination);
        Stripe stripe = cluster.getStripe(destination).get();
        sourceClusters.forEach((addr, c) -> stripe.attachNode(c.getNode(addr).get()));
        break;
      }

      case STRIPE: {
        logger.info("Attaching a new stripe formed with nodes {} to cluster {}", toString(sources), destination);
        List<Node> nodes = sourceClusters.entrySet().stream().map(e -> e.getValue().getNode(e.getKey()).get()).collect(toList());
        cluster.attachStripe(new Stripe(nodes));
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
  protected NomadChange buildNomadChange(Cluster result) {
    switch (operationType) {
      case NODE:
        return new NodeAdditionNomadChange(
            result.getStripeId(destination).getAsInt(),
            sources.stream().map(addr -> result.getNode(addr).get()).collect(toList()));
      case STRIPE: {
        throw new UnsupportedOperationException("Topology modifications of whole stripes on an activated cluster is not yet supported");
      }
      default: {
        throw new UnsupportedOperationException(operationType.name());
      }
    }
  }

  @Override
  protected void onNomadChangeCommitted(Cluster result) {
    logger.info("Activating nodes: {}", toString(sources));

    // we activate the passive node without any license: the license will be synced from active and installed
    try (DiagnosticServices diagnosticServices = multiDiagnosticServiceProvider.fetchOnlineDiagnosticServices(sources)) {
      dynamicConfigServices(diagnosticServices)
          .map(Tuple2::getT2)
          .forEach(service -> service.activate(result, null));
    }

    runNomadChange(new ArrayList<>(sources), new ClusterActivationNomadChange(result));
    logger.debug("Configuration repositories have been created for all nodes");

    logger.info("Restarting nodes: {}", toString(sources));
    restartNodes(
        sources,
        Duration.ofMillis(restartWaitTime.getQuantity(TimeUnit.MILLISECONDS)),
        Duration.ofMillis(restartDelay.getQuantity(TimeUnit.MILLISECONDS)));
    logger.info("All nodes came back up");
  }
}
