/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
package org.terracotta.dynamic_config.api.service;

import org.terracotta.diagnostic.model.LogicalServerState;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.LockContext;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.OptionalConfig;
import org.terracotta.inet.HostPort;
import org.terracotta.nomad.client.recovery.RecoveryProcessDecider;
import org.terracotta.nomad.client.results.DiscoverResultsReceiver;
import org.terracotta.nomad.messages.ChangeDetails;
import org.terracotta.nomad.messages.DiscoverResponse;
import org.terracotta.nomad.server.NomadServerMode;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;
import static org.terracotta.diagnostic.model.LogicalServerState.DIAGNOSTIC;
import static org.terracotta.dynamic_config.api.service.ConfigurationConsistencyState.ALL_ACCEPTING;
import static org.terracotta.dynamic_config.api.service.ConfigurationConsistencyState.ALL_PREPARED;
import static org.terracotta.dynamic_config.api.service.ConfigurationConsistencyState.ALL_UNINITIALIZED;
import static org.terracotta.dynamic_config.api.service.ConfigurationConsistencyState.CHANGE_IN_PROGRESS;
import static org.terracotta.dynamic_config.api.service.ConfigurationConsistencyState.DISCOVERY_FAILURE;
import static org.terracotta.dynamic_config.api.service.ConfigurationConsistencyState.INCONSISTENT;
import static org.terracotta.dynamic_config.api.service.ConfigurationConsistencyState.ONLINE_ACCEPTING;
import static org.terracotta.dynamic_config.api.service.ConfigurationConsistencyState.ONLINE_PREPARED;
import static org.terracotta.dynamic_config.api.service.ConfigurationConsistencyState.ONLINE_UNINITIALIZED;
import static org.terracotta.dynamic_config.api.service.ConfigurationConsistencyState.PARTIALLY_COMMITTED;
import static org.terracotta.dynamic_config.api.service.ConfigurationConsistencyState.PARTIALLY_PREPARED;
import static org.terracotta.dynamic_config.api.service.ConfigurationConsistencyState.PARTIALLY_ROLLED_BACK;
import static org.terracotta.dynamic_config.api.service.ConfigurationConsistencyState.PARTITIONED;

/**
 * Can be used in conjunction with the Nomad discovery process to capture the output and analyze the state of the cluster
 *
 * @author Mathieu Carbou
 */
public class ConfigurationConsistencyAnalyzer implements DiscoverResultsReceiver<NodeContext> {

  private final Map<HostPort, DiscoverResponse<NodeContext>> responses;
  private final Map<HostPort, LogicalServerState> allNodes;
  private final RecoveryProcessDecider<NodeContext> recoveryProcessDecider;

  private volatile Throwable discoverFailure;
  private volatile boolean discoveredConfigInconsistent;
  private volatile boolean discoveredOtherClient;

  private volatile UUID inconsistentChangeUuid;
  private volatile Collection<HostPort> committedNodes;
  private volatile Collection<HostPort> rolledBackNodes;

  private volatile boolean discoveredConfigPartitioned;
  private volatile Collection<Collection<HostPort>> partitions;

  private volatile HostPort nodeProcessingOtherClient;
  private volatile String otherClientHost;
  private volatile String otherClientUser;

  public ConfigurationConsistencyAnalyzer(Map<HostPort, LogicalServerState> allNodes) {
    this.allNodes = allNodes;
    this.responses = new LinkedHashMap<>(allNodes.size());
    this.recoveryProcessDecider = new RecoveryProcessDecider<>(allNodes.size(), null);
  }

  @Override
  public void discovered(HostPort nodeAddress, DiscoverResponse<NodeContext> discovery) {
    recoveryProcessDecider.discovered(nodeAddress, discovery);
    responses.put(nodeAddress, discovery);
  }

  @Override
  public void discoverFail(HostPort server, Throwable reason) {
    recoveryProcessDecider.discoverFail(server, reason);
    discoverFailure = reason;
  }

  @Override
  public void discoverConfigInconsistent(UUID changeUuid, Collection<HostPort> committedServers, Collection<HostPort> rolledBackServers) {
    recoveryProcessDecider.discoverConfigInconsistent(changeUuid, committedServers, rolledBackServers);
    this.inconsistentChangeUuid = changeUuid;
    this.committedNodes = committedServers;
    this.rolledBackNodes = rolledBackServers;
    this.discoveredConfigInconsistent = true;
  }

  @Override
  public void discoverConfigPartitioned(Collection<Collection<HostPort>> partitions) {
    recoveryProcessDecider.discoverConfigPartitioned(partitions);
    this.discoveredConfigPartitioned = true;
    this.partitions = partitions;
  }

  @Override
  public void discoverOtherClient(HostPort nodeAddress, String lastMutationHost, String lastMutationUser) {
    recoveryProcessDecider.discoverOtherClient(nodeAddress, lastMutationHost, lastMutationUser);
    this.nodeProcessingOtherClient = nodeAddress;
    this.otherClientHost = lastMutationHost;
    this.otherClientUser = lastMutationUser;
    this.discoveredOtherClient = true;
  }

