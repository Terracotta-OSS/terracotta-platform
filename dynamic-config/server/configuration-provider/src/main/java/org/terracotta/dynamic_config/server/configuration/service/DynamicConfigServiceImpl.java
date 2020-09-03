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
package org.terracotta.dynamic_config.server.configuration.service;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.License;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.UID;
import org.terracotta.dynamic_config.api.model.nomad.DynamicConfigNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.MultiSettingNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.SettingNomadChange;
import org.terracotta.dynamic_config.api.service.ClusterValidator;
import org.terracotta.dynamic_config.api.service.DynamicConfigService;
import org.terracotta.dynamic_config.api.service.Props;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.server.api.DynamicConfigListener;
import org.terracotta.dynamic_config.server.api.InvalidLicenseException;
import org.terracotta.dynamic_config.server.api.LicenseService;
import org.terracotta.dynamic_config.server.configuration.sync.DynamicConfigNomadSynchronizer;
import org.terracotta.entity.StateDumpCollector;
import org.terracotta.entity.StateDumpable;
import org.terracotta.json.ObjectMapperFactory;
import org.terracotta.nomad.messages.AcceptRejectResponse;
import org.terracotta.nomad.messages.ChangeDetails;
import org.terracotta.nomad.messages.CommitMessage;
import org.terracotta.nomad.messages.DiscoverResponse;
import org.terracotta.nomad.messages.PrepareMessage;
import org.terracotta.nomad.messages.RollbackMessage;
import org.terracotta.nomad.server.NomadChangeInfo;
import org.terracotta.nomad.server.NomadException;
import org.terracotta.nomad.server.UpgradableNomadServer;
import org.terracotta.server.ServerEnv;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.lang.System.lineSeparator;
import static java.util.Objects.requireNonNull;
import static org.terracotta.server.StopAction.RESTART;
import static org.terracotta.server.StopAction.ZAP;

public class DynamicConfigServiceImpl implements TopologyService, DynamicConfigService, DynamicConfigListener, StateDumpable {

  private static final Logger LOGGER = LoggerFactory.getLogger(DynamicConfigServiceImpl.class);
  private static final String LICENSE_FILE_NAME = "license.xml";

  private final LicenseService licenseService;
  private final NomadServerManager nomadServerManager;
  private final Path licensePath;
  private final ObjectMapper objectMapper;
  private final ClusterValidator clusterValidator;

  private volatile NodeContext upcomingNodeContext;
  private volatile NodeContext runtimeNodeContext;
  private volatile boolean clusterActivated;

  public DynamicConfigServiceImpl(NodeContext nodeContext, LicenseService licenseService, NomadServerManager nomadServerManager, ObjectMapperFactory objectMapperFactory, ClusterValidator clusterValidator) {
    this.upcomingNodeContext = requireNonNull(nodeContext);
    this.runtimeNodeContext = requireNonNull(nodeContext);
    this.licenseService = requireNonNull(licenseService);
    this.nomadServerManager = requireNonNull(nomadServerManager);
    this.licensePath = nomadServerManager.getConfigurationManager().getLicensePath().resolve(LICENSE_FILE_NAME);
    this.objectMapper = objectMapperFactory.create();
    this.clusterValidator = requireNonNull(clusterValidator);
    if (hasLicenseFile()) {
      validateAgainstLicense(upcomingNodeContext.getCluster());
    }

    // This check is only present to safeguard against the possibility of a missing cluster validation in the call path
    clusterValidator.validate(nodeContext.getCluster());
  }

