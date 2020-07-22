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
package org.terracotta.dynamic_config.server.configuration.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.nomad.ClusterActivationNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.DynamicConfigNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.TopologyNomadChange;
import org.terracotta.nomad.messages.AcceptRejectResponse;
import org.terracotta.nomad.messages.DiscoverResponse;
import org.terracotta.nomad.server.NomadChangeInfo;
import org.terracotta.nomad.server.NomadException;
import org.terracotta.nomad.server.UpgradableNomadServer;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.terracotta.nomad.server.ChangeRequestState.COMMITTED;
import static org.terracotta.nomad.server.ChangeRequestState.PREPARED;

public class DynamicConfigNomadSynchronizer {

  private static final Logger LOGGER = LoggerFactory.getLogger(DynamicConfigNomadSynchronizer.class);

  private final NodeContext nodeStartupConfiguration;
  private final UpgradableNomadServer<NodeContext> nomadServer;

  public DynamicConfigNomadSynchronizer(NodeContext nodeStartupConfiguration, UpgradableNomadServer<NodeContext> nomadServer) {
    this.nodeStartupConfiguration = nodeStartupConfiguration;
    this.nomadServer = nomadServer;
  }

  public Set<Require> syncNomadChanges(List<NomadChangeInfo> sourceNomadChanges) throws NomadException {
    List<NomadChangeInfo> nomadChanges = nomadServer.getAllNomadChanges();

    // programming errors
    // to be able to start, there must be at least one committed activation change in the append log
    // so that we can boot on the tc-config file version 1.
    // DynamicConfigConfigurationProvider will catch such uncommitted changes and prevent startup (there won't be any available configuration)
    Check.assertNonEmpty(sourceNomadChanges, nomadChanges);
    Check.assertThat(() -> sourceNomadChanges.get(0).getNomadChange() instanceof ClusterActivationNomadChange);
    Check.assertThat(() -> nomadChanges.get(0).getNomadChange() instanceof ClusterActivationNomadChange);
    Check.assertThat(() -> sourceNomadChanges.get(0).getChangeRequestState() == COMMITTED);
    Check.assertThat(() -> nomadChanges.get(0).getChangeRequestState() == COMMITTED);

    if (nomadChanges.size() > sourceNomadChanges.size()) {
      throw new IllegalStateException("Node has more configuration changes than the source");
    }

    // record what to do after the sync (restart ? zap ?)
    Set<Require> requires = new HashSet<>(2);

    // is the node just been activated ?
    final boolean newNode = Check.isNodeNew(nomadChanges);

    // was the node activated at the same time as the source node or after ?
    final boolean jointActivation = Check.isJointActivation(nomadChanges, sourceNomadChanges);

    // index at which to start the normal sync
    int sourceInd;

    if (newNode && !jointActivation) {
      LOGGER.info("New node is joining an activated cluster: syncing previous existing changes");

      // current cluster that was activated
      final Cluster currentCluster = ((ClusterActivationNomadChange) nomadChanges.get(0).getNomadChange()).getCluster();

      // index until which we need to force a sync
      int pos = Check.lastIndexOfSameCommittedSourceTopologyChange(sourceNomadChanges, currentCluster);

      // Check if this node has been incorrectly activated with the wrong cluster.
      // This should never happen.
      if (pos == -1) {
        throw new IllegalStateException("Unable to find any change in the source node matching the topology used to activate this node: " + currentCluster);
      }

      // reset the node's changes
      nomadServer.reset();
      nomadChanges.clear();

      // There might be some changes in the source node, before the last source node's topology change matching
      // the node's cluster, which might not be related to this node.
      // So we need to force sync them without triggering the change applicators and without
      // by controlling how to save the config written on disk for all these commits
      if (pos >= 0) {
        LOGGER.info("This node is force-syncing {} historical changes", pos + 1);
        Iterable<NomadChangeInfo> iterable = () -> sourceNomadChanges.stream().limit(pos + 1).iterator();
        nomadServer.forceSync(iterable, (previousConfig, nomadChange) -> {
          DynamicConfigNomadChange dynamicConfigNomadChange = (DynamicConfigNomadChange) nomadChange;
          Cluster previous = previousConfig == null ? null : previousConfig.getCluster();
          Cluster update;
          if (dynamicConfigNomadChange instanceof TopologyNomadChange) {
            // If the change is a topology change, we just return the target topology without doing any validation.
            update = ((TopologyNomadChange) dynamicConfigNomadChange).getCluster();
          } else {
            // If the change is a setting change, we apply the setting changes to the topology
            try {
              update = dynamicConfigNomadChange.apply(previous);
            } catch (RuntimeException e) {
              // this change was not applicable probably because the topology we have
              // currently (i.e. new node) is not related to this config change
              update = previous;
            }
          }
          // note: previous won't be null here because each append log contains at least one topology change (activation)
          return nodeStartupConfiguration.withCluster(update).orElseGet(nodeStartupConfiguration::alone);
        });
      }

      // the normal sync will start at the common topology change
      sourceInd = pos + 1;

      // if we reset the append log of a node, we need to zap it
      requires.add(Require.ZAP_REQUIRED);

    } else {
      // This node is either not new or the activation was done at the same time with the source node
      // (so the beginning of the append log is the same)
      LOGGER.info("Comparing all nomad changes from source");

      // All the N changes of this node from 0 -> N-1 must be the same
      final int last = nomadChanges.size() - 1;
      Check.requireEquals(nomadChanges, sourceNomadChanges, 0, last);

      // Check if the last change can be repaired , otherwise, it has to match the source one.
      if (Check.canRepair(nomadChanges, sourceNomadChanges)) {
        Require require = repairNomadChange(sourceNomadChanges.get(last));
        requires.add(require);

      } else {
        Check.requireEquals(nomadChanges, sourceNomadChanges, last, 1);
      }

      sourceInd = last + 1;
    }

    // run the normal sync phase
    requires.addAll(normalSync(sourceNomadChanges, sourceInd));

    return requires;
  }

