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
import com.terracottatech.nomad.messages.DiscoverResponse;
import com.terracottatech.nomad.server.NomadServerMode;
import com.terracottatech.tools.detailed.state.LogicalServerState;

import java.net.InetSocketAddress;
import java.util.Map;

import static com.terracottatech.tools.detailed.state.LogicalServerState.UNREACHABLE;
import static java.lang.System.lineSeparator;
import static java.util.Objects.requireNonNull;

/**
 * @author Mathieu Carbou
 */
@Parameters(commandNames = "check", commandDescription = "Check and repair a cluster configuration")
@Usage("check -s <hostname[:port]>")
public class CheckCommand extends RemoteCommand {

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
      throw new IllegalStateException("Cannot repair: cluster is not activated");
    }
  }

  @Override
  public final void run() {
    ConsistencyReceiver<NodeContext> consistencyReceiver = getNomadConsistency(onlineNodes);
    printCheck(consistencyReceiver);

    if (consistencyReceiver.areAllAccepting()) {
      logger.info("Cluster configuration is healthy. No repair needed.");

    } else if (consistencyReceiver.hasDiscoveredInconsistentCluster()) {
      logger.error("Cluster configuration is broken and cannot be automatically repaired.");

    } else if (consistencyReceiver.hasDiscoveredOtherClient()) {
      logger.error("Unable to diagnose cluster configuration: another process is running.");

    } else if (consistencyReceiver.hasDiscoverFailed()) {
      logger.error("Unable to diagnose cluster configuration.");

    } else {
      logger.info("Attempting an automatic repair of the configuration...");
      runNomadRecovery(onlineNodes);
      logger.info("Configuration is repaired.");
      ConsistencyReceiver<NodeContext> newConsistencyReceiver = getNomadConsistency(onlineNodes);
      printCheck(newConsistencyReceiver);
    }

    logger.info("Command successful!" + lineSeparator());
  }

  private void printCheck(ConsistencyReceiver<NodeContext> consistencyReceiver) {
    StringBuilder sb = new StringBuilder();
    sb.append(lineSeparator());
    sb.append("Checking configuration:").append(lineSeparator());
    sb.append(" - Configuration discovery: ").append(consistencyReceiver.hasDiscoverFailed() ? "Failed" : "Success").append(lineSeparator());
    if (consistencyReceiver.hasDiscoveredOtherClient()) {
      sb.append("   Reason: Another process run on ")
          .append(consistencyReceiver.getOtherClientHost())
          .append(" by ")
          .append(consistencyReceiver.getOtherClientUser())
          .append(" changed the state on ")
          .append(consistencyReceiver.getServerProcessingOtherClient()).append(lineSeparator());
    }
    if (consistencyReceiver.hasDiscoveredInconsistentCluster()) {
      sb.append("   Reason: Inconsistent cluster for change: ")
          .append(consistencyReceiver.getInconsistentChangeUuid())
          .append(". Committed on: ")
          .append(consistencyReceiver.getCommittedServers())
          .append("; rolled back on: ")
          .append(consistencyReceiver.getRolledBackServers()).append(lineSeparator());
    }
    getUpcomingCluster(node).getNodes().forEach(n -> {
      InetSocketAddress nodeAddress = n.getNodeAddress();
      sb.append("[").append(nodeAddress).append("]").append(lineSeparator());
      sb.append(" - Status: ").append(onlineNodes.getOrDefault(nodeAddress, UNREACHABLE)).append(lineSeparator());
      sb.append(" - Node must be restarted to apply new configuration on disk: ").append(mustBeRestarted(node)).append(lineSeparator());
      sb.append(" - Configuration prepared but not yet committed or rolled back: ").append(hasPreparedConfigurationChange(node)).append(lineSeparator());
      DiscoverResponse<NodeContext> discoverResponse = consistencyReceiver.getResponses().get(nodeAddress);
      if (discoverResponse != null) {
        sb.append(" - Mode: ").append(discoverResponse.getMode() == NomadServerMode.ACCEPTING ? "Ready to accept new change" : "Change accepted but commit is pending").append(lineSeparator());
        sb.append(" - Current version: ").append(discoverResponse.getCurrentVersion()).append(lineSeparator());
        sb.append(" - Highest version: ").append(discoverResponse.getHighestVersion()).append(lineSeparator());
        sb.append(" - Last change state: ").append(discoverResponse.getLatestChange().getState()).append(lineSeparator());
        sb.append(" - Last change from: ").append(discoverResponse.getLatestChange().getCreationHost()).append(lineSeparator());
        sb.append(" - Last change by: ").append(discoverResponse.getLatestChange().getCreationUser()).append(lineSeparator());
        sb.append(" - Last change UUID: ").append(discoverResponse.getLatestChange().getChangeUuid()).append(lineSeparator());
        sb.append(" - Last change details: ").append(discoverResponse.getLatestChange().getOperation()).append(lineSeparator());
      }
    });
    logger.info(sb.toString());
  }
}