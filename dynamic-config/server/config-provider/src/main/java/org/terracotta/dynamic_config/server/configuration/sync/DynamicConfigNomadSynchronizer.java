/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
import org.terracotta.dynamic_config.api.service.NomadChangeInfo;
import org.terracotta.dynamic_config.api.service.Props;
import org.terracotta.dynamic_config.api.server.DynamicConfigNomadServer;
import org.terracotta.nomad.messages.AcceptRejectResponse;
import org.terracotta.nomad.messages.DiscoverResponse;
import org.terracotta.nomad.server.ChangeRequestState;
import org.terracotta.nomad.server.NomadException;

import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toSet;
import static org.terracotta.dynamic_config.server.configuration.sync.Check.assertEmpty;
import static org.terracotta.dynamic_config.server.configuration.sync.Check.assertTrue;
import static org.terracotta.dynamic_config.server.configuration.sync.Check.unwrap;
import static org.terracotta.dynamic_config.server.configuration.sync.Require.NOTHING;
import static org.terracotta.dynamic_config.server.configuration.sync.Require.RESTART_REQUIRED;
import static org.terracotta.nomad.server.ChangeRequestState.COMMITTED;
import static org.terracotta.nomad.server.ChangeRequestState.PREPARED;
import static org.terracotta.nomad.server.ChangeRequestState.ROLLED_BACK;

public class DynamicConfigNomadSynchronizer {

  private static final Logger LOGGER = LoggerFactory.getLogger(DynamicConfigNomadSynchronizer.class);

  private final NodeContext nodeStartupConfiguration;
  private final DynamicConfigNomadServer nomadServer;

  public DynamicConfigNomadSynchronizer(NodeContext nodeStartupConfiguration, DynamicConfigNomadServer nomadServer) {
    this.nodeStartupConfiguration = nodeStartupConfiguration;
    this.nomadServer = nomadServer;
  }

