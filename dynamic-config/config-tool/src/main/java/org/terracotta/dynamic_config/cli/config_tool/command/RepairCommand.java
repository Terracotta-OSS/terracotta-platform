/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.cli.config_tool.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.terracotta.diagnostic.common.LogicalServerState;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.cli.command.Usage;
import org.terracotta.dynamic_config.cli.config_tool.converter.ChangeState;
import org.terracotta.dynamic_config.cli.converter.InetSocketAddressConverter;
import org.terracotta.nomad.client.results.ConsistencyAnalyzer;

import java.net.InetSocketAddress;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static org.terracotta.nomad.server.ChangeRequestState.COMMITTED;
import static org.terracotta.nomad.server.ChangeRequestState.ROLLED_BACK;

/**
 * @author Mathieu Carbou
 */
@Parameters(commandNames = "repair", commandDescription = "Repair a cluster configuration")
@Usage("repair -s <hostname[:port]> [-f commit|rollback]")
public class RepairCommand extends RemoteCommand {

  @Parameter(names = {"-s"}, description = "Node to connect to", required = true, converter = InetSocketAddressConverter.class)
  InetSocketAddress node;

  @Parameter(names = {"-f"}, description = "State to force for the last prepared configuration: commit or rollback", converter = ChangeState.StateConverter.class)
  ChangeState forcedChangeState;

  private Map<InetSocketAddress, LogicalServerState> allNodes;

  @Override
  public void validate() {
    requireNonNull(node);

    validateAddress(node);

    // this call can take some time and we can have some timeout
    allNodes = findRuntimePeersStatus(node);

    if (!areAllNodesActivated(filterOnlineNodes(allNodes).keySet())) {
      throw new IllegalStateException("Cannot run a repair on a non activated  cluster");
    }
  }

  @Override
  public final void run() {
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

        logger.info("Attempting an automatic repair of the configuration...");

        if (forcedChangeState == null) {
          logger.info("Auto-detecting what needs to be done...");
        } else {
          logger.warn("Forcing a " + forcedChangeState.name().toLowerCase() + "...");
        }

        runNomadRepair(allNodes, forcedChangeState == ChangeState.COMMIT ? COMMITTED : forcedChangeState == ChangeState.ROLLBACK ? ROLLED_BACK : null);
        logger.info("Configuration is repaired.");

        break;
      }

      default:
        throw new AssertionError(consistencyAnalyzer.getGlobalState());
    }
  }
}