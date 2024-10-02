/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2025
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
package org.terracotta.dynamic_config.server.configuration.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.ClusterState;
import org.terracotta.dynamic_config.api.model.License;
import org.terracotta.dynamic_config.api.model.LockContext;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.UID;
import org.terracotta.dynamic_config.api.model.nomad.DynamicConfigNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.LockAwareDynamicConfigNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.LockConfigNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.MultiSettingNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.SettingNomadChange;
import org.terracotta.dynamic_config.api.service.ClusterValidator;
import org.terracotta.dynamic_config.api.service.DynamicConfigService;
import org.terracotta.dynamic_config.api.service.NomadChangeInfo;
import org.terracotta.dynamic_config.api.service.Props;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.api.server.DynamicConfigListener;
import org.terracotta.dynamic_config.api.server.DynamicConfigNomadServer;
import org.terracotta.dynamic_config.api.server.InvalidLicenseException;
import org.terracotta.dynamic_config.api.server.LicenseService;
import org.terracotta.dynamic_config.server.configuration.sync.DynamicConfigNomadSynchronizer;
import org.terracotta.entity.StateDumpCollector;
import org.terracotta.entity.StateDumpable;
import org.terracotta.json.Json;
import org.terracotta.nomad.client.change.NomadChange;
import org.terracotta.nomad.messages.AcceptRejectResponse;
import org.terracotta.nomad.messages.ChangeDetails;
import org.terracotta.nomad.messages.CommitMessage;
import org.terracotta.nomad.messages.DiscoverResponse;
import org.terracotta.nomad.messages.PrepareMessage;
import org.terracotta.nomad.messages.RollbackMessage;
import org.terracotta.nomad.server.ChangeState;
import org.terracotta.nomad.server.NomadException;
import org.terracotta.server.Server;

import java.time.Duration;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.lang.System.lineSeparator;
import static java.util.Objects.requireNonNull;
import static java.util.function.Predicate.isEqual;
import static org.terracotta.dynamic_config.api.model.LockTag.ALLOW_SCALING;
import static org.terracotta.dynamic_config.api.model.LockTag.DENY_SCALE_IN;
import static org.terracotta.dynamic_config.api.model.LockTag.DENY_SCALE_OUT;
import static org.terracotta.nomad.server.ChangeRequestState.COMMITTED;
import static org.terracotta.server.StopAction.RESTART;

public final class DynamicConfigServiceImpl implements TopologyService, DynamicConfigService, DynamicConfigListener, StateDumpable {

  private static final Logger LOGGER = LoggerFactory.getLogger(DynamicConfigServiceImpl.class);

  private final NomadServerManager nomadServerManager;
  private final Json json;
  private final Server server;
  private final Topologies topologies;
  private final Licensing licensing;

  public DynamicConfigServiceImpl(NodeContext nodeContext, LicenseService licenseService, NomadServerManager nomadServerManager, Json.Factory jsonFactory, Server server) {
    this.topologies = new Topologies(nodeContext);
    this.nomadServerManager = requireNonNull(nomadServerManager);
    this.licensing = new Licensing(licenseService, nomadServerManager);
    this.json = jsonFactory.create();
    this.server = requireNonNull(server);

    // ensure we start with a minimally valid configuration
    // if the node gets activated, the validator will be called in the activate() method with the appropriate cluster state
    new ClusterValidator(nodeContext.getCluster()).validate(ClusterState.CONFIGURING);
  }

  @Override
  public Optional<String> getLicenseContent() {
    return licensing.getLicenseContent();
  }

  @Override
  public void resetAndSync(NomadChangeInfo[] nomadChanges, Cluster cluster) {
    DynamicConfigNomadServer nomadServer = nomadServerManager.getNomadServer();
    DynamicConfigNomadSynchronizer nomadSynchronizer = new DynamicConfigNomadSynchronizer(nomadServerManager.getConfiguration().orElse(null), nomadServer);

    topologies.withUpcoming(upcomingNodeContext -> {
      Cluster thisTopology = upcomingNodeContext.getCluster();

      List<NomadChangeInfo> backup;

      try {
        backup = nomadServer.getChangeHistory();
      } catch (NomadException e) {
        throw new IllegalStateException("Unable to reset and sync Nomad system: " + e.getMessage(), e);
      }

      try {
        nomadSynchronizer.syncNomadChanges(Arrays.asList(nomadChanges), cluster);
      } catch (NomadException e) {
        try {
          nomadServer.reset();
          nomadSynchronizer.syncNomadChanges(backup, thisTopology);
        } catch (NomadException nomadException) {
          e.addSuppressed(nomadException);
        }
        throw new IllegalStateException("Unable to reset and sync Nomad system: " + e.getMessage(), e);
      }
    });
  }

