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
import org.terracotta.dynamic_config.api.model.Node.Endpoint;
import org.terracotta.dynamic_config.api.service.ConfigurationConsistencyAnalyzer;
import org.terracotta.dynamic_config.api.service.ConfigurationConsistencyState;
import org.terracotta.dynamic_config.cli.api.converter.RepairAction;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.toMap;
import static org.terracotta.nomad.server.ChangeRequestState.COMMITTED;
import static org.terracotta.nomad.server.ChangeRequestState.ROLLED_BACK;

/**
 * @author Mathieu Carbou
 */
public class RepairCommand extends RemoteCommand {

  private InetSocketAddress node;
  private RepairAction forcedRepairAction;

  public void setNode(InetSocketAddress node) {
    this.node = node;
  }

  public void setForcedRepairAction(RepairAction forcedRepairAction) {
    this.forcedRepairAction = forcedRepairAction;
  }

  @Override
  public final void run() {
    if (forcedRepairAction == RepairAction.RESET) {
      resetAndStop(node);
    } else if (forcedRepairAction == RepairAction.UNLOCK) {
      forceUnlock();
    } else {
      nomadRepair();
    }
    logger.info("Command successful!" + lineSeparator());
  }

  private void forceUnlock() {
    Map<Endpoint, LogicalServerState> allNodes = findRuntimePeersStatus(node);
    Map<Endpoint, LogicalServerState> onlineNodes = filterOnlineNodes(allNodes);

    if (onlineNodes.size() != allNodes.size()) {
      Collection<Endpoint> offlines = new ArrayList<>(allNodes.keySet());
      offlines.removeAll(onlineNodes.keySet());
      logger.warn("Some nodes are not reachable: {}", toString(offlines));
    }
    Cluster cluster = getUpcomingCluster(node);

    forceUnlock(cluster, onlineNodes);
  }

  private void nomadRepair() {
    Map<Endpoint, LogicalServerState> allNodes = findRuntimePeersStatus(node);
    Map<Endpoint, LogicalServerState> onlineNodes = filterOnlineNodes(allNodes);

    if (onlineNodes.size() != allNodes.size()) {
      Collection<Endpoint> offlines = new ArrayList<>(allNodes.keySet());
      offlines.removeAll(onlineNodes.keySet());
      logger.warn("Some nodes are not reachable: {}", toString(offlines));
    }

    // the automatic repair command can only work on activated nodes
    Map<Endpoint, LogicalServerState> activatedNodes = filter(onlineNodes, (endpoint, state) -> isActivated(endpoint));

    if (activatedNodes.isEmpty()) {
      throw new IllegalStateException("No activated node found. Repair command only works with activated nodes.");
    }

    if (activatedNodes.size() != onlineNodes.size()) {
      Collection<Endpoint> unconfigured = new ArrayList<>(onlineNodes.keySet());
      unconfigured.removeAll(activatedNodes.keySet());
      logger.warn("Some online nodes are not activated: {}. Automatic repair will only work against activated nodes: {}", toString(unconfigured), toString(activatedNodes.keySet()));
    }

    ConfigurationConsistencyAnalyzer configurationConsistencyAnalyzer = analyzeNomadConsistency(allNodes);
    ConfigurationConsistencyState state = configurationConsistencyAnalyzer.getState();
    String description = configurationConsistencyAnalyzer.getDescription();

    switch (state) {
      case ALL_ACCEPTING:
      case ONLINE_ACCEPTING:
        logger.info(description);
        break;

      case DISCOVERY_FAILURE:
        logger.error(description, configurationConsistencyAnalyzer.getDiscoverFailure());
        break;

      case INCONSISTENT:
      case PARTITIONED:
      case CHANGE_IN_PROGRESS:
      case UNKNOWN:
        logger.error(description);
        break;

      case ALL_PREPARED:
        // run a repair that will do a commit except if the user has specified otherwise by using -force
        repair(allNodes, configurationConsistencyAnalyzer, RepairAction.COMMIT);
        break;

      case ONLINE_PREPARED:
        // run a repair that will do what is asked by the user. -force is required in this case and
        // the command will fail if no hint is given
        repair(allNodes, configurationConsistencyAnalyzer, null);
        break;

      case PARTIALLY_PREPARED:
        if (forcedRepairAction != RepairAction.ROLLBACK) {
          throw new IllegalArgumentException("The configuration is partially prepared. A rollback is needed. A " + forcedRepairAction + " cannot be executed.");
        }
        // run a repair that will do a rollback
        repair(allNodes, configurationConsistencyAnalyzer, RepairAction.ROLLBACK);
        break;

      case PARTIALLY_COMMITTED:
        if (forcedRepairAction != RepairAction.COMMIT) {
          throw new IllegalArgumentException("The configuration is partially committed. A commit is needed. A " + forcedRepairAction + " cannot be executed.");
        }
        // run a repair that will do a commit
        repair(allNodes, configurationConsistencyAnalyzer, RepairAction.COMMIT);
        break;

      case PARTIALLY_ROLLED_BACK:
        if (forcedRepairAction != RepairAction.ROLLBACK) {
          throw new IllegalArgumentException("The configuration is partially rolled back. A rollback is needed. A " + forcedRepairAction + " cannot be executed.");
        }
        // run a repair that will do a rollback
        repair(allNodes, configurationConsistencyAnalyzer, RepairAction.ROLLBACK);
        break;

      default:
        throw new AssertionError(state);
    }
  }

  private void repair(Map<Endpoint, LogicalServerState> allNodes, ConfigurationConsistencyAnalyzer configurationConsistencyAnalyzer, RepairAction fallbackRepairAction) {
    Collection<InetSocketAddress> onlineActivatedAddresses = configurationConsistencyAnalyzer.getOnlineNodesActivated().keySet();
    Map<Endpoint, LogicalServerState> onlineActivatedEndpoints = allNodes.entrySet().stream()
        .filter(e -> onlineActivatedAddresses.contains(e.getKey().getAddress()))
        .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    RepairAction wanted = forcedRepairAction == null ? fallbackRepairAction : forcedRepairAction;
    if (wanted == null) {
      throw new IllegalArgumentException("Please use the '-force' option to specify whether a commit or rollback is wanted.");
    } else {
      logger.info("Repairing configuration by running a " + wanted + "...");
    }
    runConfigurationRepair(onlineActivatedEndpoints, allNodes.size(), wanted == RepairAction.COMMIT ? COMMITTED : wanted == RepairAction.ROLLBACK ? ROLLED_BACK : null);
    logger.info("Configuration is repaired.");
  }
}