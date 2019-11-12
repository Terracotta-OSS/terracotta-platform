/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.service;

import com.tc.server.TCServerMain;
import com.terracottatech.License;
import com.terracottatech.dynamic_config.diagnostic.DynamicConfigService;
import com.terracottatech.dynamic_config.diagnostic.TopologyService;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Configuration;
import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.dynamic_config.model.NodeContext;
import com.terracottatech.dynamic_config.model.Stripe;
import com.terracottatech.dynamic_config.model.validation.ClusterValidator;
import com.terracottatech.dynamic_config.nomad.NomadBootstrapper.NomadServerManager;
import com.terracottatech.dynamic_config.util.IParameterSubstitutor;
import com.terracottatech.dynamic_config.validation.LicenseValidator;
import com.terracottatech.licensing.LicenseParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.monitoring.PlatformService;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class DynamicConfigServiceImpl implements TopologyService, DynamicConfigService {
  private static final Logger LOGGER = LoggerFactory.getLogger(DynamicConfigServiceImpl.class);
  private static final String LICENSE_FILE_NAME = "license.xml";

  private final NomadServerManager nomadServerManager;
  private final IParameterSubstitutor substitutor;

  private volatile NodeContext upcomingNodeContext;
  private volatile NodeContext runtimeNodeContext;
  private volatile License license;
  private volatile boolean clusterActivated;

  public DynamicConfigServiceImpl(NodeContext nodeContext, NomadServerManager nomadServerManager, IParameterSubstitutor substitutor) {
    this.upcomingNodeContext = requireNonNull(nodeContext);
    this.runtimeNodeContext = requireNonNull(nodeContext);
    this.nomadServerManager = requireNonNull(nomadServerManager);
    this.substitutor = requireNonNull(substitutor);
    if (loadLicense()) {
      validateAgainstLicense();
    }
  }

  /**
   * called from startup manager (in case we want a pre-activated node) (and this class) to make Nomad RW.
   */
  public synchronized void activate() {
    if (isActivated()) {
      throw new AssertionError("Already activated");
    }
    LOGGER.info("Preparing activation of Node with validated topology: {}", upcomingNodeContext.getCluster());
    nomadServerManager.upgradeForWrite(upcomingNodeContext.getStripeId(), upcomingNodeContext.getNodeName(), upcomingNodeContext.getCluster());
    LOGGER.debug("Setting nomad writable successful");

    clusterActivated = true;
  }

  /**
   * called from Nomad just after a config repository change has been committed and persisted
   */
  public synchronized void newTopologyCommitted(NodeContext updatedNodeContext) {
    if (!isActivated()) {
      throw new AssertionError("Not activated");
    }
    this.upcomingNodeContext = updatedNodeContext;
  }

  /**
   * called from Nomad just after change has been applied at runtime
   */
  public synchronized void newConfigurationAppliedAtRuntime(Configuration configuration) {
    if (!isActivated()) {
      throw new AssertionError("Not activated");
    }
    configuration.apply(runtimeNodeContext.getCluster(), substitutor);
  }

  @Override
  public synchronized NodeContext getUpcomingNodeContext() {
    return upcomingNodeContext;
  }

  @Override
  public NodeContext getRuntimeNodeContext() {
    return runtimeNodeContext;
  }

  @Override
  public void restart() {
    LOGGER.info("Executing restart on node: {} in stripe: {}", runtimeNodeContext.getNodeName(), runtimeNodeContext.getStripeId());
    TCServerMain.getServer().stop(PlatformService.RestartMode.STOP_AND_RESTART);
  }

  @Override
  public boolean isActivated() {
    return clusterActivated;
  }

  @Override
  public boolean isRestartRequired() {
    return !runtimeNodeContext.equals(upcomingNodeContext);
  }

  @Override
  public synchronized void setUpcomingCluster(Cluster updatedCluster) {
    if (isActivated()) {
      throw new AssertionError("This method cannot be used at runtime when node is activated. Use Nomad instead.");
    }

    requireNonNull(updatedCluster);

    new ClusterValidator(updatedCluster, substitutor).validate();

    Node oldMe = upcomingNodeContext.getNode();
    Node newMe = findMe(updatedCluster);

    if (newMe != null) {
      // we have updated the topology and I am still part of this cluster
      LOGGER.info("Set pending topology to: {}", updatedCluster);
      this.upcomingNodeContext = new NodeContext(updatedCluster, newMe);
    } else {
      // We have updated the topology and I am not part anymore of the cluster
      // So we just reset the cluster object so that this node is alone
      LOGGER.info("Node {} ({}) removed from pending topology: {}", oldMe.getNodeName(), oldMe.getNodeAddress(), updatedCluster);
      this.upcomingNodeContext = new NodeContext(new Cluster(new Stripe(oldMe)), oldMe);
    }

    // When node is not yest activated, runtimeNodeContext == upcomingNodeContext
    this.runtimeNodeContext = upcomingNodeContext;
  }

  @Override
  public synchronized void prepareActivation(Cluster maybeUpdatedCluster, String licenseContent) {
    if (isActivated()) {
      throw new IllegalStateException("Node is already activated");
    }

    LOGGER.info("Preparing activation of cluster: {}", maybeUpdatedCluster);

    // validate that we are part of this cluster
    if (findMe(maybeUpdatedCluster) == null) {
      throw new IllegalArgumentException(String.format(
          "No match found for node: %s in cluster topology: %s",
          upcomingNodeContext.getNodeName(),
          maybeUpdatedCluster
      ));
    }

    this.setUpcomingCluster(maybeUpdatedCluster);
    this.installLicense(licenseContent);

    activate();
  }

  @Override
  public synchronized void upgradeLicense(String licenseContent) {
    if (this.license == null) {
      throw new IllegalStateException("Cannot upgrade license: none has been installed first");
    }
    this.installLicense(licenseContent);
  }

  @Override
  public synchronized Optional<License> getLicense() {
    return Optional.ofNullable(license);
  }

  @Override
  public synchronized void validateAgainstLicense(Cluster cluster) {
    if (this.license == null) {
      throw new IllegalStateException("Cannot validate against license: none has been installed first");
    }
    LicenseValidator licenseValidator = new LicenseValidator(cluster, license);
    licenseValidator.validate();
    LOGGER.debug("License is valid for cluster: {}", cluster);
  }

  private void validateAgainstLicense() {
    validateAgainstLicense(upcomingNodeContext.getCluster());
  }

  private synchronized void installLicense(String licenseContent) {
    LOGGER.info("Installing license");

    License backup = this.license;
    Path tempFile = null;

    try {
      tempFile = Files.createTempFile("terracotta-license-", ".xml");
      Files.write(tempFile, licenseContent.getBytes(StandardCharsets.UTF_8));
      this.license = new LicenseParser(tempFile).parse();

      validateAgainstLicense();
      moveLicense(tempFile);
    } catch (IOException e) {
      // rollback to previous license (or null) on IO error
      this.license = backup;
      throw new UncheckedIOException(e);
    } catch (RuntimeException e) {
      // rollback to previous license (or null) on validation error
      this.license = backup;
      throw e;

    } finally {
      if (tempFile != null) {
        try {
          Files.deleteIfExists(tempFile);
        } catch (IOException ignored) {
        }
      }
    }

    LOGGER.info("License installation successful");
  }

  private void moveLicense(Path tempFile) {
    Path licensePath = nomadServerManager.getRepositoryManager().getLicensePath();
    Path destination = licensePath.resolve(LICENSE_FILE_NAME);
    LOGGER.debug("Moving license file: {} to: {}", tempFile, destination);
    try {
      Files.move(tempFile, destination, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private boolean loadLicense() {
    Path licenseFile = nomadServerManager.getRepositoryManager().getLicensePath().resolve(LICENSE_FILE_NAME);
    if (Files.exists(licenseFile)) {
      LOGGER.info("Reloading license");
      this.license = new LicenseParser(licenseFile).parse();
      return true;
    }
    return false;
  }

  /**
   * Tries to find the node representing this process within the updated cluster.
   * <p>
   * - We cannot use the node hostname or port only, since they might have changed through a set command.
   * - We cannot use the node name and stripe id only, since the stripe id can have changed in the new cluster with the attach/detach commands
   * <p>
   * So we try to find the best match we can...
   */
  private Node findMe(Cluster updatedCluster) {
    final Node me = upcomingNodeContext.getNode();
    return updatedCluster.getNode(me.getNodeAddress())
        .orElseGet(() -> updatedCluster.getNode(upcomingNodeContext.getStripeId(), me.getNodeName())
            .orElse(null));
  }
}