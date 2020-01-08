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
@Parameters(commandNames = "diagnostic", commandDescription = "Diagnose and repair a cluster configuration")
@Usage("diagnostic -s <hostname[:port]>")
public class DiagnosticCommand extends RemoteCommand {

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
      throw new IllegalStateException("Cannot run a diagnostic on a non activated  cluster");
    }
  }

  @Override
  public final void run() {
    ConsistencyReceiver<NodeContext> consistencyReceiver = getNomadConsistency(onlineNodes);
    printCheck(consistencyReceiver);

    if (consistencyReceiver.areAllAccepting()) {
      logger.info("Cluster configuration is healthy.");

    } else if (consistencyReceiver.hasDiscoveredInconsistentCluster()) {
      logger.error("Cluster configuration is broken and cannot be automatically repaired.");

    } else if (consistencyReceiver.hasDiscoveredOtherClient()) {
      logger.error("Unable to diagnose cluster configuration: another process has started a configuration change.");

    } else if (consistencyReceiver.getDiscoverFailure() != null) {
      logger.error("Unable to diagnose cluster configuration: " + consistencyReceiver.getDiscoverFailure());

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

    sb.append("Diagnostic result:")
        .append(lineSeparator());

    sb.append("[Cluster]")
        .append(lineSeparator());
    sb.append(" - Configuration discovery: ")
        .append(consistencyReceiver.getDiscoverFailure() == null ?
            "SUCCESS" :
            ("FAILED (Reason: " + consistencyReceiver.getDiscoverFailure() + ")"))
        .append(lineSeparator());
    sb.append(" - Configuration consistency check: ")
        .append(!consistencyReceiver.hasDiscoveredInconsistentCluster() ?
            "SUCCESS" :
            ("FAILED (Change " + consistencyReceiver.getInconsistentChangeUuid() + " is committed on " + toString(consistencyReceiver.getCommittedServers()) + " and rolled back on " + toString(consistencyReceiver.getRolledBackServers()) + ")"))
        .append(lineSeparator());
    sb.append(" - Configuration change in progress from other client: ")
        .append(!consistencyReceiver.hasDiscoveredOtherClient() ?
            "NO" :
            ("YES (Host: " + consistencyReceiver.getOtherClientHost() + ", By: " + consistencyReceiver.getOtherClientUser() + ", On: " + consistencyReceiver.getServerProcessingOtherClient() + ")"))
        .append(lineSeparator());

    getUpcomingCluster(node).getNodes().forEach(n -> {
      InetSocketAddress nodeAddress = n.getNodeAddress();
      DiscoverResponse<NodeContext> discoverResponse = consistencyReceiver.getResponses().get(nodeAddress);

      sb.append("[").append(nodeAddress).append("]").append(lineSeparator());
      sb.append(" - Node state: ")
          .append(onlineNodes.getOrDefault(nodeAddress, UNREACHABLE))
          .append(lineSeparator());
      sb.append(" - Node restart required: ")
          .append(mustBeRestarted(node) ?
              "YES" :
              "NO")
          .append(lineSeparator());
      sb.append(" - Node configuration change in progress: ").append(hasIncompleteChange(node) ?
          "YES" :
          "NO")
          .append(lineSeparator());

      if (discoverResponse != null) {
        sb.append(" - Node can accept new changes: ")
            .append(discoverResponse.getMode() == NomadServerMode.ACCEPTING ? "YES" : "NO")
            .append(lineSeparator());
        sb.append(" - Node current configuration change version: ")
            .append(discoverResponse.getCurrentVersion())
            .append(lineSeparator());
        sb.append(" - Node highest configuration change version: ")
            .append(discoverResponse.getHighestVersion())
            .append(lineSeparator());
        sb.append(" - Node last configuration change UUID: ")
            .append(discoverResponse.getLatestChange().getChangeUuid())
            .append(lineSeparator());
        sb.append(" - Node last configuration state: ")
            .append(discoverResponse.getLatestChange().getState())
            .append(lineSeparator());
        sb.append(" - Node last configuration changed from: ")
            .append(discoverResponse.getLatestChange().getCreationHost())
            .append(lineSeparator());
        sb.append(" - Node last configuration changed by: ")
            .append(discoverResponse.getLatestChange().getCreationUser())
            .append(lineSeparator());
        sb.append(" - Node last configuration change details: ")
            .append(discoverResponse.getLatestChange().getOperation().getSummary())
            .append(lineSeparator());
      }
    });
    logger.info(sb.toString());
  }
}