  public int getNodeCount() {
    return allNodes.size();
  }

  public Map<HostPort, LogicalServerState> getAllNodes() {
    return allNodes;
  }

  public LogicalServerState getState(HostPort endpoint) {
    return allNodes.get(endpoint);
  }

  public Optional<DiscoverResponse<NodeContext>> getDiscoveryResponse(HostPort endpoint) {
    return Optional.ofNullable(responses.get(endpoint));
  }

  public Optional<Throwable> getDiscoverFailure() {
    return Optional.ofNullable(discoverFailure);
  }

  // discoverClusterInconsistent

  public UUID getInconsistentChangeUuid() {
    return inconsistentChangeUuid;
  }

  public Collection<HostPort> getCommittedNodes() {
    return committedNodes;
  }

  public Collection<HostPort> getRolledBackNodes() {
    return rolledBackNodes;
  }

  public Collection<Collection<HostPort>> getPartitions() {
    return partitions;
  }

  // discoverOtherClient

  public HostPort getNodeProcessingOtherClient() {
    return nodeProcessingOtherClient;
  }

  public String getOtherClientHost() {
    return otherClientHost;
  }

  public String getOtherClientUser() {
    return otherClientUser;
  }

  // others

  public int getOnlineNodeCount() {
    return responses.size();
  }

  public Map<HostPort, LogicalServerState> getOnlineNodes() {
    return responses.entrySet()
        .stream()
        .collect(responseEntryToMap());
  }

  public Map<HostPort, LogicalServerState> getOnlineNodesActivated() {
    // activated nodes are passive / actives that have some nomad changes
    return responses.entrySet()
        .stream()
        .filter(e -> allNodes.get(e.getKey()) != DIAGNOSTIC && e.getValue().getLatestChange() != null)
        .collect(responseEntryToMap());
  }

  public Map<HostPort, LogicalServerState> getOnlineNodesInRepair() {
    // nodes in repair are started in diagnostic mode and have nomad changes
    return responses.entrySet()
        .stream()
        .filter(e -> allNodes.get(e.getKey()) == DIAGNOSTIC && e.getValue().getLatestChange() != null)
        .collect(responseEntryToMap());
  }

  public Map<HostPort, LogicalServerState> getOnlineNodesInConfiguration() {
    // new nodes in configuration are started in diagnostic mode and have no nomad change yet
    return responses.entrySet()
        .stream()
        .filter(e -> allNodes.get(e.getKey()) == DIAGNOSTIC && e.getValue().getLatestChange() == null)
        .collect(responseEntryToMap());
  }

  public ConfigurationConsistencyState getState() {
    if (discoverFailure != null) {
      return DISCOVERY_FAILURE;
    }

    if (discoveredConfigInconsistent) {
      return INCONSISTENT;
    }

    if (discoveredConfigPartitioned) {
      return PARTITIONED;
    }

    if (discoveredOtherClient) {
      return CHANGE_IN_PROGRESS;
    }

    final int onlineNodeCount = getOnlineNodeCount();
    final int totalNodeCount = getNodeCount();

    boolean areAllAccepting = responses.values().stream().map(DiscoverResponse::getMode).allMatch(Predicate.isEqual(NomadServerMode.ACCEPTING));
    if (areAllAccepting) {
      return onlineNodeCount >= totalNodeCount ? ALL_ACCEPTING : ONLINE_ACCEPTING;
    }

    boolean areAllPrepared = responses.values().stream().map(DiscoverResponse::getMode).allMatch(Predicate.isEqual(NomadServerMode.PREPARED));
    if (areAllPrepared) {
      return onlineNodeCount >= totalNodeCount ? ALL_PREPARED : ONLINE_PREPARED;
    }

    boolean areAllUninitialized = responses.values().stream().map(DiscoverResponse::getMode).allMatch(Predicate.isEqual(NomadServerMode.UNINITIALIZED));
    if (areAllUninitialized) {
      return onlineNodeCount >= totalNodeCount ? ALL_UNINITIALIZED : ONLINE_UNINITIALIZED;
    }

    // We have a mix of NomadServerMode == PREPARED / ACCEPTING
    // And we know our config is not inconsistent or partitioned
    // So we are in an ongoing unfinished change process

    // Change UUIDs and result Hashes have already been validated by the consistency checker
    // So if we end up here were a change is in progress it means:
    // - all servers had the same result hash (have the same configuration in force)
    // - all servers either ends with the same change UUID or ends with any rolled back entries,
    // but their last committed change had the same UUID or led to the same change result hash (same config)

    if (recoveryProcessDecider.partiallyPrepared()) {
      return PARTIALLY_PREPARED;
    }

    if (recoveryProcessDecider.partiallyRolledBack()) {
      return PARTIALLY_ROLLED_BACK;
    }

    if (recoveryProcessDecider.partiallyCommitted()) {
      return PARTIALLY_COMMITTED;
    }

    return ConfigurationConsistencyState.UNKNOWN;
  }

