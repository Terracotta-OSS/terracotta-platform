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
import org.terracotta.diagnostic.model.LogicalServerState;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.nomad.NodeNomadChange;
import org.terracotta.dynamic_config.api.service.ClusterValidator;
import org.terracotta.dynamic_config.cli.config_tool.converter.OperationType;
import org.terracotta.dynamic_config.cli.converter.InetSocketAddressConverter;
import org.terracotta.inet.InetSocketAddressUtils;
import org.terracotta.json.Json;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;

import static java.lang.System.lineSeparator;
import static org.terracotta.dynamic_config.cli.config_tool.converter.OperationType.STRIPE;

/**
 * @author Mathieu Carbou
 */
public abstract class TopologyCommand extends RemoteCommand {
  @Parameter(names = {"-t"}, description = "Determine if the sources are nodes or stripes. Default: node", converter = OperationType.TypeConverter.class)
  protected OperationType operationType = OperationType.NODE;

  @Parameter(required = true, names = {"-d"}, description = "Destination stripe or cluster", converter = InetSocketAddressConverter.class)
  protected InetSocketAddress destination;

  @Parameter(required = true, names = {"-s"}, description = "Source node or stripe", converter = InetSocketAddressConverter.class)
  protected InetSocketAddress source;

  @Parameter(names = {"-f"}, description = "Force the operation")
  protected boolean force;

  protected Map<InetSocketAddress, LogicalServerState> destinationOnlineNodes;
  protected boolean destinationClusterActivated;
  protected Cluster destinationCluster;
  protected Cluster sourceCluster;

  @Override
  public void validate() {
    if (source == null) {
      throw new IllegalArgumentException("Missing source node");
    }
    if (destination == null) {
      throw new IllegalArgumentException("Missing destination node");
    }
    if (operationType == null) {
      throw new IllegalArgumentException("Missing type");
    }
    if (InetSocketAddressUtils.areEqual(source, destination)) {
      throw new IllegalArgumentException("The destination and the source endpoints must not be the same");
    }

    logger.debug("Validating the parameters");
    validateAddress(destination);
    validateAddress(source);

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

    sourceCluster = getUpcomingCluster(source);
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
    logger.info("Sending the topology change");

    if (destinationClusterActivated) {
      NodeNomadChange nomadChange = buildNomadChange(result);
      onNomadChangeReady(nomadChange);
      try {
        runPassiveChange(destinationCluster, destinationOnlineNodes, nomadChange);
      } catch (RuntimeException e) {
        onNomadChangeFailure(nomadChange, e);
      }
      onNomadChangeSuccess(nomadChange);

    } else {
      setUpcomingCluster(Collections.singletonList(source), result);
      setUpcomingCluster(destinationOnlineNodes.keySet(), result);
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

  InetSocketAddress getSource() {
    return source;
  }

  TopologyCommand setSource(InetSocketAddress source) {
    this.source = source;
    return this;
  }

  protected final void validateLogOrFail(Supplier<Boolean> expectedCondition, String error) {
    if (!expectedCondition.get()) {
      if (force) {
        logger.warn("Force option supplied, not failing on the following validation:");
        logger.warn(error);
      } else {
        throw new IllegalArgumentException(error);
      }
    }
  }

  protected void onNomadChangeReady(NodeNomadChange nomadChange) {
  }

  protected void onNomadChangeSuccess(NodeNomadChange nomadChange) {
  }

  protected void onNomadChangeFailure(NodeNomadChange nomadChange, RuntimeException error) {
    throw error;
  }

  protected abstract Cluster updateTopology();

  protected abstract NodeNomadChange buildNomadChange(Cluster result);
}
