/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.service.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.terracottatech.dynamic_config.cli.common.InetSocketAddressConverter;
import com.terracottatech.dynamic_config.cli.common.Usage;
import com.terracottatech.dynamic_config.model.NodeContext;
import com.terracottatech.nomad.client.results.ConsistencyReceiver;
import com.terracottatech.tools.detailed.state.LogicalServerState;

import java.net.InetSocketAddress;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * @author Mathieu Carbou
 */
@Parameters(commandNames = "repair", commandDescription = "Repair a cluster configuration")
@Usage("repair -s <hostname[:port]>")
public class RepairCommand extends RemoteCommand {

  @Parameter(names = {"-s"}, description = "Node to connect to", required = true, converter = InetSocketAddressConverter.class)
  InetSocketAddress node;

  private Map<InetSocketAddress, LogicalServerState> onlineNodes;

  @Override
  public void validate() {
    requireNonNull(node);

    validateAddress(node);

    // this call can take some time and we can have some timeout
    onlineNodes = findOnlineRuntimePeers(node);

    if (!areAllNodesActivated(onlineNodes.keySet())) {
      throw new IllegalStateException("Cannot run a repair on a non activated  cluster");
    }
  }

  @Override
  public final void run() {
    ConsistencyReceiver<NodeContext> consistencyReceiver = getNomadConsistency(onlineNodes);

    if (consistencyReceiver.areAllAccepting()) {
      logger.info("Cluster configuration is healthy. No repair needed.");

    } else if (consistencyReceiver.hasDiscoveredOtherClient()) {
      logger.error("Unable to diagnose cluster configuration: another process has started a configuration change.");

    } else if (consistencyReceiver.getDiscoverFailure() != null) {
      logger.error("Unable to diagnose cluster configuration: " + consistencyReceiver.getDiscoverFailure());

    } else if (consistencyReceiver.hasDiscoveredInconsistentCluster()) {
      //TODO [DYNAMIC-CONFIG]: TDB-4787 - enhance repair command
      logger.error("Cluster configuration is broken and cannot be automatically repaired.");

    } else {
      logger.info("Attempting an automatic repair of the configuration...");
      runNomadRepair(onlineNodes);
      logger.info("Configuration is repaired.");
    }
  }
}