  public String getDescription() {
    ConfigurationConsistencyState state = getState();
    switch (state) {
      case ALL_ACCEPTING:
        return "The cluster configuration is healthy and all nodes are online. No repair needed. " + getLockingInfo();

      case ONLINE_ACCEPTING:
        return "The cluster configuration seems healthy (some nodes are unreachable). " + getLockingInfo();

      case DISCOVERY_FAILURE:
        return "Failed to analyze cluster configuration." + getDiscoverFailure()
            .map(Throwable::getMessage)
            .map(msg -> " Reason: " + msg)
            .orElse("");

      case CHANGE_IN_PROGRESS:
        return "Failed to analyze cluster configuration. Reason: a change is in progress:"
            + " Host: " + getOtherClientHost()
            + ", By: " + getOtherClientUser()
            + ", On: " + getNodeProcessingOtherClient();

      case INCONSISTENT:
        return "Cluster configuration is inconsistent: Change " + getInconsistentChangeUuid()
            + " is committed on " + toString(getCommittedNodes())
            + " and rolled back on " + toString(getRolledBackNodes());

      case PARTITIONED:
        return "Cluster configuration is partitioned and cannot be automatically repaired." +
            " Some nodes have a different configuration that others." +
            " Groups: | " + getPartitions().stream().map(ConfigurationConsistencyAnalyzer::toString).collect(Collectors.joining(" | ")) + " |";

      case ALL_PREPARED:
        return "A new cluster configuration has been prepared on all nodes but not yet committed."
            + " No further configuration change can be done until the 'repair' command is run to finalize the configuration change.";

      case ONLINE_PREPARED:
        return "A new cluster configuration has been prepared but not yet committed or rolled back on online nodes."
            + " Some nodes are unreachable so we do not know if the last configuration change has been committed or rolled back on them."
            + " No further configuration change can be done until the offline nodes are restarted and the 'repair' command is run again"
            + " to finalize the configuration change. Please refer to the Troubleshooting Guide if needed.";

      case ALL_UNINITIALIZED:
        return "All the nodes are being configured (or being repaired).";

      case ONLINE_UNINITIALIZED:
        return "All the online nodes are being configured (or being repaired).";

      case PARTIALLY_PREPARED:
        return "A new  cluster configuration has been *partially* prepared (some nodes didn't get the new change)."
            + " No further configuration change can be done until the 'repair' command is run to rollback the prepared nodes.";

      case PARTIALLY_COMMITTED:
        return "A new  cluster configuration has been *partially* committed (some nodes didn't commit)."
            + " No further configuration change can be done until the 'repair' command is run to commit all nodes.";

      case PARTIALLY_ROLLED_BACK:
        return "A new  cluster configuration has been *partially* rolled back (some nodes didn't rollback)."
            + " No further configuration change can be done until the 'repair' command is run to rollback all nodes.";

      case UNKNOWN:
        return "Unable to determine the global configuration state."
            + " There might be some configuration inconsistencies or some nodes being repaired."
            + " Please look at each node details."
            + " A manual intervention might be needed to reset some nodes.";

      default:
        throw new AssertionError(state);
    }
  }

  public Optional<LockContext> findLockContext() {
    return responses.values()
        .stream()
        .map(DiscoverResponse::getLatestChange)
        .filter(Objects::nonNull)
        .map(ChangeDetails::getResult)
        .findAny()
        .map(NodeContext::getCluster)
        .map(Cluster::getConfigurationLockContext)
        .flatMap(OptionalConfig::asOptional);
  }

  private String getLockingInfo() {
    return findLockContext()
        .map((c) -> format("No changes are possible as config is locked by '%s'.", c.ownerInfo()))
        .orElse("New configuration changes are possible.");
  }

  public Optional<Cluster> findCluster() {
    return responses.values()
        .stream()
        .map(DiscoverResponse::getLatestChange)
        .filter(Objects::nonNull)
        .map(ChangeDetails::getResult)
        .findAny()
        .map(NodeContext::getCluster);
  }

  private Collector<Map.Entry<HostPort, DiscoverResponse<NodeContext>>, ?, LinkedHashMap<HostPort, LogicalServerState>> responseEntryToMap() {
    return toMap(
        Map.Entry::getKey,
        e -> allNodes.getOrDefault(e.getKey(), LogicalServerState.UNKNOWN),
        (logicalServerState, logicalServerState2) -> {
          throw new UnsupportedOperationException();
        },
        LinkedHashMap::new);
  }

  protected static String toString(Collection<?> items) {
    return items.stream().map(Object::toString).sorted().collect(Collectors.joining(", "));
  }
}
