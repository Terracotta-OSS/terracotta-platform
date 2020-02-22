/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.cli.config_tool.nomad;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.diagnostic.common.LogicalServerState;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.nomad.ClusterActivationNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.MultiNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.NodeNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.SettingNomadChange;
import org.terracotta.nomad.client.change.ChangeResultReceiver;
import org.terracotta.nomad.client.change.NomadChange;
import org.terracotta.nomad.client.recovery.RecoveryResultReceiver;
import org.terracotta.nomad.client.results.DiscoverResultsReceiver;
import org.terracotta.nomad.client.results.LoggingResultReceiver;
import org.terracotta.nomad.client.results.MultiChangeResultReceiver;
import org.terracotta.nomad.client.results.MultiRecoveryResultReceiver;
import org.terracotta.nomad.client.status.MultiDiscoveryResultReceiver;
import org.terracotta.nomad.server.ChangeRequestState;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static org.terracotta.diagnostic.common.LogicalServerState.ACTIVE;
import static org.terracotta.diagnostic.common.LogicalServerState.ACTIVE_RECONNECTING;
import static org.terracotta.diagnostic.common.LogicalServerState.PASSIVE;
import static org.terracotta.diagnostic.common.LogicalServerState.STARTING;

public class NomadManager<T> {
  private static final Logger LOGGER = LoggerFactory.getLogger(NomadManager.class);
  private static final EnumSet<LogicalServerState> ALLOWED = EnumSet.of(
      ACTIVE,
      PASSIVE,
      ACTIVE_RECONNECTING,
      STARTING // this mode is when a server is forced to start in diagnostic mode for repair
  );

  private final NomadClientFactory<T> clientFactory;

  public NomadManager(NomadClientFactory<T> clientFactory) {
    this.clientFactory = clientFactory;
  }

  public void runConfigurationDiscovery(Map<InetSocketAddress, LogicalServerState> nodes, DiscoverResultsReceiver<T> results) {
    LOGGER.debug("Attempting to discover nodes: {}", nodes);
    checkServerStates(nodes);
    List<InetSocketAddress> orderedList = keepOnlineAndOrderPassivesFirst(nodes);
    try (CloseableNomadClient<T> client = clientFactory.createClient(orderedList)) {
      client.tryDiscovery(new MultiDiscoveryResultReceiver<>(asList(new LoggingResultReceiver<>(), results)));
    }
  }

  public void runClusterActivation(Collection<InetSocketAddress> nodes, Cluster cluster, ChangeResultReceiver<T> results) {
    LOGGER.debug("Attempting to activate cluster: {}", cluster);
    runChange(new ArrayList<>(nodes), new ClusterActivationNomadChange(cluster), results);
  }

  public void runConfigurationChange(Map<InetSocketAddress, LogicalServerState> nodes, MultiNomadChange<SettingNomadChange> changes, ChangeResultReceiver<T> results) {
    LOGGER.debug("Attempting to make co-ordinated configuration change: {} on nodes: {}", changes, nodes);
    checkServerStates(nodes);
    List<InetSocketAddress> orderedList = keepOnlineAndOrderPassivesFirst(nodes);
    runChange(orderedList, changes, results);
  }

  public void runConfigurationRepair(Map<InetSocketAddress, LogicalServerState> nodes, RecoveryResultReceiver<T> results, ChangeRequestState forcedState) {
    LOGGER.debug("Attempting to repair configuration on nodes: {}", nodes);
    checkServerStates(nodes);
    List<InetSocketAddress> orderedList = keepOnlineAndOrderPassivesFirst(nodes);
    try (CloseableNomadClient<T> client = clientFactory.createClient(orderedList)) {
      client.tryRecovery(new MultiRecoveryResultReceiver<>(asList(new LoggingResultReceiver<>(), results)), nodes.size(), forcedState);
    }
  }

  public void runPassiveChange(Map<InetSocketAddress, LogicalServerState> nodes, NodeNomadChange change, ChangeResultReceiver<T> results) {
    LOGGER.debug("Attempting to add or remove a node: {} on cluster {}", change, nodes);
    checkServerStates(nodes);
    List<InetSocketAddress> orderedList = keepOnlineAndOrderPassivesFirst(nodes);
    runChange(orderedList, change, results);
  }

  private void runChange(List<InetSocketAddress> expectedOnlineNodes, NomadChange change, ChangeResultReceiver<T> results) {
    try (CloseableNomadClient<T> client = clientFactory.createClient(expectedOnlineNodes)) {
      client.tryApplyChange(new MultiChangeResultReceiver<>(asList(new LoggingResultReceiver<>(), results)), change);
    }
  }

  /**
   * Put passive firsts and then actives last and filter out offline nodes
   */
  private static List<InetSocketAddress> keepOnlineAndOrderPassivesFirst(Map<InetSocketAddress, LogicalServerState> expectedOnlineNodes) {
    Predicate<Map.Entry<InetSocketAddress, LogicalServerState>> online = e -> !e.getValue().isUnknown() && !e.getValue().isUnreacheable();
    Predicate<Map.Entry<InetSocketAddress, LogicalServerState>> actives = e -> e.getValue().isActive();
    return Stream.concat(
        expectedOnlineNodes.entrySet().stream().filter(online.and(actives.negate())),
        expectedOnlineNodes.entrySet().stream().filter(online.and(actives))
    ).map(Map.Entry::getKey).collect(Collectors.toList());
  }

  private static void checkServerStates(Map<InetSocketAddress, LogicalServerState> expectedOnlineNodes) {
    // find any illegal state that should prevent any Nomad access
    for (Map.Entry<InetSocketAddress, LogicalServerState> entry : expectedOnlineNodes.entrySet()) {
      if (!ALLOWED.contains(entry.getValue())) {
        throw new IllegalStateException("Nomad system is currently not accessible. Node: " + entry.getKey() + " is in state: " + entry.getValue());
      }
    }
  }
}