  @Override
  public void addStateTo(StateDumpCollector stateDumpCollector) {
    stateDumpCollector.addState("licensePath", licensing.getLicenseFile().toString());
    stateDumpCollector.addState("hasLicenseFile", licensing.isInstalled());
    stateDumpCollector.addState("configurationDir", nomadServerManager.getConfigurationManager().getConfigurationDirectory().toString());
    stateDumpCollector.addState("activated", isActivated());
    stateDumpCollector.addState("mustBeRestarted", mustBeRestarted());
    stateDumpCollector.addState("runtimeNodeContext", toMap(getRuntimeNodeContext()));
    stateDumpCollector.addState("upcomingNodeContext", toMap(getUpcomingNodeContext()));
    StateDumpCollector nomad = stateDumpCollector.subStateDumpCollector("Nomad");
    try {
      DiscoverResponse<NodeContext> discoverResponse = nomadServerManager.getNomadServer().discover();
      nomad.addState("mode", discoverResponse.getMode().name());
      nomad.addState("currentVersion", discoverResponse.getCurrentVersion());
      nomad.addState("highestVersion", discoverResponse.getHighestVersion());
      nomad.addState("mutativeMessageCount", discoverResponse.getMutativeMessageCount());
      nomad.addState("lastMutationUser", discoverResponse.getLastMutationUser());
      nomad.addState("lastMutationHost", discoverResponse.getLastMutationHost());
      nomad.addState("lastMutationTimestamp", discoverResponse.getLastMutationTimestamp());
      ChangeDetails<NodeContext> changeDetails = discoverResponse.getLatestChange();
      if (changeDetails != null) {
        StateDumpCollector latestChange = stateDumpCollector.subStateDumpCollector("latestChange");
        latestChange.addState("uuid", changeDetails.getChangeUuid().toString());
        latestChange.addState("state", changeDetails.getState().name());
        latestChange.addState("creationUser", changeDetails.getCreationUser());
        latestChange.addState("creationHost", changeDetails.getCreationHost());
        latestChange.addState("creationTimestamp", changeDetails.getCreationTimestamp());
        latestChange.addState("version", changeDetails.getVersion());
        latestChange.addState("summary", changeDetails.getOperation().getSummary());
      }
    } catch (NomadException e) {
      nomad.addState("error", e.getMessage());
    }
  }

  // we only listen to log

  @Override
  public void onSettingChanged(SettingNomadChange change, Cluster updated) {
    if (change.canUpdateRuntimeTopology(getRuntimeNodeContext())) {
      LOGGER.info("Configuration change: {} applied at runtime", change.getSummary());
    } else {
      LOGGER.info("Configuration change: {} will be applied after restart", change.getSummary());
    }
  }

  @Override
  public void onNewConfigurationSaved(NodeContext nodeContext, Long version) {
    LOGGER.info("New configuration version: {} has been saved", version);
  }

  @Override
  public void onNodeRemoval(UID stripeUID, Node removedNode) {
    LOGGER.info("Removed node: {} from stripe: {}", removedNode.getName(), getRuntimeNodeContext().getCluster().getStripe(stripeUID).get().getName());
  }

  @Override
  public void onNodeAddition(UID stripeUID, Node addedNode) {
    LOGGER.info("Added node: {} to stripe: {}", addedNode.getName(), getRuntimeNodeContext().getCluster().getStripe(stripeUID).get().getName());
  }

  @Override
  public void onStripeAddition(Stripe addedStripe) {
    LOGGER.info("Added stripe: {} to cluster: {}", addedStripe.toShapeString(), getRuntimeNodeContext().getCluster().toShapeString());
  }

  @Override
  public void onStripeRemoval(Stripe removedStripe) {
    LOGGER.info("Removed stripe: {} from cluster: {}", removedStripe.toShapeString(), getRuntimeNodeContext().getCluster().toShapeString());
  }