  // do not move this method up in the interface otherwise any client could access the license content through diagnostic port
  public synchronized Optional<String> getLicenseContent() {
    Path targetLicensePath = nomadServerManager.getConfigurationManager().getLicensePath().resolve(LICENSE_FILE_NAME);
    if (targetLicensePath.toFile().exists()) {
      try {
        return Optional.of(new String(Files.readAllBytes(targetLicensePath), StandardCharsets.UTF_8));
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
    return Optional.empty();
  }

  @Override
  public void resetAndSync(NomadChangeInfo[] nomadChanges, Cluster cluster) {
    UpgradableNomadServer<NodeContext> nomadServer = nomadServerManager.getNomadServer();
    DynamicConfigNomadSynchronizer nomadSynchronizer = new DynamicConfigNomadSynchronizer(
        nomadServerManager.getConfiguration().orElse(null), nomadServer);

    List<NomadChangeInfo> backup;
    Cluster thisTopology = upcomingNodeContext.getCluster();
    try {
      backup = nomadServer.getAllNomadChanges();
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
  }

  @Override
  public void addStateTo(StateDumpCollector stateDumpCollector) {
    stateDumpCollector.addState("licensePath", licensePath.toString());
    stateDumpCollector.addState("hasLicenseFile", hasLicenseFile());
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
    if (change.canApplyAtRuntime(runtimeNodeContext)) {
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
    LOGGER.info("Removed node: {} from stripe: {}", removedNode.getName(), runtimeNodeContext.getCluster().getStripe(stripeUID).get().getName());
  }

  @Override
  public void onNodeAddition(UID stripeUID, Node addedNode) {
    LOGGER.info("Added node: {} to stripe: {}", addedNode.getName(), runtimeNodeContext.getCluster().getStripe(stripeUID).get().getName());
  }

  @Override
  public void onStripeAddition(Stripe addedStripe) {
    LOGGER.info("Added stripe: {} to cluster: {}", addedStripe.toShapeString(), runtimeNodeContext.getCluster().toShapeString());
  }

  @Override
  public void onStripeRemoval(Stripe removedStripe) {
    LOGGER.info("Removed stripe: {} from cluster: {}", removedStripe.toShapeString(), runtimeNodeContext.getCluster().toShapeString());
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

  // this is the only real listener where we act on in this service

  @Override
  public void onNomadCommit(CommitMessage message, AcceptRejectResponse response, NomadChangeInfo changeInfo) {
    if (response.isAccepted()) {
      DynamicConfigNomadChange dynamicConfigNomadChange = (DynamicConfigNomadChange) changeInfo.getNomadChange();
      LOGGER.info("Nomad change {} committed: {}", message.getChangeUuid(), dynamicConfigNomadChange.getSummary());

      // extract the changes since there can be multiple settings change
      List<? extends DynamicConfigNomadChange> nomadChanges = MultiSettingNomadChange.extractChanges(dynamicConfigNomadChange.unwrap());

      // the following code will be executed on all the nodes, regardless of the applicability
      // level to update the config
      synchronized (this) {
        for (DynamicConfigNomadChange nomadChange : nomadChanges) {
          // first we update the upcoming one
          Cluster upcomingCluster = nomadChange.apply(upcomingNodeContext.getCluster());
          upcomingNodeContext = upcomingNodeContext.withCluster(upcomingCluster).orElseGet(upcomingNodeContext::alone);
          // if the change can be applied at runtime, it was previously done in the config change handler.
          // so update also the runtime topology there
          if (nomadChange.canApplyAtRuntime(upcomingNodeContext)) {
            Cluster runtimeCluster = nomadChange.apply(runtimeNodeContext.getCluster());
            runtimeNodeContext = runtimeNodeContext.withCluster(runtimeCluster).orElseGet(runtimeNodeContext::alone);
          }
        }
      }

      if (runtimeNodeContext.equals(upcomingNodeContext)) {
        LOGGER.info("New cluster configuration: {}{}", lineSeparator(), Props.toString(runtimeNodeContext.getCluster().toProperties(false, false, true)));
      } else {
        LOGGER.info("Pending cluster configuration: {}{}", lineSeparator(), Props.toString(upcomingNodeContext.getCluster().toProperties(false, false, true)));
      }
    } else {
      LOGGER.warn("Nomad change {} failed to commit: {}", message.getChangeUuid(), response);
    }
  }

  @Override
  public synchronized NodeContext getUpcomingNodeContext() {
    return upcomingNodeContext.clone();
  }

  @Override
  public synchronized NodeContext getRuntimeNodeContext() {
    return runtimeNodeContext.clone();
  }

  @Override
  public boolean isActivated() {
    return clusterActivated;
  }

  @Override
  public synchronized boolean mustBeRestarted() {
    return !runtimeNodeContext.equals(upcomingNodeContext);
  }

  @Override
  public boolean hasIncompleteChange() {
    return nomadServerManager.getNomadServer().hasIncompleteChange();
  }

  @Override
  public synchronized void setUpcomingCluster(Cluster updatedCluster) {
    if (isActivated()) {
      throw new IllegalStateException("Use Nomad instead to change the topology of activated node: " + runtimeNodeContext.getNode().getName());
    }

    requireNonNull(updatedCluster);

    clusterValidator.validate(updatedCluster);

    Node newMe = findMe(updatedCluster);

    if (newMe != null) {
      // we have updated the topology and I am still part of this cluster
      LOGGER.info("Set upcoming topology to:\n{}", updatedCluster);
      this.upcomingNodeContext = new NodeContext(updatedCluster, newMe.getUID());
    } else {
      // We have updated the topology and I am not part anymore of the cluster
      // So we just reset the cluster object so that this node is alone
      Node oldMe = upcomingNodeContext.getNode();
      LOGGER.info("Node {} ({}) removed from pending topology: {}", oldMe.getName(), oldMe.getUID(), updatedCluster.toShapeString());
      this.upcomingNodeContext = this.upcomingNodeContext.withOnlyNode(oldMe);
    }

    // When node is not yet activated, runtimeNodeContext == upcomingNodeContext
    this.runtimeNodeContext = upcomingNodeContext;
  }

  @Override
  public synchronized void activate(Cluster maybeUpdatedCluster, String licenseContent) {
    if (isActivated()) {
      throw new IllegalStateException("Node is already activated");
    }

    LOGGER.info("Preparing activation of cluster: {}", maybeUpdatedCluster.toShapeString());

    // validate that we are part of this cluster
    if (findMe(maybeUpdatedCluster) == null) {
      throw new IllegalArgumentException(String.format(
          "No match found for node: %s in cluster topology: %s",
          upcomingNodeContext.getNodeUID(),
          maybeUpdatedCluster
      ));
    }

    this.setUpcomingCluster(maybeUpdatedCluster);
    this.installLicense(licenseContent);

    LOGGER.info("Preparing activation of Node with validated topology: {}", upcomingNodeContext.getCluster().toShapeString());
    nomadServerManager.upgradeForWrite(upcomingNodeContext.getNodeUID());
    LOGGER.debug("Setting nomad writable successful");

    clusterActivated = true;
    LOGGER.info("Node activation successful");
  }

  @Override
  public void reset() {
    LOGGER.info("Resetting...");
    try {
      nomadServerManager.getNomadServer().reset();
      clusterActivated = false;
      nomadServerManager.downgradeForRead();
    } catch (NomadException e) {
      throw new IllegalStateException("Unable to reset Nomad system: " + e.getMessage(), e);
    }
  }

  @Override
  public void restart(Duration delayInSeconds) {
    LOGGER.info("Will restart node in {} seconds", delayInSeconds.getSeconds());
    runAfterDelay(delayInSeconds, () -> {
      LOGGER.info("Restarting node");
      ServerEnv.getServer().stop(RESTART);
    });
  }

  @Override
  public void stop(Duration delayInSeconds) {
    LOGGER.info("Will stop node in {} seconds", delayInSeconds.getSeconds());
    runAfterDelay(delayInSeconds, () -> {
      LOGGER.info("Stopping node");
      ServerEnv.getServer().stop(ZAP);
    });
  }

  @Override
  public synchronized void upgradeLicense(String licenseContent) {
    this.installLicense(licenseContent);
  }

  @Override
  public synchronized Optional<License> getLicense() {
    return hasLicenseFile() ? Optional.of(licenseService.parse(licensePath)) : Optional.empty();
  }

  @Override
  public NomadChangeInfo[] getChangeHistory() {
    try {
      return nomadServerManager.getNomadServer().getAllNomadChanges().toArray(new NomadChangeInfo[0]);
    } catch (NomadException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public synchronized boolean validateAgainstLicense(Cluster cluster) throws InvalidLicenseException {
    if (!hasLicenseFile()) {
      LOGGER.warn("Unable to validate cluster against license: license not installed: {}", cluster.toShapeString());
      return false;
    }
    licenseService.validate(licensePath, cluster);
    LOGGER.debug("License is valid for cluster: {}", cluster.toShapeString());
    return true;
  }

  private Map<String, ?> toMap(Object o) {
    try {
      JsonNode node = objectMapper.valueToTree(o);
      JsonParser jsonParser = objectMapper.treeAsTokens(node);
      return jsonParser.readValueAs(new TypeReference<Map<String, ?>>() {});
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private synchronized void installLicense(String licenseContent) {
    if (licenseContent != null) {
      Path tempFile = null;
      try {
        tempFile = Files.createTempFile("terracotta-license-", ".xml");
        Files.write(tempFile, licenseContent.getBytes(StandardCharsets.UTF_8));
        licenseService.validate(tempFile, upcomingNodeContext.getCluster());
        LOGGER.info("License validated");
        LOGGER.debug("Moving license file: {} to: {}", tempFile, licensePath);
        org.terracotta.utilities.io.Files.relocate(tempFile, licensePath, StandardCopyOption.REPLACE_EXISTING);
        LOGGER.info("License installed");
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      } finally {
        if (tempFile != null) {
          try {
            org.terracotta.utilities.io.Files.deleteIfExists(tempFile);
          } catch (IOException ignored) {
          }
        }
      }
      LOGGER.info("License installation successful");

    } else {
      LOGGER.info("No license installed");
      try {
        org.terracotta.utilities.io.Files.deleteIfExists(licensePath);
      } catch (IOException e) {
        LOGGER.warn("Error deleting existing license: " + e.getMessage(), e);
      }
    }
  }

  private boolean hasLicenseFile() {
    return licensePath.toFile().exists() && Files.isRegularFile(licensePath) && Files.isReadable(licensePath);
  }

  /**
   * Tries to find the node representing this process within the updated cluster.
   * <p>
   * - We cannot use the node hostname or port only, since they might have changed through a set command.
   * - We cannot use the node name and stripe ID only, since the stripe ID can have changed in the new cluster with the attach/detach commands
   * - Name could change following a set command when un-configured
   * <p>
   * So we try to find the best match we can...
   */
  private synchronized Node findMe(Cluster updatedCluster) {
    final Node me = upcomingNodeContext.getNode();
    for (Node node : updatedCluster.getNodes()) {
      if(node.getUID().equals(me.getUID())
          || node.getInternalAddress().equals(me.getInternalAddress())
          || node.getName().equals(me.getName())) {
        return node;
      }
    }
    return null;
  }

  private void runAfterDelay(Duration delayInSeconds, Runnable runnable) {
    // The delay helps the caller close the connection while it's live, otherwise it gets stuck for request timeout duration
    final long millis = delayInSeconds.toMillis();
    if (millis < 1_000) {
      throw new IllegalArgumentException("Invalid delay: " + delayInSeconds.getSeconds() + " seconds");
    }
    LOGGER.info("Node will restart in: {} seconds", delayInSeconds.getSeconds());
    new Thread(getClass().getSimpleName() + "-DelayedRestart") {
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
}
