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

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.terracotta.diagnostic.model.LogicalServerState;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.cli.command.Usage;
import org.terracotta.dynamic_config.cli.config_tool.nomad.ConsistencyAnalyzer;
import org.terracotta.dynamic_config.cli.converter.InetSocketAddressConverter;
import org.terracotta.nomad.messages.ChangeDetails;
import org.terracotta.nomad.server.NomadServerMode;

import java.net.InetSocketAddress;
import java.time.Clock;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeSet;

import static java.lang.System.lineSeparator;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;

/**
 * @author Mathieu Carbou
 */
@Parameters(commandNames = "diagnostic", commandDescription = "Diagnose a cluster configuration")
@Usage("diagnostic -s <hostname[:port]>")
public class DiagnosticCommand extends RemoteCommand {

  @Parameter(names = {"-s"}, description = "Node to connect to", required = true, converter = InetSocketAddressConverter.class)
  InetSocketAddress node;

  @Override
  public void validate() {
    requireNonNull(node);
    validateAddress(node);
  }

  @Override
  public final void run() {
    // this call can take some time and we can have some timeout
    Map<InetSocketAddress, LogicalServerState> allNodes = findRuntimePeersStatus(node);

    ConsistencyAnalyzer<NodeContext> consistencyAnalyzer = analyzeNomadConsistency(allNodes);
    Collection<InetSocketAddress> onlineNodes = sort(consistencyAnalyzer.getOnlineNodes().keySet());
    Collection<InetSocketAddress> onlineActivatedNodes = sort(consistencyAnalyzer.getOnlineActivatedNodes().keySet());
    Collection<InetSocketAddress> onlineInConfigurationNodes = sort(consistencyAnalyzer.getOnlineInConfigurationNodes().keySet());
    Collection<InetSocketAddress> onlineInRepairNodes = sort(consistencyAnalyzer.getOnlineInRepairNodes().keySet());
    Collection<InetSocketAddress> nodesPendingRestart = sort(allNodes.keySet().stream()
        .filter(onlineNodes::contains)
        .filter(this::mustBeRestarted)
        .collect(toSet()));

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
    sb.append(" - Nodes: ")
        .append(allNodes.size())
        .append(details(allNodes.keySet()))
        .append(lineSeparator());
    sb.append(" - Nodes online: ")
        .append(onlineNodes.size())
        .append(details(onlineNodes))
        .append(lineSeparator());
    sb.append(" - Nodes online, configured and activated: ")
        .append(onlineActivatedNodes.size())
        .append(details(onlineActivatedNodes))
        .append(lineSeparator());
    sb.append(" - Nodes online, configured and in repair: ")
        .append(onlineInRepairNodes.size())
        .append(details(onlineInRepairNodes))
        .append(lineSeparator());
    sb.append(" - Nodes online, new and being configured: ")
        .append(onlineInConfigurationNodes.size())
        .append(details(onlineInConfigurationNodes))
        .append(lineSeparator());
    sb.append(" - Nodes pending restart: ")
        .append(nodesPendingRestart.size())
        .append(details(nodesPendingRestart))
        .append(lineSeparator());
    sb.append(" - Configuration state: ")
        .append(meaningOf(consistencyAnalyzer))
        .append(lineSeparator());
    sb.append(" - Configuration checkpoint found across all online configured nodes (activated or in repair): ")
        .append(consistencyAnalyzer.getCheckpoint()
            .map(nci -> "YES (Version: " + nci.getVersion() + ", UUID: " + nci.getChangeUuid() + ", At: " + nci.getCreationTimestamp().atZone(zoneId).toLocalDateTime().format(ISO_8601) + ", Details: " + nci.getNomadChange().getSummary() + ")")
            .orElse("NO"))
        .append(lineSeparator());

    allNodes.keySet().forEach(nodeAddress -> {
      // header
      sb.append("[").append(nodeAddress).append("]").append(lineSeparator());

      // node status
      sb.append(" - Node state: ")
          .append(consistencyAnalyzer.getState(nodeAddress))
          .append(lineSeparator());
      sb.append(" - Node online, configured and activated: ")
          .append(consistencyAnalyzer.isOnlineAndActivated(nodeAddress) ?
              "YES" :
              "NO")
          .append(lineSeparator());
      sb.append(" - Node online, configured and in repair: ")
          .append(consistencyAnalyzer.isOnlineAndInRepair(nodeAddress) ?
              "YES" :
              "NO")
          .append(lineSeparator());
      sb.append(" - Node online, new and being configured: ")
          .append(consistencyAnalyzer.isOnlineAndInConfiguration(nodeAddress) ?
              "YES" :
              "NO")
          .append(lineSeparator());

      // if node is online, display more information
      if (onlineNodes.contains(nodeAddress)) {

        sb.append(" - Node restart required: ")
            .append(nodesPendingRestart.contains(nodeAddress) ?
                "YES" :
                "NO")
            .append(lineSeparator());
        sb.append(" - Node configuration change in progress: ").append(hasIncompleteChange(nodeAddress) ?
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

          ChangeDetails<NodeContext> latestChange = discoverResponse.getLatestChange();
          if (latestChange != null) {

            sb.append(" - Node last configuration change UUID: ")
                .append(latestChange.getChangeUuid())
                .append(lineSeparator());
            sb.append(" - Node last configuration state: ")
                .append(latestChange.getState())
                .append(lineSeparator());
            sb.append(" - Node last configuration created at: ")
                .append(latestChange.getCreationTimestamp().atZone(zoneId).toLocalDateTime().format(ISO_8601))
                .append(lineSeparator());
            sb.append(" - Node last configuration created from: ")
                .append(latestChange.getCreationHost())
                .append(lineSeparator());
            sb.append(" - Node last configuration created by: ")
                .append(latestChange.getCreationUser())
                .append(lineSeparator());
            sb.append(" - Node last configuration change details: ")
                .append(latestChange.getOperation().getSummary())
                .append(lineSeparator());

            sb.append(" - Node last mutation at: ")
                .append(discoverResponse.getLastMutationTimestamp().atZone(zoneId).toLocalDateTime().format(ISO_8601))
                .append(lineSeparator());
            sb.append(" - Node last mutation from: ")
                .append(discoverResponse.getLastMutationHost())
                .append(lineSeparator());
            sb.append(" - Node last mutation by: ")
                .append(discoverResponse.getLastMutationUser())
                .append(lineSeparator());
          }
        });
      }
    });
    logger.info(sb.toString());
  }

  private static String details(Collection<InetSocketAddress> addresses) {
    return addresses.isEmpty() ? "" : " (" + toString(addresses) + ")";
  }

  private static Collection<InetSocketAddress> sort(Collection<InetSocketAddress> addrs) {
    TreeSet<InetSocketAddress> sorted = new TreeSet<>(Comparator.comparing(InetSocketAddress::toString));
    sorted.addAll(addrs);
    return sorted;
  }

  private static String meaningOf(ConsistencyAnalyzer<NodeContext> consistencyAnalyzer) {
    switch (consistencyAnalyzer.getGlobalState()) {
      case ACCEPTING:
        return "The cluster configuration is healthy. New configuration changes are possible.";

      case DISCOVERY_FAILURE:
        return "Failed to analyze cluster configuration. Reason: " + consistencyAnalyzer.getDiscoverFailure().getMessage();

      case CONCURRENT_ACCESS:
        return "Failed to analyze cluster configuration. Reason: concurrent client access:"
            + " Host: " + consistencyAnalyzer.getOtherClientHost()
            + ", By: " + consistencyAnalyzer.getOtherClientUser()
            + ", On: " + consistencyAnalyzer.getNodeProcessingOtherClient();

      case INCONSISTENT:
        return "Cluster configuration is inconsistent: Change " + consistencyAnalyzer.getInconsistentChangeUuid()
            + " is committed on " + toString(consistencyAnalyzer.getCommittedNodes())
            + " and rolled back on " + toString(consistencyAnalyzer.getRolledBackNodes());

      case DESYNCHRONIZED:
        return "Cluster configuration is desynchronized: Different last changes UUIDs found: " + consistencyAnalyzer.getLastChangeUuids().keySet();

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