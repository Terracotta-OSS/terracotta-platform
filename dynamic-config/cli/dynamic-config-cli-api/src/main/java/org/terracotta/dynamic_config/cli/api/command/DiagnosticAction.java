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

import org.terracotta.diagnostic.model.LogicalServerState;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.LockContext;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.service.ConfigurationConsistencyAnalyzer;
import org.terracotta.dynamic_config.cli.api.nomad.DefaultNomadManager;
import org.terracotta.inet.HostPort;
import org.terracotta.nomad.messages.ChangeDetails;
import org.terracotta.nomad.server.NomadServerMode;

import java.time.Clock;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Stream;

import static java.lang.System.lineSeparator;
import static java.util.function.Predicate.isEqual;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.terracotta.diagnostic.model.LogicalServerState.ACTIVE;
import static org.terracotta.diagnostic.model.LogicalServerState.ACTIVE_RECONNECTING;
import static org.terracotta.diagnostic.model.LogicalServerState.ACTIVE_SUSPENDED;
import static org.terracotta.diagnostic.model.LogicalServerState.PASSIVE_SUSPENDED;
import static org.terracotta.diagnostic.model.LogicalServerState.START_SUSPENDED;
import static org.terracotta.dynamic_config.api.service.ConfigurationConsistencyState.ALL_ACCEPTING;
import static org.terracotta.dynamic_config.api.service.ConfigurationConsistencyState.ALL_PREPARED;
import static org.terracotta.dynamic_config.api.service.ConfigurationConsistencyState.CHANGE_IN_PROGRESS;
import static org.terracotta.dynamic_config.api.service.ConfigurationConsistencyState.DISCOVERY_FAILURE;
import static org.terracotta.dynamic_config.api.service.ConfigurationConsistencyState.INCONSISTENT;
import static org.terracotta.dynamic_config.api.service.ConfigurationConsistencyState.ONLINE_ACCEPTING;
import static org.terracotta.dynamic_config.api.service.ConfigurationConsistencyState.ONLINE_PREPARED;
import static org.terracotta.dynamic_config.api.service.ConfigurationConsistencyState.PARTIALLY_COMMITTED;
import static org.terracotta.dynamic_config.api.service.ConfigurationConsistencyState.PARTIALLY_PREPARED;
import static org.terracotta.dynamic_config.api.service.ConfigurationConsistencyState.PARTIALLY_ROLLED_BACK;
import static org.terracotta.dynamic_config.api.service.ConfigurationConsistencyState.PARTITIONED;

/**
 * @author Mathieu Carbou
 */
public class DiagnosticAction extends RemoteAction {

  private static final DateTimeFormatter ISO_8601 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
  private static final Clock CLOCK = Clock.systemDefaultZone();
  private static final ZoneId ZONE_ID = CLOCK.getZone();

  private List<HostPort> nodes = Collections.emptyList();
  private String outputFormat = "text";

  public void setNodes(List<HostPort> nodes) {
    this.nodes = nodes;
  }

  public void setOutputFormat(String outputFormat) {
    this.outputFormat = outputFormat;
  }

  @Override
  public final void run() {
    if (Stream.of("text", "json").noneMatch(isEqual(outputFormat))) {
      throw new IllegalArgumentException("Output format must be set to 'text' or 'json'");
    }

    // this call can take some time and we can have some timeout
    Map<Node.Endpoint, LogicalServerState> allNodes = findRuntimePeersStatus(nodes);

    ConfigurationConsistencyAnalyzer configurationConsistencyAnalyzer = analyzeNomadConsistency(allNodes);
    Collection<HostPort> onlineNodes = sort(configurationConsistencyAnalyzer.getOnlineNodes().keySet());
    Collection<HostPort> onlineActivatedNodes = sort(configurationConsistencyAnalyzer.getOnlineNodesActivated().keySet());
    Collection<HostPort> onlineInConfigurationNodes = sort(configurationConsistencyAnalyzer.getOnlineNodesInConfiguration().keySet());
    Collection<HostPort> onlineInRepairNodes = sort(configurationConsistencyAnalyzer.getOnlineNodesInRepair().keySet());
    Collection<HostPort> nodesPendingRestart = sort(allNodes.keySet().stream()
        .map(Node.Endpoint::getHostPort)
        .filter(onlineNodes::contains)
        .filter(this::mustBeRestarted)
        .collect(toSet()));

    if ("text".equals(outputFormat)) {
      output.out(toText(
          configurationConsistencyAnalyzer,
          allNodes,
          onlineNodes,
          onlineActivatedNodes,
          onlineInConfigurationNodes,
          onlineInRepairNodes,
          nodesPendingRestart));
    } else if ("json".equals(outputFormat)) {
      output.out(toJson(
          configurationConsistencyAnalyzer,
          allNodes,
          onlineNodes,
          onlineActivatedNodes,
          onlineInConfigurationNodes,
          onlineInRepairNodes,
          nodesPendingRestart));
    } else {
      throw new AssertionError(outputFormat);
    }
  }

