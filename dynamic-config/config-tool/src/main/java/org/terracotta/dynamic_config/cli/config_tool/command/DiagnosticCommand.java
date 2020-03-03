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
import org.terracotta.diagnostic.common.LogicalServerState;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.cli.command.Usage;
import org.terracotta.dynamic_config.cli.converter.InetSocketAddressConverter;
import org.terracotta.inet.InetSocketAddressUtils;
import org.terracotta.nomad.client.results.ConsistencyAnalyzer;
import org.terracotta.nomad.server.NomadServerMode;

import java.net.InetSocketAddress;
import java.time.Clock;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.System.lineSeparator;
import static java.util.Objects.requireNonNull;
import static org.terracotta.diagnostic.common.LogicalServerState.STARTING;
import static org.terracotta.diagnostic.common.LogicalServerState.UNREACHABLE;

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
  private Collection<InetSocketAddress> onlineActivatedNodes;

  @Override
  public void validate() {
    requireNonNull(node);

    validateAddress(node);

    // this call can take some time and we can have some timeout
    allNodes = findRuntimePeersStatus(node);
    onlineNodes = filterOnlineNodes(allNodes);
    onlineActivatedNodes = onlineNodes.keySet().stream().filter(this::isActivated).collect(Collectors.toSet());
  }

  @Override
  public final void run() {
    ConsistencyAnalyzer<NodeContext> consistencyAnalyzer = analyzeNomadConsistency(allNodes);

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
    sb.append(" - Node count: ")
        .append(consistencyAnalyzer.getNodeCount())
        .append(lineSeparator());
    sb.append(" - Online node count: ")
        .append(onlineNodes.size())
        .append(lineSeparator());
    sb.append(" - Online and activated node count: ")
        .append(onlineActivatedNodes.size())
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
      LogicalServerState state = allNodes.getOrDefault(nodeAddress, UNREACHABLE);

      // header
      sb.append("[").append(nodeAddress).append("]").append(lineSeparator());

      // node status
      sb.append(" - Node state: ")
          .append(state)
          .append(lineSeparator());

      // if not is online, display more information
      if (onlineNodes.containsKey(nodeAddress)) {
        sb.append(" - Node activated: ")
            .append(isActivated(node) ?
                "YES" :
                "NO")
            .append(lineSeparator());
        sb.append(" - Node started in diagnostic mode for initial configuration or repair: ")
            .append(state == STARTING && !InetSocketAddressUtils.contains(onlineActivatedNodes, nodeAddress) ?
                "YES" :
                "NO")
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