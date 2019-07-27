/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.managers;

import com.tc.server.TCServerMain;
import com.terracottatech.diagnostic.server.DiagnosticServices;
import com.terracottatech.dynamic_config.diagnostic.LicensingService;
import com.terracottatech.dynamic_config.diagnostic.TopologyService;
import com.terracottatech.dynamic_config.diagnostic.TopologyServiceImpl;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.dynamic_config.model.util.ConfigUtils;
import com.terracottatech.dynamic_config.model.util.ParameterSubstitutor;
import com.terracottatech.dynamic_config.model.validation.LicenseValidator;
import com.terracottatech.dynamic_config.nomad.ClusterActivationNomadChange;
import com.terracottatech.dynamic_config.nomad.NomadBootstrapper;
import com.terracottatech.dynamic_config.nomad.NomadFailureRecorder;
import com.terracottatech.dynamic_config.parsing.Options;
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
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.terracottatech.dynamic_config.DynamicConfigConstants.DEFAULT_CONFIG_DIR;
import static com.terracottatech.dynamic_config.DynamicConfigConstants.DEFAULT_HOSTNAME;
import static com.terracottatech.dynamic_config.DynamicConfigConstants.DEFAULT_PORT;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.NODE_HOSTNAME;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.NODE_PORT;
import static com.terracottatech.dynamic_config.repository.NomadRepositoryManager.findNodeName;
import static com.terracottatech.utilities.Assertion.assertNonNull;
import static java.util.Collections.singleton;
import static java.util.Objects.requireNonNull;
import static org.terracotta.config.util.ParameterSubstitutor.substitute;

