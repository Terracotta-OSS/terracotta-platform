/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.service.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.terracottatech.dynamic_config.cli.common.InetSocketAddressConverter;
import com.terracottatech.dynamic_config.cli.common.StateConverter;
import com.terracottatech.dynamic_config.cli.common.Usage;
import com.terracottatech.dynamic_config.model.NodeContext;
import com.terracottatech.nomad.client.results.ConsistencyAnalyzer;
import com.terracottatech.tools.detailed.state.LogicalServerState;

import java.net.InetSocketAddress;
import java.util.Map;

import static com.terracottatech.nomad.server.ChangeRequestState.COMMITTED;
import static com.terracottatech.nomad.server.ChangeRequestState.ROLLED_BACK;
import static java.util.Objects.requireNonNull;

/**
 * @author Mathieu Carbou
 */
@Parameters(commandNames = "repair", commandDescription = "Repair a cluster configuration")
@Usage("repair -s <hostname[:port]> [-f commit|rollback]")
public class RepairCommand extends RemoteCommand {

  public enum State {COMMIT, ROLLBACK}

  @Parameter(names = {"-s"}, description = "Node to connect to", required = true, converter = InetSocketAddressConverter.class)
  InetSocketAddress node;

  @Parameter(names = {"-f"}, description = "State to force for the last prepared configuration: commit or rollback", converter = StateConverter.class)
  State forcedState;

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
        //TODO [DYNAMIC-CONFIG]: TDB-4787 - enhance repair command to force a rollback to a checkpoint ?
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
          logger.info("All nodes appear online.");
        }

        logger.info("Attempting an automatic repair of the configuration...");

        if (forcedState == null) {
          logger.info("Auto-detecting what needs to be done...");
        } else {
          logger.warn("Forcing a " + forcedState.name().toLowerCase() + "...");
        }

        runNomadRepair(allNodes, forcedState == State.COMMIT ? COMMITTED : forcedState == State.ROLLBACK ? ROLLED_BACK : null);
        logger.info("Configuration is repaired.");

        break;
      }

      default:
        throw new AssertionError(consistencyAnalyzer.getGlobalState());
    }
  }
}