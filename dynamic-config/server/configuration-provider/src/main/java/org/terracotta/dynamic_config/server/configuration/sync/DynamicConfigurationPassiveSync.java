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
import org.terracotta.dynamic_config.api.service.DynamicConfigService;
import org.terracotta.nomad.messages.AcceptRejectResponse;
import org.terracotta.nomad.messages.DiscoverResponse;
import org.terracotta.nomad.server.NomadChangeInfo;
import org.terracotta.nomad.server.NomadException;
import org.terracotta.nomad.server.UpgradableNomadServer;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import static org.terracotta.nomad.server.ChangeRequestState.COMMITTED;
import static org.terracotta.nomad.server.ChangeRequestState.PREPARED;

public class DynamicConfigurationPassiveSync {

  private static final Logger LOGGER = LoggerFactory.getLogger(DynamicConfigurationPassiveSync.class);

  private final NodeContext nodeStartupConfiguration;
  private final UpgradableNomadServer<NodeContext> nomadServer;
  private final DynamicConfigService dynamicConfigService;
  private final Supplier<String> licenseContent;

  public DynamicConfigurationPassiveSync(NodeContext nodeStartupConfiguration,
                                         UpgradableNomadServer<NodeContext> nomadServer,
                                         DynamicConfigService dynamicConfigService,
                                         Supplier<String> licenseContent) {
    this.nodeStartupConfiguration = nodeStartupConfiguration;
    this.nomadServer = nomadServer;
    this.dynamicConfigService = dynamicConfigService;
    this.licenseContent = licenseContent;
  }

  public DynamicConfigSyncData getSyncData() {
    try {
      return new DynamicConfigSyncData(nomadServer.getAllNomadChanges(), licenseContent.get());
    } catch (NomadException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Nomad changes:
   * - SettingNomadChange (config change)
   * - MultiSettingNomadChange (contains several changes at once)
   * - TopologyNomadChange (activation, node addition, node removal)
   * <p>
   * Nomad processors are executed in this order:
   * - ConfigChangeApplicator: unwrap the MultiNomadChange, returns the new config if change is allowed
   * - ApplicabilityNomadChangeProcessor: only call following processors if stripe/node/cluster matches
   * - RoutingNomadChangeProcessor: call the right processor depending on the message type
   * <p>
   * 2 Kind of processors:
   * - SettingNomadChangeProcessor: handle config changes
   * - TopologyNomadChangeProcessor (activation, node addition, node removal): will update the config, or create a new one
   * <p>
   * Sync process from active A to passive P:
   * - Needs to sync the active append log into the passive one
   * - We want all the nodes to have the same append log
   * - We want to sync append log history from A that do not relate to P, without triggering the processors of P
   * - We want to reset and re-sync P append log when:
   * .... 1) P has been started pre-activated to join a pre-activated stripe
   * .... 2) P has been activated and restarted as passive as part of node addition
   */
  public Set<Require> sync(DynamicConfigSyncData data) throws NomadException {
    // sync the active append log in the passive append log
    Set<Require> requires = syncNomadChanges(data.getNomadChanges());

    // sync the license from active node
    syncLicense(data.getLicense());

    return requires;
  }

  private void syncLicense(String activeLicense) {
    String passiveLicense = licenseContent.get();
    if (!Objects.equals(activeLicense, passiveLicense)) {
      LOGGER.info("Syncing license");
      dynamicConfigService.upgradeLicense(activeLicense);
    }
  }

  private Set<Require> syncNomadChanges(List<NomadChangeInfo> activeNomadChanges) throws NomadException {
    List<NomadChangeInfo> passiveNomadChanges = nomadServer.getAllNomadChanges();

    // programming errors
    // to be able to start, there must be at least one committed activation change in the append log
    // so that we can boot on the tc-config file version 1.
    // DynamicConfigConfigurationProvider will catch such uncommitted changes and prevent startup (there won't be any available configuration)
    Check.assertNonEmpty(activeNomadChanges, passiveNomadChanges);
    Check.assertThat(() -> activeNomadChanges.get(0).getNomadChange() instanceof ClusterActivationNomadChange);
    Check.assertThat(() -> passiveNomadChanges.get(0).getNomadChange() instanceof ClusterActivationNomadChange);
    Check.assertThat(() -> activeNomadChanges.get(0).getChangeRequestState() == COMMITTED);
    Check.assertThat(() -> passiveNomadChanges.get(0).getChangeRequestState() == COMMITTED);

    if (passiveNomadChanges.size() > activeNomadChanges.size()) {
      throw new IllegalStateException("Passive has more configuration changes");
    }

    // record what to do after the sync (restart ? zap ?)
    Set<Require> requires = new HashSet<>(2);

    // is passive has just been activated ?
    final boolean passiveNew = Check.isPassiveMew(passiveNomadChanges);

    // is the passive node was activated at the same time of the active node or after ?
    final boolean jointActivation = Check.isJointActivation(passiveNomadChanges, activeNomadChanges);

    // index at which to start the normal sync
    int activeInd;

    if (passiveNew && !jointActivation) {
      LOGGER.info("New passive is joining an activated stripe: syncing previous existing changes");

      // passive cluster that was activated
      final Cluster passiveCluster = ((ClusterActivationNomadChange) passiveNomadChanges.get(0).getNomadChange()).getCluster();

      // index until which we need to force a sync
      int pos = Check.lastIndexOfSameCommittedActiveTopologyChange(activeNomadChanges, passiveCluster);

      // Check if the passive node has been incorrectly activated with the wrong cluster.
      // This should never happen.
      if (pos == -1) {
        throw new IllegalStateException("Unable to find any change in active node matching the topology used to activate this passive node: " + passiveCluster);
      }

      // reset the passive changes
      nomadServer.reset();
      passiveNomadChanges.clear();

      // There might be some changes in the active node, before the last active topology change matching
      // the passive cluster, which might not be related to this passive node.
      // So we need to force sync them without triggering the change applicators and without
      // by controlling how to save the config written on disk for all these commits
      if (pos >= 0) {
        LOGGER.info("Passive is force-syncing {} historical changes", pos + 1);
        Iterable<NomadChangeInfo> iterable = () -> activeNomadChanges.stream().limit(pos + 1).iterator();
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
          return nodeStartupConfiguration.withCluster(update);
        });
      }

      // the normal sync will start at the common topology change
      activeInd = pos + 1;

      // if we reset the append log of a node, we need to zap it
      requires.add(Require.ZAP_REQUIRED);

    } else {
      // passive is either not new or the activation was done at the same time with the active
      // (so the beginning of the append log is the same)
      LOGGER.info("Comparing all changes from active and passive node");

      // All the N changes of the passive node from 0 -> N-1 must be the same
      final int last = passiveNomadChanges.size() - 1;
      Check.requireEquals(passiveNomadChanges, activeNomadChanges, 0, last);

      // Check if the last change can be repaired , otherwise, it has to match the active one.
      if (Check.canRepair(passiveNomadChanges, activeNomadChanges)) {
        Require require = repairNomadChange(activeNomadChanges.get(last));
        requires.add(require);

      } else {
        Check.requireEquals(passiveNomadChanges, activeNomadChanges, last, 1);
      }

      activeInd = last + 1;
    }

    // run the normal sync phase
    requires.addAll(normalSync(activeNomadChanges, activeInd));

    return requires;
  }

  private Collection<Require> normalSync(List<NomadChangeInfo> changes, int from) throws NomadException {
    Collection<Require> requires = new HashSet<>(2);
    if (from < changes.size()) {
      LOGGER.info("Passive is syncing {} configuration changes", changes.size());
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