  private Collection<Require> normalSync(List<NomadChangeInfo> changes, int from) throws NomadException {
    Collection<Require> requires = new HashSet<>(2);
    if (from < changes.size()) {
      LOGGER.info("Node is syncing {} configuration changes", changes.size());
      for (; from < changes.size(); from++) {
        Require require = syncNomadChange(changes.get(from));
        requires.add(require);
      }
    } else {
      LOGGER.info("No configuration change left to sync");
    }
    return requires;
  }

  private Require repairNomadChange(NomadChangeInfo nomadChangeInfo) throws NomadException {
    LOGGER.info("Repairing prepared change version {} ({}) created at {} by {} from {}",
      nomadChangeInfo.getVersion(),
      nomadChangeInfo.getNomadChange().getSummary(),
      nomadChangeInfo.getCreationTimestamp(),
      nomadChangeInfo.getCreationUser(),
      nomadChangeInfo.getCreationHost());

    DiscoverResponse<NodeContext> discoverResponse = nomadServer.discover();

    if (discoverResponse.getLatestChange().getState() != PREPARED) {
      throw new AssertionError("Expected PREPARED state in change " + discoverResponse.getLatestChange());
    }

    switch (nomadChangeInfo.getChangeRequestState()) {
      case COMMITTED:
        commit(nomadChangeInfo, discoverResponse.getMutativeMessageCount());
        return Require.RESTART_REQUIRED;
      case ROLLED_BACK:
        rollback(nomadChangeInfo, discoverResponse.getMutativeMessageCount());
        return Require.CAN_CONTINUE;
      default:
        throw new AssertionError(nomadChangeInfo.getChangeRequestState());
    }
  }

  private Require syncNomadChange(NomadChangeInfo nomadChangeInfo) throws NomadException {
    LOGGER.debug("Syncing change version {} ({}) created at {} by {} from {}",
      nomadChangeInfo.getVersion(),
      nomadChangeInfo.getNomadChange().getSummary(),
      nomadChangeInfo.getCreationTimestamp(),
      nomadChangeInfo.getCreationUser(),
      nomadChangeInfo.getCreationHost());

    DiscoverResponse<NodeContext> discoverResponse = nomadServer.discover();
    long mutativeMessageCount = discoverResponse.getMutativeMessageCount();

    switch (nomadChangeInfo.getChangeRequestState()) {
      case PREPARED:
        prepare(nomadChangeInfo, mutativeMessageCount);
        return Require.CAN_CONTINUE;
      case COMMITTED:
        prepare(nomadChangeInfo, mutativeMessageCount);
        commit(nomadChangeInfo, mutativeMessageCount + 1);
        return Require.RESTART_REQUIRED;
      case ROLLED_BACK:
        prepare(nomadChangeInfo, mutativeMessageCount);
        rollback(nomadChangeInfo, mutativeMessageCount + 1);
        return Require.CAN_CONTINUE;
      default:
        throw new AssertionError(nomadChangeInfo.getChangeRequestState());
    }
  }

  private void prepare(NomadChangeInfo nomadChangeInfo, long mutativeMessageCount) throws NomadException {
    AcceptRejectResponse response = nomadServer.prepare(nomadChangeInfo.toPrepareMessage(mutativeMessageCount));
    if (!response.isAccepted()) {
      throw new NomadException("Prepare failure. " +
        "Reason: " + response + ". " +
        "Change:" + nomadChangeInfo.getNomadChange().getSummary());
    }
  }

  private void commit(NomadChangeInfo nomadChangeInfo, long mutativeMessageCount) throws NomadException {
    AcceptRejectResponse response = nomadServer.commit(nomadChangeInfo.toCommitMessage(mutativeMessageCount));
    if (!response.isAccepted()) {
      throw new NomadException("Unexpected commit failure. " +
        "Reason: " + response + ". " +
        "Change:" + nomadChangeInfo.getNomadChange().getSummary());
    }
  }

  private void rollback(NomadChangeInfo nomadChangeInfo, long mutativeMessageCount) throws NomadException {
    AcceptRejectResponse response = nomadServer.rollback(nomadChangeInfo.toRollbackMessage(mutativeMessageCount));
    if (!response.isAccepted()) {
      throw new NomadException("Unexpected rollback failure. " +
        "Reason: " + response + ". " +
        "Change:" + nomadChangeInfo.getNomadChange().getSummary());
    }
  }
}
