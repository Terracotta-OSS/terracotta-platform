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
import org.terracotta.dynamic_config.cli.config_tool.converter.RepairAction;
import org.terracotta.dynamic_config.cli.config_tool.nomad.ConsistencyAnalyzer;
import org.terracotta.dynamic_config.cli.converter.InetSocketAddressConverter;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import static java.lang.System.lineSeparator;
import static java.util.Objects.requireNonNull;
import static org.terracotta.nomad.server.ChangeRequestState.COMMITTED;
import static org.terracotta.nomad.server.ChangeRequestState.ROLLED_BACK;

/**
 * @author Mathieu Carbou
 */
@Parameters(commandNames = "repair", commandDescription = "Repair a cluster configuration")
@Usage("repair -s <hostname[:port]> [-f commit|rollback|reset]")
public class RepairCommand extends RemoteCommand {

  @Parameter(names = {"-s"}, description = "Node to connect to", required = true, converter = InetSocketAddressConverter.class)
  InetSocketAddress node;

  @Parameter(names = {"-f"}, description = "Repair action to force: commit, rollback, reset", converter = RepairAction.RepairActionConverter.class)
  RepairAction forcedRepairAction;

  @Override
  public void validate() {
    requireNonNull(node);
    validateAddress(node);
  }

  @Override
  public final void run() {
    if (forcedRepairAction == RepairAction.RESET) {
      resetAndRestart(node);
    } else {
      nomadRepair();
    }
    logger.info("Command successful!" + lineSeparator());
  }

  private void nomadRepair() {
    Map<InetSocketAddress, LogicalServerState> allNodes = findRuntimePeersStatus(node);
    Map<InetSocketAddress, LogicalServerState> onlineNodes = filterOnlineNodes(allNodes);

    if (onlineNodes.size() != allNodes.size()) {
      Collection<InetSocketAddress> offlines = new ArrayList<>(allNodes.keySet());
      offlines.removeAll(onlineNodes.keySet());
      logger.warn("Some nodes are not reachable: {}", toString(offlines));
    }

    // the automatic repair command can only work on activated nodes
    Map<InetSocketAddress, LogicalServerState> activatedNodes = filter(onlineNodes, (addr, state) -> isActivated(addr));

    if (activatedNodes.isEmpty()) {
      throw new IllegalStateException("No activated node found. Repair command only works with activated nodes.");
    }

    if (activatedNodes.size() != onlineNodes.size()) {
      Collection<InetSocketAddress> unconfigured = new ArrayList<>(onlineNodes.keySet());
      unconfigured.removeAll(activatedNodes.keySet());
      logger.warn("Some online nodes are not activated: {}. Automatic repair will only work against activated nodes: {}", toString(unconfigured), toString(activatedNodes.keySet()));
    }

    ConsistencyAnalyzer<NodeContext> consistencyAnalyzer = analyzeNomadConsistency(allNodes);

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

        runConfigurationRepair(consistencyAnalyzer, forcedRepairAction == RepairAction.COMMIT ? COMMITTED : forcedRepairAction == RepairAction.ROLLBACK ? ROLLED_BACK : null);
        logger.info("Configuration is repaired.");

        break;
      }

      default:
        throw new AssertionError(consistencyAnalyzer.getGlobalState());
    }
  }
}