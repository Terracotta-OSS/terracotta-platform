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
package org.terracotta.dynamic_config.cli.config_tool.nomad;

import org.terracotta.diagnostic.model.LogicalServerState;
import org.terracotta.dynamic_config.api.model.Node.Endpoint;
import org.terracotta.dynamic_config.api.model.UID;
import org.terracotta.nomad.client.results.DiscoverResultsReceiver;
import org.terracotta.nomad.messages.DiscoverResponse;
import org.terracotta.nomad.server.ChangeRequestState;
import org.terracotta.nomad.server.NomadChangeInfo;
import org.terracotta.nomad.server.NomadServerMode;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collector;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.terracotta.diagnostic.model.LogicalServerState.STARTING;
import static org.terracotta.diagnostic.model.LogicalServerState.UNKNOWN;
import static org.terracotta.dynamic_config.cli.config_tool.nomad.ConsistencyAnalyzer.GlobalState.ACCEPTING;
import static org.terracotta.dynamic_config.cli.config_tool.nomad.ConsistencyAnalyzer.GlobalState.CONCURRENT_ACCESS;
import static org.terracotta.dynamic_config.cli.config_tool.nomad.ConsistencyAnalyzer.GlobalState.DESYNCHRONIZED;
import static org.terracotta.dynamic_config.cli.config_tool.nomad.ConsistencyAnalyzer.GlobalState.DISCOVERY_FAILURE;
import static org.terracotta.dynamic_config.cli.config_tool.nomad.ConsistencyAnalyzer.GlobalState.INCONSISTENT;
import static org.terracotta.dynamic_config.cli.config_tool.nomad.ConsistencyAnalyzer.GlobalState.MAYBE_PARTIALLY_COMMITTED;
import static org.terracotta.dynamic_config.cli.config_tool.nomad.ConsistencyAnalyzer.GlobalState.MAYBE_PARTIALLY_ROLLED_BACK;
import static org.terracotta.dynamic_config.cli.config_tool.nomad.ConsistencyAnalyzer.GlobalState.MAYBE_UNKNOWN;
import static org.terracotta.dynamic_config.cli.config_tool.nomad.ConsistencyAnalyzer.GlobalState.PARTIALLY_COMMITTED;
import static org.terracotta.dynamic_config.cli.config_tool.nomad.ConsistencyAnalyzer.GlobalState.PARTIALLY_PREPARED;
import static org.terracotta.dynamic_config.cli.config_tool.nomad.ConsistencyAnalyzer.GlobalState.PARTIALLY_ROLLED_BACK;
import static org.terracotta.nomad.server.ChangeRequestState.COMMITTED;
import static org.terracotta.nomad.server.ChangeRequestState.PREPARED;
import static org.terracotta.nomad.server.ChangeRequestState.ROLLED_BACK;

/**
 * Can be used in conjunction with the Nomad discovery process to capture the output and analyze the state of the cluster
 *
 * @author Mathieu Carbou
 */
public class ConsistencyAnalyzer<T> implements DiscoverResultsReceiver<T> {

  public enum GlobalState {
    ACCEPTING,
    PREPARED,

    INCONSISTENT,
    DESYNCHRONIZED,
    CONCURRENT_ACCESS,
    DISCOVERY_FAILURE,

    PARTIALLY_PREPARED,
    PARTIALLY_COMMITTED,
    PARTIALLY_ROLLED_BACK,
    UNKNOWN,

    MAYBE_PARTIALLY_COMMITTED,
    MAYBE_PARTIALLY_ROLLED_BACK,
    MAYBE_PREPARED,
    MAYBE_UNKNOWN
  }

  private final Map<Endpoint, DiscoverResponse<T>> responses;
  private final Map<Endpoint, LogicalServerState> allNodes;

  private volatile Throwable discoverFailure;
  private volatile boolean discoveredInconsistentCluster;
  private volatile boolean discoveredOtherClient;

  private volatile UUID inconsistentChangeUuid;
  private volatile Collection<InetSocketAddress> committedNodes;
  private volatile Collection<InetSocketAddress> rolledBackNodes;

  private volatile boolean discoveredDesynchronizedCluster;
  private volatile Map<UUID, Collection<InetSocketAddress>> lastChangeUuids;

  private volatile InetSocketAddress nodeProcessingOtherClient;
  private volatile String otherClientHost;
  private volatile String otherClientUser;

  public ConsistencyAnalyzer(Map<Endpoint, LogicalServerState> allNodes) {
    this.allNodes = allNodes;
    this.responses = new LinkedHashMap<>(allNodes.size());
  }

  @Override
  public void discovered(InetSocketAddress nodeAddress, DiscoverResponse<T> discovery) {
    responses.put(findEndpoint(nodeAddress), discovery);
  }

  @Override
  public void discoverFail(InetSocketAddress server, Throwable reason) {
    discoverFailure = reason;
  }