  public Set<Require> syncNomadChanges(List<NomadChangeInfo> sourceChanges, Cluster sourceTopology) throws NomadException {
    // ensure the source list and this node's list of changes are all committed ones
    // We might have some relevant prepared changes at the end that we will handle after
    final Collection<NomadChangeInfo> changes = nomadServer.getChangeHistory();
    final Deque<NomadChangeInfo> relevantChanges = changes.stream().filter(c -> c.getChangeRequestState() == COMMITTED || c.getChangeRequestState() == PREPARED).collect(toCollection(LinkedList::new));
    final Deque<NomadChangeInfo> sourceRelevantChanges = sourceChanges.stream().filter(c -> c.getChangeRequestState() == COMMITTED || c.getChangeRequestState() == PREPARED).collect(toCollection(LinkedList::new));

    LOGGER.info("Syncing committed changes from source ({}) and this node ({})", sourceRelevantChanges.size(), relevantChanges.size());

    final NomadChangeInfo firstRelevantChange = relevantChanges.iterator().next();
    final NomadChangeInfo firstSourceRelevantChange = sourceRelevantChanges.iterator().next();

    // programming errors
    // to be able to start, there must be at least one committed activation change in the append log
    // so that we can boot on the tc-config file version 1.
    // DynamicConfigConfigurationProvider will catch such uncommitted changes and prevent startup (there won't be any available configuration)
    Check.assertNonEmpty(sourceRelevantChanges, relevantChanges);
    Check.assertTrue(unwrap(firstSourceRelevantChange.getNomadChange()) instanceof ClusterActivationNomadChange);
    Check.assertTrue(unwrap(firstRelevantChange.getNomadChange()) instanceof ClusterActivationNomadChange);
    Check.assertTrue(firstSourceRelevantChange.getChangeRequestState() == COMMITTED);
    Check.assertTrue(firstRelevantChange.getChangeRequestState() == COMMITTED);

    // record what to do after the sync (restart ? zap ?)
    Set<Require> requires = new HashSet<>(2);

    // is the node just been activated ?
    final boolean newNode = Check.isNodeNew(relevantChanges);

    // was the node activated at the same time as the source node or after ?
    final boolean jointActivation = Check.isJointActivation(firstRelevantChange, firstSourceRelevantChange);

    if (newNode && !jointActivation) {
      LOGGER.info("New node is joining an activated cluster: syncing previous existing changes");

      // current cluster that was activated
      final Cluster currentCluster = ((ClusterActivationNomadChange) unwrap(firstRelevantChange.getNomadChange())).getCluster();
      LOGGER.trace("This node topology at activation time: {}", currentCluster);

      int pos = Check.findLastSyncPosition(sourceRelevantChanges, sourceTopology, currentCluster)
          .orElseThrow(() -> new IllegalStateException("Unable to find any change in the source node matching the topology used to activate this node.\n" +
              Props.toString(currentCluster.toProperties(false, false, true), "Passive topology") + "\n" +
              Props.toString(sourceTopology.toProperties(false, false, true), "Active topology")
          ));

      // reset the node's changes
      LOGGER.trace("Reset and clear this node changes");
      nomadServer.reset();
      relevantChanges.clear();

      // There might be some changes in the source node, before the last source node's topology change matching
      // the node's cluster, which might not be related to this node.
      // So we need to force sync them without triggering the change applicators and without
      // by controlling how to save the config written on  disk for all these commits
      if (pos >= 0) {
        int count = pos + 1;
        LOGGER.info("This node is force-syncing {} historical changes", count);

        // grab all changes to force-sync
        Queue<NomadChangeInfo> forced = new LinkedList<>();
        while (!sourceRelevantChanges.isEmpty() && count-- > 0) {
          forced.offer(sourceRelevantChanges.poll());
        }

        nomadServer.forceSync(forced, (previousConfig, nomadChange) -> {
          LOGGER.trace("SYNC: {}", nomadChange);
          DynamicConfigNomadChange dynamicConfigNomadChange = ((DynamicConfigNomadChange) nomadChange).unwrap();
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

    } else {
      // This node is either not new or the activation was done at the same time with the source node
      // (so the beginning of the append log should be the same)
      //
      // We require all changes to be the same and with the same states, with some exceptions for the last entry.
      // The comparison list only has non rolled back changes.
      //
      // Example of scenario we can have during the "relevant" changes comparison:
      //
      // *  [0] A: P, P: P => change same and PREPARED => OK
      // *  [1] A: C, P: C => change same and COMMITTED => OK
      // *  [2] A: C, P: P => change on passive is PREPARED and COMMITTED on active => OK (partial commit) => repair
      // *  [3] A: P, P: C => change on passive is COMMITTED and PREPARED on active => OK (partial commit) => do nothing: user will be able to automatically repair
      // *  [4] A: R, P: R => change same and ROLLED BACK => OK => ignored
      // *  [5] A: P, P: R => change on passive is PREPARED and ROLLED BACK on active => OK (partial rollback) => repair
      // *  [6] A: R, P: P => change on passive is ROLLED BACK and PREPARED on active => OK (partial rollback) => do nothing: user will be able to automatically repair
      // *  [7] A: C, P: R => change on passive is ROLLED BACK and COMMITTED on active => ERR (inconsistent)
      // *  [8] A: R, P: C => change on passive is COMMITTED and ROLLED BACK on active => ERR (inconsistent)
      // *  [9] A: X, P: C => change on passive is COMMITTED but MISSING on active => ERR (ahead)
      // * [10] A: X, P: P => change on passive is PREPARED but MISSING on active => OK (partial prepare) => repair
      // * [11] A: P, P: X => change on passive is MISSING but PREPARED on active => OK => will sync (partial prepare or change in progress)
      // * [12] A: C, P: X => change on passive is MISSING but COMMITTED on active => OK => will sync
      // * [13] A: X, P: R => change on passive is ROLLED BACK but MISSING on active => OK => ignored
      // * [14] A: R, P: X => change on passive is MISSING but ROLLED BACK on active => OK => ignored

      LOGGER.info("Comparing all {} committed and prepared changes", relevantChanges.size());

      Set<UUID> rolledBack = Stream.concat(
          changes.stream(),
          sourceChanges.stream()
      ).filter(c -> c.getChangeRequestState() == ROLLED_BACK).map(NomadChangeInfo::getChangeUuid).collect(toSet());

      // check for inconsistencies with commits / rollbacks
      // check if the change has been rolled back on active
      for (NomadChangeInfo change : relevantChanges) {
        LOGGER.trace("PASS 1/3: {}", change);

        if (rolledBack.contains(change.getChangeUuid())) {
          ChangeRequestState state = change.getChangeRequestState();
          if (state == COMMITTED) {
            // [8]
            // inconsistent states
            throw new IllegalStateException("Node cannot sync because the configuration change history does not match: " + change + " has been rolled back on the source");

          } else {
            assertTrue(state == PREPARED);

            // [5]
            // we have a prepared change on the passive server which has been rolled back on the active (partial rollback)
            LOGGER.trace("REPAIR: {} => {}", change, ROLLED_BACK);
            Require require = repairNomadChange(change, ROLLED_BACK);
            requires.add(require);

            // we should be at the end with a prepared change
            assertTrue(Objects.equals(change, relevantChanges.peekLast()));
          }
        }
      }

      // check for inconsistencies with commits / rollbacks
      // check if the change has been rolled back on passive
      for (NomadChangeInfo source : sourceRelevantChanges) {
        LOGGER.trace("PASS 2/3: {}", source);

        if (rolledBack.contains(source.getChangeUuid())) {
          ChangeRequestState sourceState = source.getChangeRequestState();
          if (sourceState == COMMITTED) {
            // [7]
            // inconsistent states
            throw new IllegalStateException("Node cannot sync because the configuration change history does not match: " + source + " has been rolled back on this node");

          } else {
            assertTrue(sourceState == PREPARED);

            // [6]
            // we have a prepared change on the active server which has been rolled back on the passive
            // this can happen in case of a partially rolled back change mixed with a failover
            // we cannot repair a change on the active, but we can log
            LOGGER.warn(lineSeparator() + lineSeparator()
                + "==============================================================================================================" + lineSeparator()
                + "The last change in the source node is prepared, but rolled back in this node. Please run the 'repair' command." + lineSeparator()
                + "==============================================================================================================" + lineSeparator()
            );

            // if this happens, this change should be the latest on the active and has to be skipped,
            // because we do not want to sync it.
            // Otherwise, it will appear again in the passive append.log.
            // The change being prepared, it means we have reached the end of the source stream.
            assertTrue(Objects.equals(source, sourceRelevantChanges.peekLast()));

            // If we end up there it means that the current relevant change we are comparing from the passive node is ahead
            // of the active node since this node has a rollback entry matching this current source node (from active).
            //
            // This can happen when node A crashes during a partial rollback, rollback happened on P
            // (which was active before), then the user triggers a new change targeting P, and then shuts down the cluster,
            // and then restarts node A (which becomes active).
            //
            // A: C(1) P(2)
            // P: C(1) R(2) C(3)
            //
            // If this situation occurs, we just need to loop again, and the code below
            // will handle the changes we still have on the passive when the source list is empty
          }
        }
      }

      // We remove from the list (if found):
      // - prepared changes that we saw on the active that was rolled back on passive (this change would be at the end)
      // - prepared changes that we saw on the passive that was rolled back on the active (this change would be at the end, and it was repaired above)
      relevantChanges.removeIf(c -> rolledBack.contains(c.getChangeUuid()));
      sourceRelevantChanges.removeIf(c -> rolledBack.contains(c.getChangeUuid()));

      while (!relevantChanges.isEmpty()) {
        NomadChangeInfo change = relevantChanges.poll();
        ChangeRequestState state = change.getChangeRequestState();

        LOGGER.trace("PASS 3/3: {}", change);

        if (sourceRelevantChanges.isEmpty()) {
          // There is no more source changes for this passive change (MISSING)
          // this can happen if the last prepared change on passive was actually rolled back on active.
          // otherwise, if we see a committed change, this is a mismatch (passive would be ahead).
          if (state == COMMITTED) {
            // [9]
            // we have some committed changes (1 or more) in the passive server which are missing from the active
            throw new IllegalStateException("Node cannot sync because the configuration change history does not match: this node is ahead of the source: " + change);

          } else {
            assertTrue(state == PREPARED);

            // [10]
            // we have a prepared change in the passive server which is missing from the active.
            // this might be caused by a partially prepared change.
            // In both case, we have to rollback
            LOGGER.trace("REPAIR: {} => {}", change, ROLLED_BACK);
            Require require = repairNomadChange(change, ROLLED_BACK);
            requires.add(require);

            // this change has to be skipped, so move to the next one
            // note: a prepared change always being at the end, this should be a final operation
            assertEmpty(relevantChanges);
            continue;
          }
        }

        NomadChangeInfo source = sourceRelevantChanges.poll();
        ChangeRequestState sourceState = source.getChangeRequestState();

        // [0], [1], [2], [3]
        // We are comparing relevant changes

        if (!change.matches(source)) {
          throw new IllegalStateException("Node cannot sync because the configuration change history does not match: " + change + " does not match source: " + source);
        }

        if (state == COMMITTED && sourceState == COMMITTED) {
          // [1]
          // exact match of a committed change
          LOGGER.trace("MATCH: {}", change);

        } else if (state == PREPARED && sourceState == PREPARED) {
          // [0]
          // exact match of a prepared change. Please commit or rollback!!
          LOGGER.warn(lineSeparator() + lineSeparator()
              + "================================================================================================================================" + lineSeparator()
              + "The last change in this node and the source node node is still prepared. Please run the 'repair' command to complete the change." + lineSeparator()
              + "=================================================================================================================================" + lineSeparator()
          );

        } else if (state == PREPARED && sourceState == COMMITTED) {
          // [2]
          // change in passive needs to be repaired
          LOGGER.trace("REPAIR: {} => {}", change, COMMITTED);
          Require require = repairNomadChange(change, COMMITTED);
          requires.add(require);

        } else if (state == COMMITTED && sourceState == PREPARED) {
          // [3]
          LOGGER.warn(lineSeparator() + lineSeparator()
              + "==================================================================================================================" + lineSeparator()
              + "The last change in this node is committed, but still prepared in the source node. Please run the 'repair' command." + lineSeparator()
              + "==================================================================================================================" + lineSeparator()
          );

        } else {
          throw new AssertionError("state=" + state + ",sourceState=" + sourceState);
        }
      }
    }

    // [11] + [12]
    // run the normal sync phase with the new remaining changes
    requires.addAll(normalSync(sourceRelevantChanges));

    if (requires.isEmpty()) {
      requires.add(NOTHING);
    }

    return requires;
  }

  private Collection<Require> normalSync(Collection<NomadChangeInfo> changes) throws NomadException {
    Collection<Require> requires = new HashSet<>(2);
    if (!changes.isEmpty()) {
      LOGGER.info("Node is syncing {} new configuration changes", changes.size());
      for (NomadChangeInfo change : changes) {
        Require require = syncNomadChange(change);
        requires.add(require);
      }
    } else {
      LOGGER.info("No configuration change left to sync");
    }
    return requires;
  }

  private Require repairNomadChange(NomadChangeInfo nomadChangeInfo, ChangeRequestState newState) throws NomadException {
    LOGGER.info("Repairing prepared transaction version {} ({}) created at {} by {} from {}",
        nomadChangeInfo.getVersion(),
        nomadChangeInfo.getNomadChange().getSummary(),
        nomadChangeInfo.getCreationTimestamp(),
        nomadChangeInfo.getCreationUser(),
        nomadChangeInfo.getCreationHost());

    DiscoverResponse<NodeContext> discoverResponse = nomadServer.discover();

    if (discoverResponse.getLatestChange().getState() != PREPARED) {
      throw new AssertionError("Expected PREPARED state in change " + discoverResponse.getLatestChange());
    }

    switch (newState) {
      case COMMITTED:
        commit(nomadChangeInfo, discoverResponse.getMutativeMessageCount());
        return RESTART_REQUIRED;
      case ROLLED_BACK:
        rollback(nomadChangeInfo, discoverResponse.getMutativeMessageCount());
        return NOTHING;
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
        return NOTHING;
      case COMMITTED:
        prepare(nomadChangeInfo, mutativeMessageCount);
        commit(nomadChangeInfo, mutativeMessageCount + 1);
        return RESTART_REQUIRED;
      case ROLLED_BACK:
        prepare(nomadChangeInfo, mutativeMessageCount);
        rollback(nomadChangeInfo, mutativeMessageCount + 1);
        return NOTHING;
      default:
        throw new AssertionError(nomadChangeInfo.getChangeRequestState());
    }
  }

  private void prepare(NomadChangeInfo nomadChangeInfo, long mutativeMessageCount) throws NomadException {
    LOGGER.info("Prepare: {}", nomadChangeInfo);
    AcceptRejectResponse response = nomadServer.prepare(nomadChangeInfo.toPrepareMessage(mutativeMessageCount));
    if (!response.isAccepted()) {
      throw new NomadException("Prepare failure. " +
          "Reason: " + response + ". " +
          "Change:" + nomadChangeInfo.getNomadChange().getSummary());
    }
  }

  private void commit(NomadChangeInfo nomadChangeInfo, long mutativeMessageCount) throws NomadException {
    LOGGER.info("Commit: {}", nomadChangeInfo);
    AcceptRejectResponse response = nomadServer.commit(nomadChangeInfo.toCommitMessage(mutativeMessageCount));
    if (!response.isAccepted()) {
      throw new NomadException("Unexpected commit failure. " +
          "Reason: " + response + ". " +
          "Change:" + nomadChangeInfo.getNomadChange().getSummary());
    }
  }

  private void rollback(NomadChangeInfo nomadChangeInfo, long mutativeMessageCount) throws NomadException {
    LOGGER.info("Rollback: {}", nomadChangeInfo);
    AcceptRejectResponse response = nomadServer.rollback(nomadChangeInfo.toRollbackMessage(mutativeMessageCount));
    if (!response.isAccepted()) {
      throw new NomadException("Unexpected rollback failure. " +
          "Reason: " + response + ". " +
          "Change:" + nomadChangeInfo.getNomadChange().getSummary());
    }
  }
}