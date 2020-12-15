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
import org.terracotta.diagnostic.model.LogicalServerState;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.service.ConfigurationConsistencyAnalyzer;
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
import static java.util.stream.Collectors.toSet;

/**
 * @author Mathieu Carbou
 */
public class DiagnosticAction extends RemoteAction {

  private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticAction.class);

  private InetSocketAddress node;

  public void setNode(InetSocketAddress node) {
    this.node = node;
  }

  @Override
  public final void run() {
    // this call can take some time and we can have some timeout
    Map<Node.Endpoint, LogicalServerState> allNodes = findRuntimePeersStatus(node);

    ConfigurationConsistencyAnalyzer configurationConsistencyAnalyzer = analyzeNomadConsistency(allNodes);
    Collection<InetSocketAddress> onlineNodes = sort(configurationConsistencyAnalyzer.getOnlineNodes().keySet());
    Collection<InetSocketAddress> onlineActivatedNodes = sort(configurationConsistencyAnalyzer.getOnlineNodesActivated().keySet());
    Collection<InetSocketAddress> onlineInConfigurationNodes = sort(configurationConsistencyAnalyzer.getOnlineNodesInConfiguration().keySet());
    Collection<InetSocketAddress> onlineInRepairNodes = sort(configurationConsistencyAnalyzer.getOnlineNodesInRepair().keySet());
    Collection<InetSocketAddress> nodesPendingRestart = sort(allNodes.keySet().stream()
        .map(Node.Endpoint::getAddress)
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
        .append(configurationConsistencyAnalyzer.getDescription())
        .append(lineSeparator());

    allNodes.keySet().forEach(endpoint -> {
      // header
      sb.append("[").append(endpoint).append("]").append(lineSeparator());

      // node status
      sb.append(" - Node state: ")
          .append(configurationConsistencyAnalyzer.getState(endpoint.getAddress()))
          .append(lineSeparator());
      sb.append(" - Node online, configured and activated: ")
          .append(onlineActivatedNodes.contains(endpoint.getAddress()) ?
              "YES" :
              "NO")
          .append(lineSeparator());
      sb.append(" - Node online, configured and in repair: ")
          .append(onlineInRepairNodes.contains(endpoint.getAddress()) ?
              "YES" :
              "NO")
          .append(lineSeparator());
      sb.append(" - Node online, new and being configured: ")
          .append(onlineInConfigurationNodes.contains(endpoint.getAddress()) ?
              "YES" :
              "NO")
          .append(lineSeparator());

      // if node is online, display more information
      if (onlineNodes.contains(endpoint.getAddress())) {

        sb.append(" - Node restart required: ")
            .append(nodesPendingRestart.contains(endpoint.getAddress()) ?
                "YES" :
                "NO")
            .append(lineSeparator());
        sb.append(" - Node configuration change in progress: ").append(hasIncompleteChange(endpoint) ?
            "YES" :
            "NO")
            .append(lineSeparator());

        configurationConsistencyAnalyzer.getDiscoveryResponse(endpoint.getAddress()).ifPresent(discoverResponse -> {

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
    LOGGER.info(sb.toString());
  }

  private static String details(Collection<?> items) {
    return items.isEmpty() ? "" : " (" + toString(items) + ")";
  }

  private static Collection<InetSocketAddress> sort(Collection<InetSocketAddress> addrs) {
    TreeSet<InetSocketAddress> sorted = new TreeSet<>(Comparator.comparing(InetSocketAddress::toString));
    sorted.addAll(addrs);
    return sorted;
  }
}