  @Override
  public void discoverClusterInconsistent(UUID changeUuid, Collection<InetSocketAddress> committedNodes, Collection<InetSocketAddress> rolledBackNodes) {
    this.inconsistentChangeUuid = changeUuid;
    this.committedNodes = committedNodes;
    this.rolledBackNodes = rolledBackNodes;
    this.discoveredInconsistentCluster = true;
  }

  @Override
  public void discoverClusterDesynchronized(Map<UUID, Collection<InetSocketAddress>> lastChangeUuids) {
    this.discoveredDesynchronizedCluster = true;
    this.lastChangeUuids = lastChangeUuids;
  }

  @Override
  public void discoverOtherClient(InetSocketAddress nodeAddress, String lastMutationHost, String lastMutationUser) {
    this.nodeProcessingOtherClient = nodeAddress;
    this.otherClientHost = lastMutationHost;
    this.otherClientUser = lastMutationUser;
    this.discoveredOtherClient = true;
  }

  public int getNodeCount() {
    return allNodes.size();
  }

  public Map<Endpoint, LogicalServerState> getAllNodes() {
    return allNodes;
  }

  public LogicalServerState getState(UID nodeUID) {
    return allNodes.get(findEndpoint(nodeUID));
  }

  public Optional<DiscoverResponse<T>> getDiscoveryResponse(Endpoint endpoint) {
    return Optional.ofNullable(responses.get(endpoint));
  }

  public Throwable getDiscoverFailure() {
    return discoverFailure;
  }

  // discoverClusterInconsistent

  public UUID getInconsistentChangeUuid() {
    return inconsistentChangeUuid;
  }

  public Collection<InetSocketAddress> getCommittedNodes() {
    return committedNodes;
  }

  public Collection<InetSocketAddress> getRolledBackNodes() {
    return rolledBackNodes;
  }

  public Map<UUID, Collection<InetSocketAddress>> getLastChangeUuids() {
    return lastChangeUuids;
  }

  // discoverOtherClient

  public InetSocketAddress getNodeProcessingOtherClient() {
    return nodeProcessingOtherClient;
  }

  public String getOtherClientHost() {
    return otherClientHost;
  }

  public String getOtherClientUser() {
    return otherClientUser;
  }

  // others

  public boolean hasUnreachableNodes() {
    return getNodeCount() > responses.size();
  }

  public Optional<NomadChangeInfo> getCheckpoint() {
    int configuredNodeCount = getOnlineConfiguredNodes();
    return responses.values()
        .stream()
        .flatMap(response -> response.getCheckpoints().stream())
        .collect(groupingBy(NomadChangeInfo::getChangeUuid, toList())) // Map<UUID, List<NomadChangeInfo>>
        .entrySet().stream()
        .filter(e -> e.getValue().size() == configuredNodeCount) // only consider entries having the change UUID on all the nodes
        .max(Comparator.comparing(e -> e.getValue().get(0).getVersion())) // select the UUID having the maximum version ID
        .map(e -> e.getValue().get(0));
  }

  /**
   * The number of nodes having some Nomad configuration changes.
   * <p>
   * A node can have some Nomad configuration changes but be activated and running, or in diagnostic mode for repair (not activated)
   */
  public int getOnlineConfiguredNodes() {
    return Math.toIntExact(responses.entrySet().stream().filter(e -> e.getValue().getLatestChange() != null).count());
  }

  public Map<Endpoint, LogicalServerState> getOnlineNodes() {
    return responses.entrySet()
        .stream()
        .collect(responseEntryToMap());
  }

  public Map<Endpoint, LogicalServerState> getOnlineActivatedNodes() {
    // activated nodes are passive / actives that have some nomad changes
    return responses.entrySet()
        .stream()
        .filter(e -> allNodes.get(e.getKey()) != STARTING && e.getValue().getLatestChange() != null)
        .collect(responseEntryToMap());
  }

  public Map<Endpoint, LogicalServerState> getOnlineInRepairNodes() {
    // nodes in repair are started in diagnostic mode and have nomad changes
    return responses.entrySet()
        .stream()
        .filter(e -> allNodes.get(e.getKey()) == STARTING && e.getValue().getLatestChange() != null)
        .collect(responseEntryToMap());
  }

  public Map<Endpoint, LogicalServerState> getOnlineInConfigurationNodes() {
    // new nodes in configuration are started in diagnostic mode and have no nomad change yet
    return responses.entrySet()
        .stream()
        .filter(e -> allNodes.get(e.getKey()) == STARTING && e.getValue().getLatestChange() == null)
        .collect(responseEntryToMap());
  }

  public boolean isOnlineAndActivated(UID nodeUID) {
    Endpoint endpoint = findEndpoint(nodeUID);
    return responses.containsKey(endpoint) && responses.get(endpoint).getLatestChange() != null && allNodes.get(endpoint) != STARTING;
  }

