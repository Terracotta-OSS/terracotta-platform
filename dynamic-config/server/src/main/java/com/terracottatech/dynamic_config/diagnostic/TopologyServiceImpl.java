/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.diagnostic;

import com.tc.server.TCServerMain;
import com.terracottatech.License;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.dynamic_config.model.Stripe;
import com.terracottatech.dynamic_config.model.Topology;
import com.terracottatech.dynamic_config.nomad.NomadBootstrapper;
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

  private volatile Topology topology;
  private License license;
  private final boolean clusterActivated;
  private final NomadBootstrapper.NomadServerManager nomadServerManager;

  public TopologyServiceImpl(Topology topology,
                             boolean clusterActivated,
                             NomadBootstrapper.NomadServerManager nomadServerManager) {
    this.topology = requireNonNull(topology);
    this.clusterActivated = clusterActivated;
    this.nomadServerManager = requireNonNull(nomadServerManager);

    loadLicense();
  }

  @Override
  public Node getThisNode() {
    return topology.getNode();
  }

  @Override
  public InetSocketAddress getThisNodeAddress() {
    return getThisNode().getNodeAddress();
  }

  @Override
  public void restart() {
    LOGGER.info("Executing restart on node: {} in stripe: {}", getThisNode().getNodeName(), topology.getStripeId());
    TCServerMain.getServer().stop(PlatformService.RestartMode.STOP_AND_RESTART);
  }

  @Override
  public Cluster getTopology() {
    return topology.getCluster();
  }

  @Override
  public boolean isActivated() {
    return clusterActivated;
  }

  @Override
  public synchronized void setTopology(Cluster cluster) {
    requireNonNull(cluster);

    if (isActivated()) {
      throw new UnsupportedOperationException("Unable to change the topology at runtime");

    } else {
      Node oldMe = getThisNode();
      InetSocketAddress myNodeAddress = oldMe.getNodeAddress();
      Optional<Node> newMe = cluster.getNode(myNodeAddress);
      if (newMe.isPresent()) {
        // we have updated the topology and I am still part of this cluster
        LOGGER.info("Set pending topology to: {}", cluster);
        this.topology = new Topology(cluster, topology.getStripeId(), newMe.get().getNodeName());
      } else {
        // We have updated the topology and I am not part anymore of the cluster
        // So we just reset the cluster object so that this node is alone
        LOGGER.info("Node {} removed from pending topology: {}", myNodeAddress, cluster);
        this.topology = new Topology(new Cluster(new Stripe(oldMe)), topology.getStripeId(), oldMe.getNodeName());
      }

    }
  }

  @Override
  public void prepareActivation(Cluster validatedTopology) {
    if (isActivated()) {
      throw new IllegalStateException("Node is already activated");
    }
    Node me = getThisNode();
    Node node = validatedTopology.getStripes()
        .stream()
        .flatMap(stripe -> stripe.getNodes().stream())
        .filter(node1 -> node1.getNodeHostname().equals(me.getNodeHostname()) && node1.getNodePort() == me.getNodePort())
        .findFirst()
        .orElseThrow(() -> {
          String message = String.format(
              "No match found for host: %s and port: %s in cluster topology: %s",
              me.getNodeHostname(),
              me.getNodePort(),
              validatedTopology
          );
          return new IllegalArgumentException(message);
        });

    LOGGER.info("Preparing activation of Node with validated topology: {}", validatedTopology);
    nomadServerManager.upgradeForWrite(node.getNodeName(), validatedTopology.getStripeId(node).get());
  }

  @Override
  public void installLicense(String xml) {
    LOGGER.info("Validating license");
    Path licenseFile = nomadServerManager.getRepositoryManager().getLicensePath().resolve(LICENSE_FILE_NAME);

    Path tempFile;
    try {
      tempFile = Files.createTempFile("terracotta-license-", ".xml");
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    try {
      Files.write(tempFile, xml.getBytes(StandardCharsets.UTF_8));

      License license = new LicenseParser(tempFile).parse();
      LicenseValidator licenseValidator = new LicenseValidator(getTopology(), license);
      licenseValidator.validate();

      LOGGER.info("Installing license");
      Files.move(tempFile, licenseFile, StandardCopyOption.REPLACE_EXISTING);
      LOGGER.debug("License file: {} successfully copied to: {}", LICENSE_FILE_NAME, nomadServerManager.getRepositoryManager().getLicensePath());
      LOGGER.info("License installation successful");

      this.license = license;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } finally {
      try {
        Files.deleteIfExists(tempFile);
      } catch (IOException ignored) {
      }
    }
  }

  @Override
  public Optional<License> getLicense() {
    return Optional.ofNullable(license);
  }

  private void loadLicense() {
    Path licenseFile = nomadServerManager.getRepositoryManager().getLicensePath().resolve(LICENSE_FILE_NAME);
    if (Files.exists(licenseFile)) {
      LOGGER.info("Reloading license");
      License license = new LicenseParser(licenseFile).parse();
      LicenseValidator licenseValidator = new LicenseValidator(getTopology(), license);
      licenseValidator.validate();

      this.license = license;
    }
  }
}
