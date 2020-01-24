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
import com.terracottatech.nomad.client.results.ConsistencyAnalyzer;
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
    ConsistencyAnalyzer<NodeContext> consistencyAnalyzer = analyzeNomadConsistency(allNodes);
    printDiagnostic(consistencyAnalyzer);
  }

  private void printDiagnostic(ConsistencyAnalyzer<NodeContext> consistencyAnalyzer) {
    Clock clock = Clock.systemDefaultZone();
    ZoneId zoneId = clock.getZone();
    DateTimeFormatter ISO_8601 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    StringBuilder sb = new StringBuilder();

    sb.append(lineSeparator());

    sb.append("Diagnostic result:")
        .append(lineSeparator())
        .append(lineSeparator());

    sb.append("[Cluster]")
        .append(lineSeparator());

    sb.append(" - Configuration state: ")
        .append(meaningOf(consistencyAnalyzer))
        .append(lineSeparator());
    sb.append(" - Configuration checkpoint found across all online nodes: ")
        .append(consistencyAnalyzer.getCheckpoint()
            .map(nci -> "YES (Version: " + nci.getVersion() + ", UUID: " + nci.getChangeUuid() + ", At: " + nci.getCreationTimestamp().atZone(zoneId).toLocalDateTime().format(ISO_8601) + ", Details: " + nci.getNomadChange().getSummary() + ")")
            .orElse("NO"))
        .append(lineSeparator());

    allNodes.keySet().forEach(nodeAddress -> {

      // header
      sb.append("[").append(nodeAddress).append("]").append(lineSeparator());

      // node status
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

        consistencyAnalyzer.getDiscoveryResponse(nodeAddress).ifPresent(discoverResponse -> {
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

  private static String meaningOf(ConsistencyAnalyzer<NodeContext> consistencyAnalyzer) {
    switch (consistencyAnalyzer.getGlobalState()) {
      case ACCEPTING:
        return "The cluster configuration is healthy. New configuration changes are possible.";

      case DISCOVERY_FAILURE:
        return "Failed to analyze cluster configuration. Reason: " + consistencyAnalyzer.getDiscoverFailure();

      case CONCURRENT_ACCESS:
        return "Failed to analyze cluster configuration. Reason: concurrent client access:"
            + " Host: " + consistencyAnalyzer.getOtherClientHost()
            + ", By: " + consistencyAnalyzer.getOtherClientUser()
            + ", On: " + consistencyAnalyzer.getNodeProcessingOtherClient();

      case INCONSISTENT:
        return "Cluster configuration is inconsistent: Change " + consistencyAnalyzer.getInconsistentChangeUuid()
            + " is committed on " + toString(consistencyAnalyzer.getCommittedNodes())
            + " and rolled back on " + toString(consistencyAnalyzer.getRolledBackNodes());

      case PREPARED:
        return "A new  cluster configuration has been prepared but not yet committed or rolled back on all nodes."
            + " No further configuration change can be done until the 'repair' command is run to finalize the configuration change.";

      case MAYBE_PREPARED:
        return "A new  cluster configuration has been prepared but not yet committed or rolled back on online nodes."
            + " Some nodes are unreachable so we do not know if the last configuration change has been committed or rolled back on them."
            + " No further configuration change can be done until the 'repair' command is run to finalize the configuration change."
            + " If the unreachable nodes do not become available again, you might need to use the '-f' option to force a commit or rollback ";

      case PARTIALLY_PREPARED:
        return "A new  cluster configuration has been *partially* prepared (some nodes didn't get the new change)."
            + " No further configuration change can be done until the 'repair' command is run to rollback the prepared nodes.";

      case PARTIALLY_COMMITTED:
        return "A new  cluster configuration has been *partially* committed (some nodes didn't commit)."
            + " No further configuration change can be done until the 'repair' command is run to commit all nodes.";

      case MAYBE_PARTIALLY_COMMITTED:
        return "A new  cluster configuration has been *partially* committed (some nodes didn't commit)."
            + " Some nodes are unreachable so we do not know their last configuration state."
            + " No further configuration change can be done until the 'repair' command is run to commit all nodes.";

      case PARTIALLY_ROLLED_BACK:
        return "A new  cluster configuration has been *partially* rolled back (some nodes didn't rollback)."
            + " No further configuration change can be done until the 'repair' command is run to rollback all nodes.";

      case MAYBE_PARTIALLY_ROLLED_BACK:
        return "A new  cluster configuration has been *partially* rolled back (some nodes didn't rollback)."
            + " Some nodes are unreachable so we do not know their last configuration state."
            + " No further configuration change can be done until the 'repair' command is run to rollback all nodes.";

      case UNKNOWN:
        return "Unable to determine the global configuration state."
            + " There might be some configuration inconsistencies."
            + " Please look at each node details.";

      case MAYBE_UNKNOWN:
        return "Unable to determine the global configuration state."
            + " There might be some configuration inconsistencies."
            + " Some nodes are unreachable so we do not know their last configuration state."
            + " Please look at each node details.";

      default:
        throw new AssertionError(consistencyAnalyzer.getGlobalState());
    }
  }
}