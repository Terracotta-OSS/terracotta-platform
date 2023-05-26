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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.diagnostic.model.LogicalServerState;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.LockTag;
import org.terracotta.dynamic_config.api.model.Node.Endpoint;
import org.terracotta.dynamic_config.api.model.nomad.TopologyNomadChange;
import org.terracotta.dynamic_config.api.service.ClusterValidator;
import org.terracotta.dynamic_config.cli.api.converter.OperationType;
import org.terracotta.inet.HostPort;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.lang.System.lineSeparator;
import static org.terracotta.dynamic_config.api.model.ClusterState.ACTIVATED;
import static org.terracotta.dynamic_config.api.model.ClusterState.CONFIGURING;

/**
 * @author Mathieu Carbou
 */
public abstract class TopologyAction extends RemoteAction {
  private static final Logger LOGGER = LoggerFactory.getLogger(TopologyAction.class);

  @Injector.Inject
  public final UnlockConfigAction unlockAction = new UnlockConfigAction();

  protected OperationType operationType = OperationType.NODE;
  protected HostPort destinationHostPort;
  protected boolean force;

  protected Endpoint destination;

  protected Map<Endpoint, LogicalServerState> destinationOnlineNodes;
  protected boolean destinationClusterActivated;
  protected Cluster destinationCluster;

  protected boolean useLock; // if set, a lock will try to be created before the operation and unlocked after
  protected String createdLockToken;
  private boolean unlock;

  public void setLock(boolean lock) {
    this.useLock = lock;
  }

  public boolean isLockRequired() {
    return useLock;
  }

  public boolean isUnlockRequired() {
    return unlock;
  }

  public void setUnlock(boolean unlock) {
    this.unlock = unlock;
  }

  public void setOperationType(OperationType operationType) {
    this.operationType = operationType;
  }

  public void setDestinationHostPort(HostPort destinationHostPort) {
    this.destinationHostPort = destinationHostPort;
  }

  public void setForce(boolean force) {
    this.force = force;
  }

  protected void validate() {
    destination = getEndpoint(destinationHostPort);
    destinationCluster = getUpcomingCluster(destination);
    destinationOnlineNodes = findOnlineRuntimePeers(destination);
    destinationClusterActivated = areAllNodesActivated(destinationOnlineNodes.keySet());

    if (!destinationCluster.containsNode(destination.getNodeUID())) {
      throw new IllegalArgumentException("Wrong destination endpoint: " + destination + ". It does not match any node in destination cluster: " + destinationCluster.toShapeString());
    }

    if (destinationClusterActivated) {
      ensureNodesAreEitherActiveOrPassive(destinationOnlineNodes);
      ensureActivesAreAllOnline(destinationCluster, destinationOnlineNodes);
    }
  }

  protected final boolean isScaleInOrOut() {
    return destinationClusterActivated && operationType == OperationType.STRIPE;
  }

  @Override
  public final void run() {
    validate();
    final Runnable scaleTask = () -> tryCatch(this::execute, onExecuteError());
    final boolean mustLock = isLockRequired() && destinationClusterActivated && !isLockAwareNomadManager();
    if (mustLock) {
      // if we need to lock the operation, lock it first
      createdLockToken = lock(destinationCluster, destinationOnlineNodes, LockTag.OWNER_PLATFORM, buildLockTag());
      output.out("Config lock with token: " + createdLockToken);
      destinationCluster = getUpcomingCluster(destination);
    }
    // then we run the scale task. In case of error, we unlock, but only if we placed a lock
    tryCatch(scaleTask, mustLock ? e -> unlock(destinationCluster, destinationOnlineNodes) : e -> {});
  }

  protected abstract String buildLockTag();

  protected Consumer<RuntimeException> onExecuteError() {
    return e -> {};
  }

  protected void execute() {
    // build an updated topology
    Cluster result = updateTopology();

    // triggers validation
    new ClusterValidator(result).validate(destinationClusterActivated ? ACTIVATED : CONFIGURING);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Updated topology:{}{}.", lineSeparator(), toPrettyJson(result));
    }

    // push the updated topology to all the addresses
    // If a node has been removed, then it will make itself alone on its own cluster and will have no more links to the previous nodes
    // This is done in the DynamicConfigService#setUpcomingCluster() method

    if (destinationClusterActivated) {
      TopologyNomadChange nomadChange = buildNomadChange(result);
      licenseValidation(destination.getHostPort(), nomadChange.getCluster());
      onNomadChangeReady(nomadChange);
      output.info("Sending the topology change");
      try {
        runTopologyChange(destinationCluster, destinationOnlineNodes, nomadChange);
      } catch (RuntimeException e) {
        onNomadChangeFailure(nomadChange, e);
      }
      onNomadChangeSuccess(nomadChange);

    } else {
      output.info("Sending the topology change");
      Set<Endpoint> allOnlineNodes = new HashSet<>(getAllOnlineSourceNodes());
      allOnlineNodes.addAll(destinationOnlineNodes.keySet());
      setUpcomingCluster(allOnlineNodes, result);
    }

    output.info("Resulting cluster: " + result.toShapeString());
    output.info("Command successful!");
  }

  /*<-- Test methods --> */
  OperationType getOperationType() {
    return operationType;
  }

  protected final void validateLogOrFail(Supplier<Boolean> expectedCondition, String error) {
    if (!expectedCondition.get()) {
      if (force) {
        LOGGER.warn("Following validation has been bypassed with the force option:{} - {}", lineSeparator(), error);
      } else {
        throw new IllegalArgumentException(error);
      }
    }
  }

  protected void onNomadChangeReady(TopologyNomadChange nomadChange) {}

  protected void onNomadChangeSuccess(TopologyNomadChange nomadChange) {}

  protected final void unlock(TopologyNomadChange nomadChange) {
    Cluster newCluster = nomadChange.apply(destinationCluster);
    newCluster.getNodes().stream().findAny().ifPresent(remaining -> {
      unlockAction.nomadManager = nomadManager; // because the initial nomad manager might have been wrapped by a locked aware one
      unlockAction.setNode(remaining.determineEndpoint(destination).getHostPort());
      unlockAction.run();
    });
  }

  protected void onNomadChangeFailure(TopologyNomadChange nomadChange, RuntimeException error) {
    throw error;
  }

  protected abstract Collection<Endpoint> getAllOnlineSourceNodes();

  protected abstract Cluster updateTopology();

  protected abstract TopologyNomadChange buildNomadChange(Cluster result);
}
