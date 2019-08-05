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
import com.terracottatech.dynamic_config.model.validation.LicenseValidator;
import com.terracottatech.dynamic_config.nomad.ClusterActivationNomadChange;
import com.terracottatech.dynamic_config.nomad.NomadBootstrapper;
import com.terracottatech.nomad.client.results.NomadFailureRecorder;
import com.terracottatech.dynamic_config.repository.NomadRepositoryManager;
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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.terracottatech.dynamic_config.DynamicConfigConstants.DEFAULT_CONFIG_DIR;
import static com.terracottatech.dynamic_config.DynamicConfigConstants.DEFAULT_HOSTNAME;
import static com.terracottatech.dynamic_config.DynamicConfigConstants.DEFAULT_PORT;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.NODE_HOSTNAME;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.NODE_PORT;
import static com.terracottatech.dynamic_config.util.ParameterSubstitutor.substitute;
import static java.util.Collections.singleton;
import static java.util.Objects.requireNonNull;

public class StartupManager {
  private static final Logger logger = LoggerFactory.getLogger(StartupManager.class);

  void startUnconfigured(Cluster cluster, Node node) {
    logger.info("Starting node in UNCONFIGURED state");
    registerTopologyService(cluster, node, false);
    Path root = Paths.get("%(user.dir)");
    Path configPath = new TransientTcConfig(node, root).createTempTcConfigFile();
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

  Node getMatchingNodeFromConfigFile(String specifiedHostName, String specifiedPort, String specifiedConfigFile, Cluster cluster) {
    boolean isHostnameSpecified = specifiedHostName != null;
    boolean isPortSpecified = specifiedPort != null;

    String substitutedHost = substitute(isHostnameSpecified ? specifiedHostName : DEFAULT_HOSTNAME);
    String port = isPortSpecified ? specifiedPort : DEFAULT_PORT;

    List<Node> allNodes = cluster.getStripes().stream()
        .flatMap(stripe -> stripe.getNodes().stream())
        .collect(Collectors.toList());
    Optional<Node> matchingNodeOptional = allNodes.stream()
        .filter(node1 -> substitute(node1.getNodeHostname()).equals(substitutedHost) && node1.getNodePort() == Integer.parseInt(port))
        .findAny();

    Node node;
    // See if we find a match for a node based on the specified params. If not, we see if the config file contains just one node
    if (matchingNodeOptional.isPresent()) {
      logger.info("Found matching node entry from config file based on {}={} and {}={}", NODE_HOSTNAME, substitutedHost, NODE_PORT, port);
      node = matchingNodeOptional.get();
    } else if (!isHostnameSpecified && !isPortSpecified && allNodes.size() == 1) {
      logger.info("Found only one node information in config file");
      node = allNodes.get(0);
    } else {
      throw new RuntimeException(
          String.format(
              "Did not find a matching node entry in config file: %s based on %s=%s and %s=%s",
              requireNonNull(substitute(specifiedConfigFile)),
              NODE_HOSTNAME,
              substitutedHost,
              NODE_PORT,
              port
          )
      );
    }
    return node;
  }

  Path getOrDefaultConfigDir(String configDir) {
    return Paths.get(configDir != null ? configDir : DEFAULT_CONFIG_DIR);
  }

  private void startServer(String... args) {
    TCServerMain.main(args);
  }

  Optional<String> findNodeName(Path nonNullConfigDir) {
    return NomadRepositoryManager.findNodeName(nonNullConfigDir);
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
    UpgradableNomadServer<String> nomadServer = NomadBootstrapper.getNomadServerManager().getNomadServer();
    NomadClient<String> nomadClient = new NomadClient<>(
        singleton(new NamedNomadServer<>(nomadServerName, nomadServer)),
        substitute(node.getNodeHostname()),
        "SYSTEM"
    );
    NomadFailureRecorder<String> failureRecorder = new NomadFailureRecorder<>();
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