public class NodeManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(NodeManager.class);

  private final Options options;
  private final Map<String, String> paramValueMap;
  private final LicensingService licensingService;

  public NodeManager(Options options, Map<String, String> paramValueMap, LicensingService licensingService) {
    this.options = options;
    this.paramValueMap = paramValueMap;
    this.licensingService = licensingService;
  }

  public void startNode() {
    Path configRepositoryPath = Paths.get(options.getNodeConfigDir().map(ParameterSubstitutor::substitute).orElse(DEFAULT_CONFIG_DIR));
    startupWithConfigRepo(configRepositoryPath);

    if (options.getConfigFile() != null) {
      startupWithConfigFile(options.getConfigFile());
    }

    startupWithCliParams();

    throw new AssertionError("Unable to start server");
  }

  private void startupWithConfigRepo(Path configDir) {
    findNodeName(configDir).ifPresent(nodeName -> {
      LOGGER.info("Starting node {} from config repository: {}", nodeName, configDir);


      //TODO [DYNAMIC-CONFIG]: registerTopologyService(cluster, node, true);
      startNode("-r", configDir.toString(), "-n", nodeName, "--node-name", nodeName);
    });
  }

  private void startupWithConfigFile(Path configFile) {
    LOGGER.info("Starting node from config file: {}", configFile);
    Cluster cluster = ClusterManager.createCluster(configFile, options.getClusterName());
    Node node = getMatchingNodeFromConfigFile(cluster);
    if (options.getLicenseFile() != null) {
      if (cluster.getNodeCount() > 1) {
        //TODO [DYANMIC-CONFIG] TRACK #6: relax this constraint
        throw new UnsupportedOperationException("License file option can be used only with a one-node cluster config file");
      }
      createConfigRepository(cluster, node, options.getLicenseFile());
      startupWithConfigRepo(node.getNodeConfigDir());
    } else {
      startUnconfiguredNode(cluster, node);
    }
  }

  private void startupWithCliParams() {
    LOGGER.info("Starting node from command-line parameters");
    Cluster cluster = ClusterManager.createCluster(paramValueMap);
    Node node = cluster.getSingleNode().get(); // Cluster object will have only 1 node, just get that
    if (options.getLicenseFile() != null) {
      assertNonNull(options.getClusterName(), "clusterName must not be null and must be validated in " + Options.class.getName());
      createConfigRepository(cluster, node, options.getLicenseFile());
      startupWithConfigRepo(node.getNodeConfigDir());
    } else {
      startUnconfiguredNode(cluster, node);
    }
  }

  private void startUnconfiguredNode(Cluster cluster, Node node) {
    LOGGER.info("Starting node in UNCONFIGURED state");
    registerTopologyService(cluster, node, false);
    Path root = Paths.get("%(user.dir)");
    Path configPath = ConfigUtils.createTempTcConfig(node, root);
    startNode("-r", node.getNodeConfigDir().toString(), "--config-consistency", "--config", configPath.toAbsolutePath().toString(), "--node-name", node.getNodeName());
  }

  private void createConfigRepository(Cluster cluster, Node node, Path licenseFile) {
    LOGGER.info("Creating node config repository at: {}", node.getNodeConfigDir().toAbsolutePath());

    makeNomadWritable(cluster.getStripeId(node).get(), node);
    LOGGER.debug("Setting nomad writable successful");

    runNomadChange(cluster, node);
    LOGGER.debug("Nomad change run successful");

    installLicense(cluster, licenseFile);
    LOGGER.info("License installation successful");
  }

  private void makeNomadWritable(int stripeId, Node node) {
    NomadBootstrapper.bootstrap(node.getNodeConfigDir(), node.getNodeName());
    NomadBootstrapper.getNomadServerManager().upgradeForWrite(node.getNodeName(), stripeId);
  }

  private void runNomadChange(Cluster cluster, Node node) {
    String nomadServerName = node.getNodeAddress().toString();
    UpgradableNomadServer nomadServer = NomadBootstrapper.getNomadServerManager().getNomadServer();
    NomadClient nomadClient = new NomadClient(singleton(
        new NamedNomadServer(nomadServerName, nomadServer)),
        node.getNodeHostname(),
        "SYSTEM");
    NomadFailureRecorder failureRecorder = new NomadFailureRecorder();
    nomadClient.tryApplyChange(failureRecorder, new ClusterActivationNomadChange(cluster));
    //TODO [DYNAMIC-CONFIG]: TRACK #6 Consider the failure scenarios here, and rollback the change accordingly
    failureRecorder.reThrow();
  }

  private void installLicense(Cluster cluster, Path licenseFile) {
    LOGGER.info("Validating and installing license");
    LicenseValidator.validateLicense(cluster, licenseFile);
    try {
      String licenseContent = new String(Files.readAllBytes(licenseFile), StandardCharsets.UTF_8);
      licensingService.installLicense(licenseContent);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void registerTopologyService(Cluster cluster, Node node, boolean clusterActivated) {
    LOGGER.info("Registering TopologyService with DiagnosticServices");
    TopologyService topologyService = new TopologyServiceImpl(cluster, node, clusterActivated);
    DiagnosticServices.register(TopologyService.class, topologyService);
  }

  private Node getMatchingNodeFromConfigFile(Cluster cluster) {
    boolean isHostnameSpecified = options.getNodeHostname() != null;
    boolean isPortSpecified = options.getNodePort() != null;

    String substitutedHost = substitute(isHostnameSpecified ? options.getNodeHostname() : DEFAULT_HOSTNAME);
    String port = isPortSpecified ? options.getNodePort() : DEFAULT_PORT;

    List<Node> allNodes = cluster.getStripes().stream()
        .flatMap(stripe -> stripe.getNodes().stream())
        .collect(Collectors.toList());
    Optional<Node> matchingNodeOptional = allNodes.stream()
        .filter(node1 -> node1.getNodeHostname().equals(substitutedHost) && node1.getNodePort() == Integer.parseInt(port))
        .findAny();

    Node node;
    // See if we find a match for a node based on the specified params. If not, we see if the config file contains just one node
    if (matchingNodeOptional.isPresent()) {
      LOGGER.info("Found matching node entry from config file based on {}={} and {}={}", NODE_HOSTNAME, substitutedHost, NODE_PORT, port);
      node = matchingNodeOptional.get();
    } else if (!isHostnameSpecified && !isPortSpecified && allNodes.size() == 1) {
      LOGGER.info("Found only one node information in config file");
      node = allNodes.get(0);
    } else {
      throw new RuntimeException(
          String.format(
              "Did not find a matching node entry in config file: %s based on %s=%s and %s=%s",
              requireNonNull(options.getConfigFile()),
              NODE_HOSTNAME,
              substitutedHost,
              NODE_PORT,
              port
          )
      );
    }
    return node;
  }

  private void startNode(String... args) {
    TCServerMain.main(args);
  }
}
