/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.startup;

import com.tc.server.TCServerMain;
import com.terracottatech.diagnostic.server.DiagnosticServices;
import com.terracottatech.dynamic_config.diagnostic.LicensingService;
import com.terracottatech.dynamic_config.diagnostic.TopologyService;
import com.terracottatech.dynamic_config.diagnostic.TopologyServiceImpl;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.dynamic_config.model.util.TemporaryTcConfig;
import com.terracottatech.dynamic_config.model.validation.LicenseValidator;
import com.terracottatech.dynamic_config.nomad.ClusterActivationNomadChange;
import com.terracottatech.dynamic_config.nomad.NomadBootstrapper;
import com.terracottatech.dynamic_config.nomad.NomadFailureRecorder;
import com.terracottatech.nomad.client.NamedNomadServer;
import com.terracottatech.nomad.client.NomadClient;
import com.terracottatech.nomad.server.UpgradableNomadServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.terracottatech.dynamic_config.DynamicConfigConstants.DEFAULT_CONFIG_DIR;
import static com.terracottatech.dynamic_config.model.util.ParameterSubstitutor.substitute;
import static com.terracottatech.dynamic_config.repository.NomadRepositoryManager.findNodeName;
import static java.util.Collections.singleton;

abstract class NodeStarter {
  final Logger logger = LoggerFactory.getLogger(getClass());

  abstract void startNode(Cluster cluster, Node node);

  void startUnconfigured(Cluster cluster, Node node) {
    logger.info("Starting node in UNCONFIGURED state");
    registerTopologyService(cluster, node, false);
    Path root = Paths.get("%(user.dir)");
    Path configPath = new TemporaryTcConfig(node, root).createTempTcConfigFile();
    startServer("-r", node.getNodeConfigDir().toString(),
        "--config-consistency",
        "--config", configPath.toAbsolutePath().toString(),
        "--node-name", node.getNodeName()
    );
  }

  void startPreactivated(Cluster cluster, Node node, LicensingService licensingService, String licenseFile) {
    createConfigRepository(cluster, node, licensingService, licenseFile);
    Path nodeConfigDir = getOrDefaultConfigDir(node.getNodeConfigDir().toString());
    String nodeName = findNodeName(nodeConfigDir).orElseThrow(() -> new AssertionError("Created repository but couldn't extract node name from file"));
    startUsingConfigRepo(nodeConfigDir, nodeName);
  }

  void startUsingConfigRepo(Path nonNullConfigDir, String nodeName) {
    logger.info("Starting node {} from config repository: {}", nodeName, substitute(nonNullConfigDir));
    //TODO [DYNAMIC-CONFIG]: registerTopologyService(cluster, node, true);
    startServer("-r", nonNullConfigDir.toString(), "-n", nodeName, "--node-name", nodeName);
  }

  Path getOrDefaultConfigDir(String configDir) {
    return Paths.get(configDir != null ? configDir : DEFAULT_CONFIG_DIR);
  }

  void startServer(String... args) {
    TCServerMain.main(args);
  }

  private void registerTopologyService(Cluster cluster, Node node, boolean clusterActivated) {
    logger.info("Registering TopologyService with DiagnosticServices");
    TopologyService topologyService = new TopologyServiceImpl(cluster, node, clusterActivated);
    DiagnosticServices.register(TopologyService.class, topologyService);
  }

  private void createConfigRepository(Cluster cluster, Node node, LicensingService licensingService, String licenseFile) {
    logger.info("Creating node config repository at: {}", substitute(node.getNodeConfigDir().toAbsolutePath()));

    makeNomadWritable(cluster.getStripeId(node).get(), node);
    logger.debug("Setting nomad writable successful");

    runNomadChange(cluster, node);
    logger.debug("Nomad change run successful");

    installLicense(cluster, licensingService, licenseFile);
    logger.info("License installation successful");
  }

  private void makeNomadWritable(int stripeId, Node node) {
    NomadBootstrapper.bootstrap(node.getNodeConfigDir(), node.getNodeName());
    NomadBootstrapper.getNomadServerManager().upgradeForWrite(node.getNodeName(), stripeId);
  }

  private void runNomadChange(Cluster cluster, Node node) {
    String nomadServerName = substitute(node.getNodeAddress().toString());
    UpgradableNomadServer nomadServer = NomadBootstrapper.getNomadServerManager().getNomadServer();
    NomadClient nomadClient = new NomadClient(
        singleton(new NamedNomadServer(nomadServerName, nomadServer)),
        substitute(node.getNodeHostname()),
        "SYSTEM"
    );
    NomadFailureRecorder failureRecorder = new NomadFailureRecorder();
    nomadClient.tryApplyChange(failureRecorder, new ClusterActivationNomadChange(cluster));
    failureRecorder.reThrow();
  }

  private void installLicense(Cluster cluster, LicensingService licensingService, String licenseFile) {
    Path substitutedLicenseFile = Paths.get(substitute(licenseFile));
    logger.info("Validating and installing license");
    new LicenseValidator(cluster, substitutedLicenseFile).validate();
    try {
      String licenseContent = new String(Files.readAllBytes(substitutedLicenseFile), StandardCharsets.UTF_8);
      licensingService.installLicense(licenseContent);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