  public boolean isOnlineAndInRepair(UID nodeUID) {
    Endpoint endpoint = findEndpoint(nodeUID);
    return responses.containsKey(endpoint) && responses.get(endpoint).getLatestChange() != null && allNodes.get(endpoint) == STARTING;
  }

  public boolean isOnlineAndInConfiguration(UID nodeUID) {
    Endpoint endpoint = findEndpoint(nodeUID);
    return responses.containsKey(endpoint) && responses.get(endpoint).getLatestChange() == null && allNodes.get(endpoint) == STARTING;
  }

  public GlobalState getGlobalState() {
    if (discoverFailure != null) {
      return DISCOVERY_FAILURE;
    }

    if (discoveredInconsistentCluster) {
      return INCONSISTENT;
    }

    if (discoveredDesynchronizedCluster) {
      return DESYNCHRONIZED;
    }

    if (discoveredOtherClient) {
      return CONCURRENT_ACCESS;
    }

    boolean areAllAccepting = responses.values().stream().map(DiscoverResponse::getMode).allMatch(Predicate.isEqual(NomadServerMode.ACCEPTING));
    if (areAllAccepting) {
      return ACCEPTING;
    }

    // we are only looking at configured nodes
    final int nodeCount = getOnlineConfiguredNodes();

    // number of different UUIDs seen for the latest changes
    final long uuids = responses.values()
        .stream()
        .filter(r -> r.getLatestChange() != null)
        .map(r -> r.getLatestChange().getChangeUuid())
        .distinct()
        .count();

    // number of occurrences for each state
    final Map<ChangeRequestState, Long> states = responses.values()
        .stream()
        .filter(r -> r.getLatestChange() != null)
        .map(r -> r.getLatestChange().getState())
        .collect(groupingBy(identity(), counting()));

    final long rolledBack = states.getOrDefault(ROLLED_BACK, 0L);
    final long prepared = states.getOrDefault(PREPARED, 0L);
    final long committed = states.getOrDefault(COMMITTED, 0L);

    if (uuids == 1 && rolledBack == 0 && committed == 0 && prepared > 0 && prepared >= nodeCount) {
      // all nodes are online and prepared for the same change
      return GlobalState.PREPARED;
    }

    if (uuids == 1 && rolledBack == 0 && committed == 0 && prepared > 0) {
      // all online nodes are prepared for the same change, but we do not know the state of the offline ones
      return GlobalState.MAYBE_PREPARED;
    }

    if (uuids > 1 && prepared > 0) {
      // partially prepared change
      return PARTIALLY_PREPARED;
    }

    if (uuids > 1) {
      // desynchronized config
      return DESYNCHRONIZED;
    }

    if (uuids == 1 && rolledBack == 0 && committed > 0 && prepared > 0 && (prepared + committed >= nodeCount)) {
      // all nodes are either prepared or committed for the same change
      return PARTIALLY_COMMITTED;
    }

    if (uuids == 1 && rolledBack == 0 && committed > 0 && prepared > 0) {
      // all ONLINE nodes are either prepared or committed for the same change.
      // But we have an offline node, and we do not know its state
      return MAYBE_PARTIALLY_COMMITTED;
    }

    if (uuids == 1 && rolledBack > 0 && committed == 0 && prepared > 0 && (prepared + rolledBack >= nodeCount)) {
      // all nodes are either prepared or rolled back for the same change
      return PARTIALLY_ROLLED_BACK;
    }

    if (uuids == 1 && rolledBack > 0 && committed == 0 && prepared > 0) {
      // all ONLINE nodes are either prepared or rolled back for the same change.
      // But we have an offline node, and we do not know its state
      return MAYBE_PARTIALLY_ROLLED_BACK;
    }

    return responses.size() >= nodeCount ?
        GlobalState.UNKNOWN : // all nodes are up, but we were not able to determine the state
        MAYBE_UNKNOWN; // some nodes are not reachable and we were not able to determine the state
  }

  private Collector<Map.Entry<Endpoint, DiscoverResponse<T>>, ?, LinkedHashMap<Endpoint, LogicalServerState>> responseEntryToMap() {
    return toMap(
        Map.Entry::getKey,
        e -> allNodes.getOrDefault(e.getKey(), UNKNOWN),
        (logicalServerState, logicalServerState2) -> {
          throw new UnsupportedOperationException();
        },
        LinkedHashMap::new);
  }

  private Endpoint findEndpoint(InetSocketAddress address) {
    return allNodes.keySet().stream().filter(e -> e.getAddress().equals(address)).findAny().get();
  }

  private Endpoint findEndpoint(UID nodeUID) {
    return allNodes.keySet().stream().filter(e -> e.getNodeUID().equals(nodeUID)).findAny().get();
  }
}
