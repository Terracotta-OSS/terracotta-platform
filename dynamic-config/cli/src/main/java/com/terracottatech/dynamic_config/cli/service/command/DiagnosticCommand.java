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
import com.terracottatech.nomad.server.NomadServerMode;
import com.terracottatech.tools.detailed.state.LogicalServerState;

import java.net.InetSocketAddress;
import java.time.Clock;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static com.terracottatech.tools.detailed.state.LogicalServerState.UNREACHABLE;
import static java.lang.System.lineSeparator;
import static java.util.Objects.requireNonNull;

/**
 * @author Mathieu Carbou
 */
@Parameters(commandNames = "diagnostic", commandDescription = "Diagnose a cluster configuration")
@Usage("diagnostic -s <hostname[:port]>")
public class DiagnosticCommand extends RemoteCommand {

  @Parameter(names = {"-s"}, description = "Node to connect to", required = true, converter = InetSocketAddressConverter.class)
  InetSocketAddress node;

  private Map<InetSocketAddress, LogicalServerState> allNodes;
  private Map<InetSocketAddress, LogicalServerState> onlineNodes;

  @Override
  public void validate() {
    requireNonNull(node);

    validateAddress(node);

    // this call can take some time and we can have some timeout
    allNodes = findRuntimePeersStatus(node);
    onlineNodes = filterOnlineNodes(allNodes);

    if (!areAllNodesActivated(onlineNodes.keySet())) {
      throw new IllegalStateException("Cannot run a diagnostic on a non activated  cluster");
    }
  }

  @Override
  public final void run() {
    ConsistencyReceiver<NodeContext> consistencyReceiver = getNomadConsistency(onlineNodes);
    printDiagnostic(consistencyReceiver);

    if (consistencyReceiver.areAllAccepting()) {
      logger.info("Cluster configuration is healthy.");

    } else if (consistencyReceiver.hasDiscoveredOtherClient()) {
      logger.error("Unable to diagnose cluster configuration: another process has started a configuration change.");

    } else if (consistencyReceiver.getDiscoverFailure() != null) {
      logger.error("Unable to diagnose cluster configuration: " + consistencyReceiver.getDiscoverFailure());

    } else if (consistencyReceiver.hasDiscoveredInconsistentCluster()) {
      logger.error("Cluster configuration is broken. Please run the 'repair' command.");

    } else {
      logger.error("Cluster configuration is not fully committed or rolled back. Please run the 'repair' command.");
    }
  }

  private void printDiagnostic(ConsistencyReceiver<NodeContext> consistencyReceiver) {
    StringBuilder sb = new StringBuilder();

    sb.append(lineSeparator());

    sb.append("Diagnostic result:")
        .append(lineSeparator())
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
    sb.append(" - Configuration change allowed: ")
        .append(consistencyReceiver.areAllAccepting() ?
            "YES" :
            ("NO (Reason: at least one server is not accepting new change)"))
        .append(lineSeparator());

    Clock clock = Clock.systemDefaultZone();
    ZoneId zoneId = clock.getZone();
    DateTimeFormatter ISO_8601 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    allNodes.keySet().forEach(nodeAddress -> {

      // header
      sb.append("[").append(nodeAddress).append("]").append(lineSeparator());

      // server status
      sb.append(" - Node state: ")
          .append(allNodes.getOrDefault(nodeAddress, UNREACHABLE))
          .append(lineSeparator());

      // if not is online, display more information
      if (onlineNodes.containsKey(nodeAddress)) {
        sb.append(" - Node restart required: ")
            .append(mustBeRestarted(node) ?
                "YES" :
                "NO")
            .append(lineSeparator());
        sb.append(" - Node configuration change in progress: ").append(hasIncompleteChange(node) ?
            "YES" :
            "NO")
            .append(lineSeparator());

        consistencyReceiver.getDiscoveryResponse(nodeAddress).ifPresent(discoverResponse -> {
          sb.append(" - Node can accept new changes: ")
              .append(discoverResponse.getMode() == NomadServerMode.ACCEPTING ? "YES" : "NO")
              .append(lineSeparator());
          sb.append(" - Node current configuration version: ")
              .append(discoverResponse.getCurrentVersion())
              .append(lineSeparator());
          sb.append(" - Node highest configuration version: ")
              .append(discoverResponse.getHighestVersion())
              .append(lineSeparator());
          sb.append(" - Node last configuration change UUID: ")
              .append(discoverResponse.getLatestChange().getChangeUuid())
              .append(lineSeparator());
          sb.append(" - Node last configuration state: ")
              .append(discoverResponse.getLatestChange().getState())
              .append(lineSeparator());
          sb.append(" - Node last configuration created at: ")
              .append(discoverResponse.getLatestChange().getCreationTimestamp().atZone(zoneId).toLocalDateTime().format(ISO_8601))
              .append(lineSeparator());
          sb.append(" - Node last configuration created from: ")
              .append(discoverResponse.getLatestChange().getCreationHost())
              .append(lineSeparator());
          sb.append(" - Node last configuration created by: ")
              .append(discoverResponse.getLatestChange().getCreationUser())
              .append(lineSeparator());
          sb.append(" - Node last configuration mutated at: ")
              .append(discoverResponse.getLastMutationTimestamp().atZone(zoneId).toLocalDateTime().format(ISO_8601))
              .append(lineSeparator());
          sb.append(" - Node last configuration mutated from: ")
              .append(discoverResponse.getLastMutationHost())
              .append(lineSeparator());
          sb.append(" - Node last configuration mutated by: ")
              .append(discoverResponse.getLastMutationUser())
              .append(lineSeparator());
          sb.append(" - Node last configuration change details: ")
              .append(discoverResponse.getLatestChange().getOperation().getSummary())
              .append(lineSeparator());
        });
      }
    });
    logger.info(sb.toString());
  }
}