  @Override
  public void onNomadPrepare(PrepareMessage message, AcceptRejectResponse response) {
    if (response.isAccepted()) {
      LOGGER.info("Nomad change {} prepared: {}", message.getChangeUuid(), message.getChange().getSummary());
    } else {
      LOGGER.warn("Nomad change {} failed to prepare: {}", message.getChangeUuid(), response);
    }
  }

  @Override
  public void onNomadRollback(RollbackMessage message, AcceptRejectResponse response) {
    if (response.isAccepted()) {
      LOGGER.info("Nomad change {} rolled back", message.getChangeUuid());
    } else {
      LOGGER.warn("Nomad change {} failed to rollback: {}", message.getChangeUuid(), response);
    }
  }

  @Override
  public void onNomadCommit(CommitMessage message, AcceptRejectResponse response, ChangeState<NodeContext> changeState) {
    if (response.isAccepted()) {
      DynamicConfigNomadChange dynamicConfigNomadChange = (DynamicConfigNomadChange) changeState.getChange();
      LOGGER.info("Nomad change {} committed: {}", message.getChangeUuid(), dynamicConfigNomadChange.getSummary());

      // extract the changes since there can be multiple settings change
      List<? extends DynamicConfigNomadChange> nomadChanges = MultiSettingNomadChange.extractChanges(dynamicConfigNomadChange.unwrap());

      topologies.update(nomadChanges);

      if (topologies.areSame()) {
        LOGGER.info("New cluster configuration: {}{}", lineSeparator(), Props.toString(getRuntimeNodeContext().getCluster().toProperties(false, false, true)));
      } else {
        LOGGER.info("Pending cluster configuration: {}{}", lineSeparator(), Props.toString(getUpcomingNodeContext().getCluster().toProperties(false, false, true)));
      }

      topologies.warnIfProblematicConsistency();
    } else {
      LOGGER.warn("Nomad change {} failed to commit: {}", message.getChangeUuid(), response);
    }
  }

  @Override
  public NodeContext getUpcomingNodeContext() {
    return topologies.getUpcomingNodeContext();
  }

  @Override
  public NodeContext getRuntimeNodeContext() {
    return topologies.getRuntimeNodeContext();
  }

  @Override
  public boolean isActivated() {
    // a node is activated when nomad is enabled and a last committed config is available
    return nomadServerManager.getNomadMode() == NomadMode.RW
        && nomadServerManager.getConfiguration().isPresent();
  }

  @Override
  public boolean mustBeRestarted() {
    return !topologies.areSame();
  }

  @Override
  public boolean hasIncompleteChange() {
    return nomadServerManager.getNomadServer().hasIncompleteChange();
  }

  @Override
  public void setUpcomingCluster(Cluster updatedCluster) {
    if (isActivated()) {
      // we only allow direct replacement if the node is not activated
      throw new IllegalStateException("Use Nomad instead to change the topology of activated node: " + getRuntimeNodeContext().getNode().getName());
    }
    topologies.install(updatedCluster);
  }

  @Override
  public void activate(Cluster maybeUpdatedCluster, String licenseContent) {
    LOGGER.info("Activating configuration system on this node with topology: {}", maybeUpdatedCluster.toShapeString());

    // This check is only present to safeguard against the possibility of a missing cluster validation in the call path
    new ClusterValidator(maybeUpdatedCluster).validate(ClusterState.ACTIVATED);

    // validate that we are part of this cluster
    if (!topologies.containsMe(maybeUpdatedCluster)) {
      throw new IllegalArgumentException(String.format(
          "No match found for node: %s in cluster topology: %s",
          getUpcomingNodeContext().getNodeUID(),
          maybeUpdatedCluster
      ));
    }

    NodeContext installed = topologies.install(maybeUpdatedCluster);

    // activate nomad system if this wasn't done before then just make sure we can send Nomad transactions
    nomadServerManager.initNomad();
    nomadServerManager.setNomad(NomadMode.RW);

    // install the license AFTER the nomad system is initialized (no config directory exist before)
    licensing.install(licenseContent, installed.getCluster());

    topologies.warnIfProblematicConsistency();

    LOGGER.info("Configuration system activated");
  }

  @Override
  public void reset() {
    LOGGER.info("Resetting...");
    try {
      nomadServerManager.reset();
    } catch (NomadException e) {
      throw new IllegalStateException("Unable to reset Nomad system: " + e.getMessage(), e);
    }
  }