  private String toText(ConfigurationConsistencyAnalyzer configurationConsistencyAnalyzer,
                        Map<Node.Endpoint, LogicalServerState> allNodes,
                        Collection<HostPort> onlineNodes,
                        Collection<HostPort> onlineActivatedNodes,
                        Collection<HostPort> onlineInConfigurationNodes,
                        Collection<HostPort> onlineInRepairNodes,
                        Collection<HostPort> nodesPendingRestart) {

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
          .append(configurationConsistencyAnalyzer.getState(endpoint.getHostPort()))
          .append(lineSeparator());
      sb.append(" - Node online, configured and activated: ")
          .append(onlineActivatedNodes.contains(endpoint.getHostPort()) ?
              "YES" :
              "NO")
          .append(lineSeparator());
      sb.append(" - Node online, configured and in repair: ")
          .append(onlineInRepairNodes.contains(endpoint.getHostPort()) ?
              "YES" :
              "NO")
          .append(lineSeparator());
      sb.append(" - Node online, new and being configured: ")
          .append(onlineInConfigurationNodes.contains(endpoint.getHostPort()) ?
              "YES" :
              "NO")
          .append(lineSeparator());

      // if node is online, display more information
      if (onlineNodes.contains(endpoint.getHostPort())) {

        sb.append(" - Node restart required: ")
            .append(nodesPendingRestart.contains(endpoint.getHostPort()) ?
                "YES" :
                "NO")
            .append(lineSeparator());
        sb.append(" - Node configuration change in progress: ").append(hasIncompleteChange(endpoint) ?
                "YES" :
                "NO")
            .append(lineSeparator());

        configurationConsistencyAnalyzer.getDiscoveryResponse(endpoint.getHostPort()).ifPresent(discoverResponse -> {

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
                .append(latestChange.getCreationTimestamp().atZone(ZONE_ID).toLocalDateTime().format(ISO_8601))
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
                .append(discoverResponse.getLastMutationTimestamp().atZone(ZONE_ID).toLocalDateTime().format(ISO_8601))
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
    return sb.toString();
  }

  private String toJson(ConfigurationConsistencyAnalyzer analyzer,
                        Map<Node.Endpoint, LogicalServerState> allNodes,
                        Collection<HostPort> onlineNodes,
                        Collection<HostPort> onlineActivatedNodes,
                        Collection<HostPort> onlineInConfigurationNodes,
                        Collection<HostPort> onlineInRepairNodes,
                        Collection<HostPort> nodesPendingRestart) {
    final Cluster cluster = analyzer.findCluster().orElseGet(() -> getUpcomingCluster(onlineNodes));
    final Optional<LockContext> lockContext = analyzer.findLockContext();

    Map<String, Object> map = new LinkedHashMap<>();

    // shape
    map.put("nodes", allNodes.size()); // number of nodes in the cluster
    map.put("stripes", cluster.getStripeCount()); // number of stripes in the cluster

    // online / offline / restart
    map.put("nodesOnline", onlineNodes.size()); // number of reachable nodes
    map.put("nodesUnreachable", Math.max(0, allNodes.size() - onlineNodes.size())); // number of offline nodes
    map.put("nodesRequiringRestart", nodesPendingRestart.size()); // number of nodes where a config change was made and which require a restart

    // config startup mode
    map.put("nodesInActiveMode", onlineActivatedNodes.size()); // number of nodes that have been started with an activated config
    map.put("nodesInRepairMode", onlineInRepairNodes.size()); // number of nodes started in repair mode
    map.put("nodesInConfigMode", onlineInConfigurationNodes.size()); // number of nodes started in configuration mode

    // config states
    map.put("configLocked", lockContext.isPresent()); // config system locked ?
    map.put("configLockedToken", lockContext.map(LockContext::getToken).orElse(null)); // token to use if we need to do a config change while config is locked
    map.put("configDiscoveryFailed", analyzer.getState() == DISCOVERY_FAILURE); // diagnostic call failed to read config state
    map.put("configDiscoveryFailure", analyzer.getDiscoverFailure().map(Throwable::getMessage).orElse(null)); // discovery error, can be empty
    map.put("configInconsistent", analyzer.getState() == INCONSISTENT); // the same change is both committed on some nodes and rolled back on others
    map.put("configPartitioned", analyzer.getState() == PARTITIONED); // some nodes in the cluster have a change history that has branched from a common point and have now different changes
    map.put("configChangeInProgress", analyzer.getState() == CHANGE_IN_PROGRESS); // discovery call failed because a change is in progress (prepare / commit)
    map.put("configChangeCommittedAll", analyzer.getState() == ALL_ACCEPTING); // all nodes online, config committed
    map.put("configChangeCommittedOnline", analyzer.getState() == ONLINE_ACCEPTING); // some nodes online, and all online nodes have config committed. We don't know about the offline ones.
    map.put("configChangePreparedAll", analyzer.getState() == ALL_PREPARED); // all nodes are online and have a prepared change that has not yet been committed
    map.put("configChangePreparedOnline", analyzer.getState() == ONLINE_PREPARED); // some nodes are online and have a prepared change that has not yet been committed
    map.put("configChangePartiallyPrepared", analyzer.getState() == PARTIALLY_PREPARED); // some nodes didn't prepare the last change
    map.put("configChangePartiallyCommitted", analyzer.getState() == PARTIALLY_COMMITTED); // some nodes didn't commit the last change
    map.put("configChangePartiallyRolledBack", analyzer.getState() == PARTIALLY_ROLLED_BACK); // some nodes didn't rolled back the last change

    // server states
    final Map<LogicalServerState, Long> states = allNodes.entrySet().stream().collect(groupingBy(Map.Entry::getValue, counting()));
    map.put("nodeStates", states);

    // some aggregated states (i.e. useful for tools like Kube operator)

    // manual intervention required
    final boolean manualInterventionRequired = !nodesPendingRestart.isEmpty() // restart required
        || (EnumSet.of(INCONSISTENT, PARTITIONED, ALL_PREPARED, ONLINE_PREPARED, PARTIALLY_PREPARED, PARTIALLY_COMMITTED, PARTIALLY_ROLLED_BACK).contains(analyzer.getState())) // a change is in progress or needs to be repaired
        || (!onlineActivatedNodes.isEmpty() && states.getOrDefault(ACTIVE, 0L) + states.getOrDefault(ACTIVE_RECONNECTING, 0L) != cluster.getStripeCount()) // missing active ?
        || !Collections.disjoint(EnumSet.of(ACTIVE_SUSPENDED, PASSIVE_SUSPENDED, START_SUSPENDED), states.keySet());


    // ready for a topology change ?
    final boolean readyForTopologyChange = !manualInterventionRequired
        && !lockContext.isPresent() // config not locked
        && (EnumSet.of(ALL_ACCEPTING, ONLINE_ACCEPTING).contains(analyzer.getState())) // nomad is accepting changes, with or without some offline nodes
        && nodesPendingRestart.isEmpty() // no pending restart
        && onlineActivatedNodes.size() == onlineNodes.size() // all online nodes are activated
        && (states.getOrDefault(ACTIVE, 0L) + states.getOrDefault(ACTIVE_RECONNECTING, 0L) == cluster.getStripeCount()) // 1 active per stripe
        && DefaultNomadManager.ALLOWED.containsAll(states.keySet());

    map.put("manualInterventionRequired", manualInterventionRequired);
    map.put("readyForTopologyChange", readyForTopologyChange);
    map.put("scalingDenied", !readyForTopologyChange);
    map.put("scalingVetoer", null);

    findScalingVetoer(onlineNodes).ifPresent(hostPort -> {
      map.put("scalingDenied", true);
      map.put("scalingVetoer", hostPort.toString());
    });

    Map<String, Object> topology = new LinkedHashMap<>();
    map.put("cluster", topology);
    topology.put("name", cluster.getName());
    topology.put("stripes", cluster.getStripes().stream().map(stripe -> {
      Map<String, Object> stripeMap = new LinkedHashMap<>();
      stripeMap.put("name", stripe.getName());
      stripeMap.put("nodes", stripe.getNodes().stream().map(node -> {
        Map<String, Object> nodeMap = new LinkedHashMap<>();
        nodeMap.put("name", node.getName());
        nodeMap.put("status", allNodes.entrySet()
            .stream()
            .filter(e -> e.getKey().getNodeUID().equals(node.getUID()))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElseGet(() -> getLogicalServerState(node.determineEndpoint(allNodes.keySet().iterator().next().getEndpointType()))));
        return nodeMap;
      }).collect(toList()));
      return stripeMap;
    }).collect(toList()));

    return toPrettyJson(map);
  }

  private static String details(Collection<?> items) {
    return items.isEmpty() ? "" : " (" + toString(items) + ")";
  }

  private static Collection<HostPort> sort(Collection<HostPort> addrs) {
    TreeSet<HostPort> sorted = new TreeSet<>(Comparator.comparing(HostPort::toString));
    sorted.addAll(addrs);
    return sorted;
  }
}