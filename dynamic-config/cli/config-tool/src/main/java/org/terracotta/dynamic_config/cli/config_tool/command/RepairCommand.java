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

import org.terracotta.diagnostic.model.LogicalServerState;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node.Endpoint;
import org.terracotta.dynamic_config.api.service.ConsistencyAnalyzer;
import org.terracotta.dynamic_config.cli.config_tool.converter.RepairAction;

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

    ConsistencyAnalyzer consistencyAnalyzer = analyzeNomadConsistency(allNodes);

    switch (consistencyAnalyzer.getGlobalState()) {
      case ACCEPTING:
        logger.info("Cluster configuration is healthy. No repair needed.");
        break;

      case INCONSISTENT:
        //TODO [DYNAMIC-CONFIG]: TDB-4822 - enhance repair command to force a rollback to a checkpoint ?
        logger.error("Cluster configuration is inconsistent and cannot be automatically repaired. Change " + consistencyAnalyzer.getInconsistentChangeUuid()
            + " is committed on " + toString(consistencyAnalyzer.getCommittedNodes())
            + " and rolled back on " + toString(consistencyAnalyzer.getRolledBackNodes()));
        break;

      case DESYNCHRONIZED:
        //TODO [DYNAMIC-CONFIG]: TDB-4822 - enhance repair command to force a rollback to a checkpoint ?
        logger.error("Cluster configuration is desynchronized and cannot be automatically repaired. Nodes are not ending with the same change UUIDs. Details: " + consistencyAnalyzer.getLastChangeUuids());
        break;

      // normal repair cases
      case PREPARED:
      case MAYBE_PREPARED:
      case PARTIALLY_PREPARED:
      case PARTIALLY_COMMITTED:
      case MAYBE_PARTIALLY_COMMITTED:
      case PARTIALLY_ROLLED_BACK:
      case MAYBE_PARTIALLY_ROLLED_BACK:
        // perhaps we won't run into these again when issuing the repair command
      case DISCOVERY_FAILURE:
      case CONCURRENT_ACCESS:
        // let's still try to run a Nomad repair for the cases below. It will likely fail though.
      case UNKNOWN:
      case MAYBE_UNKNOWN: {
        if (consistencyAnalyzer.hasUnreachableNodes()) {
          logger.warn("Some nodes are not reachable.");
        } else {
          logger.info("All nodes are online.");
        }

        logger.info("Attempting an automatic repair of the configuration on nodes: {}...", toString(activatedNodes.keySet()));

        if (forcedRepairAction == null) {
          logger.info("Auto-detecting what needs to be done...");
        } else {
          logger.warn("Forcing a " + forcedRepairAction.name().toLowerCase() + "...");
        }

        Collection<InetSocketAddress> onlineActivatedAddresses = consistencyAnalyzer.getOnlineActivatedNodes().keySet();
        Map<Endpoint, LogicalServerState> onlineActivatedEndpoints = allNodes.entrySet().stream()
            .filter(e -> onlineActivatedAddresses.contains(e.getKey().getAddress()))
            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
        runConfigurationRepair(onlineActivatedEndpoints, allNodes.size(), forcedRepairAction == RepairAction.COMMIT ? COMMITTED : forcedRepairAction == RepairAction.ROLLBACK ? ROLLED_BACK : null);
        logger.info("Configuration is repaired.");

        break;
      }

      default:
        throw new AssertionError(consistencyAnalyzer.getGlobalState());
    }
  }
}