/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.cli.config_tool.command;

import com.beust.jcommander.Parameter;
import org.terracotta.diagnostic.common.LogicalServerState;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.service.ClusterValidator;
import org.terracotta.dynamic_config.cli.config_tool.converter.OperationType;
import org.terracotta.dynamic_config.cli.converter.InetSocketAddressConverter;
import org.terracotta.json.Json;
import org.terracotta.nomad.client.change.NomadChange;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static java.lang.System.lineSeparator;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.terracotta.dynamic_config.cli.config_tool.converter.OperationType.STRIPE;

/**
 * @author Mathieu Carbou
 */
public abstract class TopologyCommand extends RemoteCommand {
  @Parameter(names = {"-t"}, description = "Determine if the sources are nodes or stripes. Default: node", converter = OperationType.TypeConverter.class)
  protected OperationType operationType = OperationType.NODE;

  @Parameter(required = true, names = {"-d"}, description = "Destination stripe or cluster", converter = InetSocketAddressConverter.class)
  protected InetSocketAddress destination;

  @Parameter(required = true, names = {"-s"}, description = "Source nodes or stripes", variableArity = true, converter = InetSocketAddressConverter.class)
  protected List<InetSocketAddress> sources = Collections.emptyList();

  @Parameter(names = {"-f"}, description = "Forces the command by disregarding some validation")
  protected boolean force;

  protected Map<InetSocketAddress, LogicalServerState> destinationOnlineNodes;
  protected boolean destinationClusterActivated;
  protected Cluster destinationCluster;
  protected Map<InetSocketAddress, Cluster> sourceClusters;

  @Override
  public void validate() {
    if (sources.isEmpty()) {
      throw new IllegalArgumentException("Missing source nodes.");
    }
    if (destination == null) {
      throw new IllegalArgumentException("Missing destination node.");
    }
    if (operationType == null) {
      throw new IllegalArgumentException("Missing type.");
    }
    if (sources.contains(destination)) {
      throw new IllegalArgumentException("The destination endpoint must not be listed in the source endpoints.");
    }

    logger.info("Validation the parameters...");

    validateAddress(destination);
    sources.forEach(this::validateAddress);

    // prevent any topology change if a configuration change has been made through Nomad, requiring a restart, but nodes were not restarted yet
    validateLogOrFail(
        () -> !mustBeRestarted(destination),
        "Impossible to do any topology change. Cluster at address: " + destination + " is waiting to be restarted to apply some pending changes. " +
            "You can run the command with -f option to force the comment but at the risk of breaking this cluster configuration consistency. " +
            "The newly added node will be restarted, but not the existing ones.");

    destinationCluster = getUpcomingCluster(destination);
    destinationOnlineNodes = findOnlineRuntimePeers(destination);
    destinationClusterActivated = areAllNodesActivated(destinationOnlineNodes.keySet());

    if (!destinationCluster.getStripe(destination).isPresent() || !destinationCluster.getNode(destination).isPresent()) {
      throw new IllegalArgumentException("Wrong destination address: " + destination + ". It does not match any node in destination cluster: " + destinationCluster);
    }

    if (destinationClusterActivated) {
      ensureNodesAreEitherActiveOrPassive(destinationOnlineNodes);
      ensureActivesAreAllOnline(destinationCluster, destinationOnlineNodes);
      if (operationType == STRIPE) {
        throw new UnsupportedOperationException("Topology modifications of whole stripes on an activated cluster is not yet supported");
      }
    }

    sourceClusters = sources.stream().collect(toMap(
        identity(),
        this::getUpcomingCluster,
        (o1, o2) -> {
          throw new UnsupportedOperationException();
        },
        LinkedHashMap::new));
  }

  @Override
  public final void run() {
    // build an updated topology
    Cluster result = updateTopology();

    // triggers validation
    new ClusterValidator(result).validate();

    if (logger.isDebugEnabled()) {
      logger.debug("Updated topology:{}{}.", lineSeparator(), Json.toPrettyJson(result));
    }

    // push the updated topology to all the addresses
    // If a node has been removed, then it will make itself alone on its own cluster and will have no more links to the previous nodes
    // This is done in the DynamicConfigService#setUpcomingCluster() method
    logger.info("Sending the topology change to all the nodes");

    if (destinationClusterActivated) {
      NomadChange nomadChange = buildNomadChange(result);
      runNomadChange(destinationOnlineNodes, nomadChange);
      onNomadChangeCommitted(result);

    } else {
      setUpcomingCluster(destinationOnlineNodes.keySet(), result);
      setUpcomingCluster(sources, result);
    }

    logger.info("Command successful!" + lineSeparator());
  }

  /*<-- Test methods --> */
  OperationType getOperationType() {
    return operationType;
  }

  TopologyCommand setOperationType(OperationType operationType) {
    this.operationType = operationType;
    return this;
  }

  InetSocketAddress getDestination() {
    return destination;
  }

  TopologyCommand setDestination(InetSocketAddress destination) {
    this.destination = destination;
    return this;
  }

  TopologyCommand setDestination(String host, int port) {
    return setDestination(InetSocketAddress.createUnresolved(host, port));
  }

  List<InetSocketAddress> getSources() {
    return sources;
  }

  TopologyCommand setSources(List<InetSocketAddress> sources) {
    this.sources = sources;
    return this;
  }

  TopologyCommand setSources(InetSocketAddress... sources) {
    setSources(Arrays.asList(sources));
    return this;
  }

  protected final void validateLogOrFail(Supplier<Boolean> validator, String error) {
    if (!validator.get()) {
      if (force) {
        logger.warn("Not failing (-f) on this validation:");
        logger.warn(error);
      } else {
        throw new IllegalStateException(error);
      }
    }
  }

  protected abstract Cluster updateTopology();

  protected abstract NomadChange buildNomadChange(Cluster result);

  protected void onNomadChangeCommitted(Cluster result) {}
}
