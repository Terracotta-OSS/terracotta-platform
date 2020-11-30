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

import com.fasterxml.jackson.core.JsonProcessingException;
import org.terracotta.diagnostic.model.LogicalServerState;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node.Endpoint;
import org.terracotta.dynamic_config.api.model.nomad.TopologyNomadChange;
import org.terracotta.dynamic_config.api.service.ClusterValidator;
import org.terracotta.dynamic_config.cli.api.converter.OperationType;
import org.terracotta.dynamic_config.cli.api.command.Injector.Inject;
import org.terracotta.json.ObjectMapperFactory;

import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static java.lang.System.lineSeparator;

/**
 * @author Mathieu Carbou
 */
public abstract class TopologyCommand extends RemoteCommand {
  protected OperationType operationType = OperationType.NODE;
  protected InetSocketAddress destinationAddress;
  protected boolean force;

  @Inject public ObjectMapperFactory objectMapperFactory;

  protected Endpoint destination;

  protected Map<Endpoint, LogicalServerState> destinationOnlineNodes;
  protected boolean destinationClusterActivated;
  protected Cluster destinationCluster;

  public void setOperationType(OperationType operationType) {
    this.operationType = operationType;
  }

  public void setDestinationAddress(InetSocketAddress destinationAddress) {
    this.destinationAddress = destinationAddress;
  }

  public void setForce(boolean force) {
    this.force = force;
  }

  public void validate() {
    destination = getEndpoint(destinationAddress);

    // prevent any topology change if a configuration change has been made through Nomad, requiring a restart, but nodes were not restarted yet
    validateLogOrFail(
        () -> !mustBeRestarted(destination),
        "Impossible to do any topology change. Cluster at address: " + destination + " is waiting to be restarted to apply some pending changes. " +
            "You can run the command with the force option to force the commit, but at the risk of breaking this cluster configuration consistency. " +
            "The newly added node will be restarted, but not the existing ones.");

    destinationCluster = getUpcomingCluster(destination);
    destinationOnlineNodes = findOnlineRuntimePeers(destination);
    destinationClusterActivated = areAllNodesActivated(destinationOnlineNodes.keySet());

    if (!destinationCluster.containsNode(destination.getNodeUID())) {
      throw new IllegalArgumentException("Wrong destination endpoint: " + destination + ". It does not match any node in destination cluster: " + destinationCluster.toShapeString());
    }

    checkForOperationSupport();

    if (destinationClusterActivated) {
      ensureNodesAreEitherActiveOrPassive(destinationOnlineNodes);
      ensureActivesAreAllOnline(destinationCluster, destinationOnlineNodes);
    }
  }

  protected void checkForOperationSupport() {
    if (destinationClusterActivated && OperationType.STRIPE.equals(getOperationType())) {
      throw new UnsupportedOperationException("Topology modifications of whole stripes on an activated cluster are not supported");
    }
  }

  @Override
  public void run() {
    validate();
    // build an updated topology
    Cluster result = updateTopology();

    // triggers validation
    new ClusterValidator(result).validate();

    if (logger.isDebugEnabled()) {
      try {
        logger.debug("Updated topology:{}{}.", lineSeparator(), objectMapperFactory.create().writerWithDefaultPrettyPrinter().writeValueAsString(result));
      } catch (JsonProcessingException e) {
        throw new UncheckedIOException(e);
      }
    }

    // push the updated topology to all the addresses
    // If a node has been removed, then it will make itself alone on its own cluster and will have no more links to the previous nodes
    // This is done in the DynamicConfigService#setUpcomingCluster() method

    if (destinationClusterActivated) {
      TopologyNomadChange nomadChange = buildNomadChange(result);
      licenseValidation(destination, nomadChange.getCluster());
      onNomadChangeReady(nomadChange);
      logger.info("Sending the topology change");
      try {
        runTopologyChange(destinationCluster, destinationOnlineNodes, nomadChange);
      } catch (RuntimeException e) {
        onNomadChangeFailure(nomadChange, e);
      }
      onNomadChangeSuccess(nomadChange);

    } else {
      logger.info("Sending the topology change");
      Set<Endpoint> allOnlineNodes = new HashSet<>(getAllOnlineSourceNodes());
      allOnlineNodes.addAll(destinationOnlineNodes.keySet());
      setUpcomingCluster(allOnlineNodes, result);
    }

    logger.info("Resulting cluster: " + result.toShapeString());
    logger.info("Command successful!" + lineSeparator());
  }

  /*<-- Test methods --> */
  OperationType getOperationType() {
    return operationType;
  }

  protected final void validateLogOrFail(Supplier<Boolean> expectedCondition, String error) {
    if (!expectedCondition.get()) {
      if (force) {
        logger.warn("Following validation has been bypassed with the force option:{} - {}", lineSeparator(), error);
      } else {
        throw new IllegalArgumentException(error);
      }
    }
  }

  protected void onNomadChangeReady(TopologyNomadChange nomadChange) {
  }

  protected void onNomadChangeSuccess(TopologyNomadChange nomadChange) {
  }

  protected void onNomadChangeFailure(TopologyNomadChange nomadChange, RuntimeException error) {
    throw error;
  }

  protected abstract Collection<Endpoint> getAllOnlineSourceNodes();

  protected abstract Cluster updateTopology();

  protected abstract TopologyNomadChange buildNomadChange(Cluster result);
}