  @Override
  public void restart(Duration delay) {
    LOGGER.info("Will restart node in {} seconds", delay.getSeconds());
    runAfterDelay(delay, () -> {
      LOGGER.info("Restarting node");
      server.stop(RESTART);
    });
  }

  @Override
  public void restartIfPassive(Duration delay) {
    LOGGER.info("Will restart node in {} seconds (if passive)", delay.getSeconds());
    runAfterDelay(delay, () -> {
      LOGGER.info("Restarting node");
      server.stopIfPassive(RESTART);
    });
  }

  @Override
  public void restartIfActive(Duration delay) {
    LOGGER.info("Will restart node in {} seconds (if active)", delay.getSeconds());
    runAfterDelay(delay, () -> {
      LOGGER.info("Restarting node");
      server.stopIfActive(RESTART);
    });
  }

  @Override
  public void stop(Duration delayInSeconds) {
    LOGGER.info("Will stop node in {} seconds", delayInSeconds.getSeconds());
    runAfterDelay(delayInSeconds, () -> {
      LOGGER.info("Stopping node");
      server.stop();
    });
  }

  @Override
  public void upgradeLicense(String licenseContent) {
    if (licenseContent == null) {
      licensing.uninstall();
    } else {
      licensing.install(licenseContent, getUpcomingNodeContext().getCluster());
    }
  }

  @Override
  public Optional<License> getLicense() {
    return licensing.parse();
  }

  @Override
  public NomadChangeInfo[] getChangeHistory() {
    try {
      return nomadServerManager.getNomadServer().getChangeHistory().toArray(new NomadChangeInfo[0]);
    } catch (NomadException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public boolean validateAgainstLicense(Cluster cluster) throws InvalidLicenseException {
    return licensing.validate(cluster);
  }

  private static NomadChange getChange(NomadChangeInfo nomadChangeInfo) {
    return nomadChangeInfo.getNomadChange() instanceof LockAwareDynamicConfigNomadChange ?
        ((LockAwareDynamicConfigNomadChange) nomadChangeInfo.getNomadChange()).unwrap() :
        nomadChangeInfo.getNomadChange();
  }

  @Override
  public boolean isLocked() {
    return getRuntimeNodeContext().getCluster().getConfigurationLockContext().isConfigured();
  }

  private Map<String, ?> toMap(Object o) {
    return json.mapToObject(o);
  }

  private void runAfterDelay(Duration delayInSeconds, Runnable runnable) {
    // The delay helps the caller close the connection while it's live, otherwise it gets stuck for request timeout duration
    final long millis = delayInSeconds.toMillis();
    if (millis < 1_000) {
      throw new IllegalArgumentException("Invalid delay: " + delayInSeconds.getSeconds() + " seconds");
    }
    new Thread(getClass().getSimpleName() + "-DelayedRestart") {
      {
        {
          setDaemon(true);
        }
      }

      @Override
      public void run() {
        try {
          sleep(millis);
        } catch (InterruptedException e) {
          // do nothing, still try to kill server
        }
        runnable.run();
      }
    }.start();
  }

  @Override
  public boolean isScalingDenied() {
    try {
      return !isActivated() || mustBeRestarted() || hasIncompleteChange() || nomadServerManager.getNomadServer().getChangeHistory()
          .stream()
          .filter(nomadChangeInfo -> nomadChangeInfo.getChangeRequestState() == COMMITTED)
          .filter(nomadChangeInfo -> getChange(nomadChangeInfo) instanceof LockConfigNomadChange && ((LockConfigNomadChange) getChange(nomadChangeInfo)).getLockContext().getOwnerTags() != null)
          .sorted(Comparator.comparing(NomadChangeInfo::getVersion).reversed()) // most recent first
          .map(DynamicConfigServiceImpl::getChange)
          .map(LockConfigNomadChange.class::cast)
          .map(LockConfigNomadChange::getLockContext)
          .map(LockContext::getOwnerTags)
          .filter(isEqual(DENY_SCALE_IN).or(isEqual(DENY_SCALE_OUT)).or(isEqual(ALLOW_SCALING)))
          .findFirst() // get the most recently committed marker: either deny or allow or nothing
          .filter(isEqual(DENY_SCALE_IN).or(isEqual(DENY_SCALE_OUT))) // check if the last one is deny
          .isPresent(); // true if last one is deny, false if last one is allow or no marker
    } catch (NomadException e) {
      throw new IllegalStateException(e);
    }
  }
}
