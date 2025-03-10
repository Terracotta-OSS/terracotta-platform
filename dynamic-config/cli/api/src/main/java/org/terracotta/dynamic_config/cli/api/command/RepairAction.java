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
package org.terracotta.dynamic_config.cli.api.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.diagnostic.model.LogicalServerState;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node.Endpoint;
import org.terracotta.dynamic_config.api.service.ConfigurationConsistencyAnalyzer;
import org.terracotta.dynamic_config.api.service.ConfigurationConsistencyState;
import org.terracotta.dynamic_config.cli.api.converter.RepairMethod;
import org.terracotta.inet.HostPort;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.function.BiConsumer;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;
import static org.terracotta.nomad.server.ChangeRequestState.COMMITTED;
import static org.terracotta.nomad.server.ChangeRequestState.ROLLED_BACK;

/**
 * @author Mathieu Carbou
 */
public class RepairAction extends RemoteAction {

  private static final Logger LOGGER = LoggerFactory.getLogger(RepairAction.class);

  private HostPort node;
  private RepairMethod repairMethod = RepairMethod.AUTO;

  // cached values
  private Map<Endpoint, LogicalServerState> allNodes;
  private Map<Endpoint, LogicalServerState> onlineNodes;
  private Collection<Endpoint> offlineNodes;

  public void setNode(HostPort node) {
    this.node = node;
  }

  public void repairMethod(RepairMethod repairMethod) {
    this.repairMethod = requireNonNull(repairMethod);
  }

  @Override
  public final void run() {
    switch (repairMethod) {
      case RESET:
        resetAndStop(node);
        break;
      case UNLOCK:
        execute(this::forceUnlock);
        break;
      case ALLOW_SCALING:
        execute(this::allowScaling);
        break;
      default:
        execute(this::nomadRepair);
    }
    output.info("Command successful!");
  }

  private void execute(BiConsumer<Cluster, Map<Endpoint, LogicalServerState>> task) {
    allNodes = findRuntimePeersStatus(node);
    onlineNodes = filterOnlineNodes(allNodes);

    if (onlineNodes.size() != allNodes.size()) {
      offlineNodes = new ArrayList<>(allNodes.keySet());
      offlineNodes.removeAll(onlineNodes.keySet());
      LOGGER.warn("Some nodes are not reachable: {}", toString(offlineNodes));
    }
    Cluster cluster = getUpcomingCluster(node);

    task.accept(cluster, onlineNodes);
  }

  private void nomadRepair(Cluster cluster, Map<Endpoint, LogicalServerState> onlineNodes) {
    // the automatic repair command can only work on activated nodes
    Map<Endpoint, LogicalServerState> activatedNodes = filter(onlineNodes, (endpoint, state) -> isActivated(endpoint));

    if (activatedNodes.isEmpty()) {
      throw new IllegalStateException("No activated node found. Repair command only works with activated nodes.");
    }

    if (activatedNodes.size() != onlineNodes.size()) {
      Collection<Endpoint> unconfigured = new ArrayList<>(onlineNodes.keySet());
      unconfigured.removeAll(activatedNodes.keySet());
      LOGGER.warn("Some online nodes are not activated: {}. Automatic repair will only work against activated nodes: {}", toString(unconfigured), toString(activatedNodes.keySet()));
    }

    ConfigurationConsistencyAnalyzer configurationConsistencyAnalyzer = analyzeNomadConsistency(allNodes);
    ConfigurationConsistencyState state = configurationConsistencyAnalyzer.getState();
    String description = configurationConsistencyAnalyzer.getDescription();

    switch (state) {
      case ALL_ACCEPTING:
      case ONLINE_ACCEPTING:
        LOGGER.info(description);
        break;

      case DISCOVERY_FAILURE:
        LOGGER.error(description, configurationConsistencyAnalyzer.getDiscoverFailure().get());
        break;

      case INCONSISTENT:
      case PARTITIONED:
      case CHANGE_IN_PROGRESS:
      case UNKNOWN:
        LOGGER.error(description);
        break;

      case ALL_PREPARED:
        // run a repair that will do a commit except if the user has specified otherwise by using -force
        repair(allNodes, configurationConsistencyAnalyzer, RepairMethod.COMMIT);
        break;

      case ONLINE_PREPARED:
        // run a repair that will do what is asked by the user. -force is required in this case and
        // the command will fail if no hint is given
        repair(allNodes, configurationConsistencyAnalyzer, null);
        break;

      case PARTIALLY_PREPARED:
        if (repairMethod != RepairMethod.ROLLBACK) {
          throw new IllegalArgumentException("The configuration is partially prepared. A rollback is needed. A " + repairMethod + " cannot be executed.");
        }
        // run a repair that will do a rollback
        repair(allNodes, configurationConsistencyAnalyzer, RepairMethod.ROLLBACK);
        break;

      case PARTIALLY_COMMITTED:
        if (repairMethod != RepairMethod.COMMIT) {
          throw new IllegalArgumentException("The configuration is partially committed. A commit is needed. A " + repairMethod + " cannot be executed.");
        }
        // run a repair that will do a commit
        repair(allNodes, configurationConsistencyAnalyzer, RepairMethod.COMMIT);
        break;

      case PARTIALLY_ROLLED_BACK:
        if (repairMethod != RepairMethod.ROLLBACK) {
          throw new IllegalArgumentException("The configuration is partially rolled back. A rollback is needed. A " + repairMethod + " cannot be executed.");
        }
        // run a repair that will do a rollback
        repair(allNodes, configurationConsistencyAnalyzer, RepairMethod.ROLLBACK);
        break;

      default:
        throw new AssertionError(state);
    }
  }

  private void repair(Map<Endpoint, LogicalServerState> allNodes, ConfigurationConsistencyAnalyzer configurationConsistencyAnalyzer, RepairMethod detectedRepairMethod) {
    Collection<HostPort> onlineActivatedAddresses = configurationConsistencyAnalyzer.getOnlineNodesActivated().keySet();
    Map<Endpoint, LogicalServerState> onlineActivatedEndpoints = allNodes.entrySet().stream()
        .filter(e -> onlineActivatedAddresses.contains(e.getKey().getHostPort()))
        .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    RepairMethod wanted = repairMethod == RepairMethod.AUTO ? detectedRepairMethod : repairMethod;
    if (wanted == null) {
      throw new IllegalArgumentException("Some nodes are offline. Unable to determine what kind of repair to run. Please refer to the Troubleshooting Guide.");
    } else {
      output.info("Repairing configuration by running a " + wanted + "...");
    }
    runConfigurationRepair(onlineActivatedEndpoints, allNodes.size(), wanted == RepairMethod.COMMIT ? COMMITTED : wanted == RepairMethod.ROLLBACK ? ROLLED_BACK : null);
    output.info("Configuration is repaired.");
  }
}