/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.diagnostic;

import com.tc.server.TCServerMain;
import com.terracottatech.License;
import com.terracottatech.dynamic_config.model.Cluster;
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
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class TopologyServiceImpl implements TopologyService {
  private static final Logger LOGGER = LoggerFactory.getLogger(TopologyServiceImpl.class);
  private static final String LICENSE_FILE_NAME = "license.xml";

  private volatile NodeContext nodeContext;
  private volatile License license;
  private boolean clusterActivated;
  private final NomadServerManager nomadServerManager;
  private final IParameterSubstitutor substitutor;

  public TopologyServiceImpl(NodeContext nodeContext, NomadServerManager nomadServerManager, IParameterSubstitutor substitutor) {
    this.nodeContext = requireNonNull(nodeContext);
    this.nomadServerManager = requireNonNull(nomadServerManager);
    this.substitutor = requireNonNull(substitutor);
    if (loadLicense()) {
      validateAgainstLicense();
    }
  }

  /**
   * Called by startup manager to signify that the node has been activated
   */
  public void activated() {
    this.clusterActivated = true;
  }

  public void upgradeNomadForWrite() {
    LOGGER.info("Preparing activation of Node with validated topology: {}", nodeContext.getCluster());
    nomadServerManager.upgradeForWrite(nodeContext.getStripeId(), nodeContext.getNodeName(), nodeContext.getCluster());
    LOGGER.debug("Setting nomad writable successful");
  }

  @Override
  public Node getThisNode() {
    return nodeContext.getNode();
  }

  @Override
  public InetSocketAddress getThisNodeAddress() {
    return getThisNode().getNodeAddress();
  }

  @Override
  public void restart() {
    LOGGER.info("Executing restart on node: {} in stripe: {}", getThisNode().getNodeName(), nodeContext.getStripeId());
    TCServerMain.getServer().stop(PlatformService.RestartMode.STOP_AND_RESTART);
  }

  @Override
  public synchronized Cluster getCluster() {
    return nodeContext.getCluster();
  }

  @Override
  public boolean isActivated() {
    return clusterActivated;
  }

  @Override
  public void setCluster(Cluster cluster) {
    requireNonNull(cluster);

    if (isActivated()) {
      throw new AssertionError("This method cannot be used at runtime when node is activated. Use Nomad instead.");

    } else {
      new ClusterValidator(cluster, substitutor).validate();

      Node oldMe = getThisNode();
      InetSocketAddress myNodeAddress = oldMe.getNodeAddress();
      Optional<Node> newMe = cluster.getNode(myNodeAddress);
      if (newMe.isPresent()) {
        // we have updated the topology and I am still part of this cluster
        LOGGER.info("Set pending topology to: {}", cluster);
        this.nodeContext = new NodeContext(cluster, newMe.get());
      } else {
        // We have updated the topology and I am not part anymore of the cluster
        // So we just reset the cluster object so that this node is alone
        LOGGER.info("Node {} removed from pending topology: {}", myNodeAddress, cluster);
        this.nodeContext = new NodeContext(new Cluster(new Stripe(oldMe)), oldMe);
      }

    }
  }

  @Override
  public void prepareActivation(Cluster cluster, String licenseContent) {
    if (isActivated()) {
      throw new IllegalStateException("Node is already activated");
    }

    LOGGER.info("Preparing activation of cluster: {}", cluster);

    // validate that we are part of this cluster
    Node oldMe = getThisNode();
    InetSocketAddress myNodeAddress = oldMe.getNodeAddress();
    Node node = cluster.getNode(myNodeAddress).orElse(null);
    if (node == null) {
      throw new IllegalArgumentException(String.format(
          "No match found for node: %s in cluster topology: %s",
          oldMe.getNodeAddress(),
          cluster.getNodeAddresses()
      ));
    }

    this.setCluster(cluster);
    this.installLicense(licenseContent);

    upgradeNomadForWrite();
  }

  @Override
  public synchronized void upgradeLicense(String licenseContent) {
    if (this.license == null) {
      throw new IllegalStateException("Cannot upgrade license: none has been installed first");
    }
    this.installLicense(licenseContent);
  }

  @Override
  public Optional<License> getLicense() {
    return Optional.ofNullable(license);
  }

  @Override
  public void validateAgainstLicense(Cluster cluster) {
    if (this.license == null) {
      throw new IllegalStateException("Cannot validate against license: none has been installed first");
    }
    LicenseValidator licenseValidator = new LicenseValidator(getCluster(), license);
    licenseValidator.validate();
    LOGGER.debug("License is valid for cluster: {}", cluster);
  }

  private void validateAgainstLicense() {
    validateAgainstLicense(getCluster());
  }

  private void installLicense(String licenseContent) {
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
}
