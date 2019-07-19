/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.managers;

import com.tc.server.TCServerMain;
import com.terracottatech.diagnostic.server.DiagnosticServices;
import com.terracottatech.dynamic_config.diagnostic.LicensingService;
import com.terracottatech.dynamic_config.diagnostic.LicensingServiceImpl;
import com.terracottatech.dynamic_config.diagnostic.TopologyService;
import com.terracottatech.dynamic_config.diagnostic.TopologyServiceImpl;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.dynamic_config.model.util.ConfigUtils;
import com.terracottatech.dynamic_config.model.validation.LicenseValidator;
import com.terracottatech.dynamic_config.nomad.ClusterActivationNomadChange;
import com.terracottatech.dynamic_config.nomad.NomadBootstrapper;
import com.terracottatech.dynamic_config.nomad.NomadEnvironment;
import com.terracottatech.dynamic_config.parsing.Options;
import com.terracottatech.dynamic_config.repository.NodeNameExtractor;
import com.terracottatech.nomad.messages.CommitMessage;
import com.terracottatech.nomad.messages.PrepareMessage;
import com.terracottatech.nomad.server.NomadException;
import com.terracottatech.nomad.server.UpgradableNomadServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.terracottatech.dynamic_config.DynamicConfigConstants.DEFAULT_HOSTNAME;
import static com.terracottatech.dynamic_config.DynamicConfigConstants.DEFAULT_PORT;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.NODE_HOSTNAME;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.NODE_PORT;
import static com.terracottatech.dynamic_config.model.util.ConfigUtils.getSubstitutedConfigDir;
import static org.terracotta.config.util.ParameterSubstitutor.substitute;

public class NodeManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(NodeManager.class);

  private final Options options;
  private final Map<String, String> paramValueMap;
  private String licenseFile;
  private LicensingServiceImpl licensingService;

  public NodeManager(Options options, Map<String, String> paramValueMap) {
    this.options = options;
    this.paramValueMap = paramValueMap;
  }

  public void startNode() {
    String substitutedConfigDir = getSubstitutedConfigDir(options.getNodeConfigDir());
    attemptStartupWithConfigRepo(substitutedConfigDir);
    attemptStartupWithConfigFile();
    attemptStartupWithCliParams();
  }

  private void attemptStartupWithCliParams() {
    Cluster cluster = ClusterManager.createCluster(paramValueMap);
    Node node = cluster.getStripes().get(0).getNodes().iterator().next(); // Cluster object will have only 1 node, just get that

    if (options.getLicenseFile() != null) {
      String clusterName = options.getClusterName();
      if (clusterName == null) {
        throw new RuntimeException("Cluster name is required with license file parameter");
      }
      cluster.setName(clusterName);
      activateAndStartup(cluster, node);
    } else {
      registerServices(cluster, node);
      startNodeUsingTempConfig(node);
    }
  }

  private void attemptStartupWithConfigFile() {
    String configFile = options.getConfigFile();
    if (configFile != null) {
      LOGGER.info("Reading cluster config properties file from: {}", configFile);
      Cluster cluster = ClusterManager.createCluster(configFile, options.getClusterName());
      Node node = getMatchingNodeFromConfigFile(cluster);

      if (options.getLicenseFile() != null) {
        if (cluster.getStripes().stream().mapToLong(stripe -> stripe.getNodes().size()).sum() > 1) {
          throw new RuntimeException("License file option can be used only with a one-node cluster config file");
        }
        activateAndStartup(cluster, node);
      } else {
        registerServices(cluster, node);
        startNodeUsingTempConfig(node);
      }
    }
  }

  private void activateAndStartup(Cluster cluster, Node node) {
    licenseFile = options.getLicenseFile();
    registerServices(cluster, node);

    makeNomadWritable(cluster.getStripeId(node).get(), node);
    LOGGER.debug("Setting nomad writable successful");

    runNomadChange(cluster);
    LOGGER.debug("Nomad change run successful");

    installLicense(cluster);
    LOGGER.info("License installation successful");

    attemptStartupWithConfigRepo(node.getNodeConfigDir().toString());
  }

  private void makeNomadWritable(int stripeId, Node node) {
    NomadBootstrapper.bootstrap(node.getNodeConfigDir(), node.getNodeName());
    NomadBootstrapper.getNomadServerManager().upgradeForWrite(node.getNodeName(), stripeId);
  }

  private void runNomadChange(Cluster cluster) {
    UpgradableNomadServer nomadServer = NomadBootstrapper.getNomadServerManager().getNomadServer();
    NomadEnvironment nomadEnvironment = new NomadEnvironment();
    ClusterActivationNomadChange change = new ClusterActivationNomadChange(cluster);
    PrepareMessage prepareMessage = new PrepareMessage(
        1,
        nomadEnvironment.getHost(),
        nomadEnvironment.getUser(),
        UUID.randomUUID(),
        1,
        change
    );
    CommitMessage commitMessage = new CommitMessage(
        2,
        nomadEnvironment.getHost(),
        nomadEnvironment.getUser(),
        UUID.randomUUID()
    );
    try {
      //TODO [DYNAMIC-CONFIG]: TRACK #6 Consider the failure scenarios here, and rollback the change accordingly
      nomadServer.discover();
      nomadServer.prepare(prepareMessage);
      nomadServer.commit(commitMessage);
    } catch (NomadException e) {
      throw new RuntimeException(e);
    }
  }

  private void installLicense(Cluster cluster) {
    LicenseValidator.validateLicense(cluster, licenseFile);
    LOGGER.debug("License validation successful");

    String validatedLicense;
    try {
      validatedLicense = Files.readAllLines(Paths.get(licenseFile)).stream().collect(Collectors.joining(System.lineSeparator()));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    licensingService.installLicense(validatedLicense);
  }

  private void startNodeUsingTempConfig(Node node) {
    LOGGER.info("Attempting node startup in UNCONFIGURED state");
    Path configPath = ConfigUtils.createTempTcConfig(node);
    startNode("-r", node.getNodeConfigDir().toString(), "--config-consistency", "--config", configPath.toAbsolutePath().toString(), "--node-name", node.getNodeName());
  }

  private void registerServices(Cluster cluster, Node node) {
    TopologyService topologyService = new TopologyServiceImpl(cluster, node, false);
    DiagnosticServices.register(TopologyService.class, topologyService);
    LOGGER.info("Registered TopologyServiceImpl with DiagnosticServices");

    licensingService = new LicensingServiceImpl();
    DiagnosticServices.register(LicensingService.class, licensingService);
    LOGGER.info("Registered LicensingServiceImpl with DiagnosticServices");
  }

  private void attemptStartupWithConfigRepo(String substitutedConfigDir) {
    Optional<String> nodeNameOptional = NodeNameExtractor.extractFromConfigOptional(Paths.get(substitutedConfigDir));
    if (nodeNameOptional.isPresent()) {
      LOGGER.info("Found config repository at: {}", nodeNameOptional.get());
      //TODO [DYNAMIC-CONFIG]: Invoke registerServices(cluster, node) here once cluster and node can be found via TDB-4594
      startNode("-r", substitutedConfigDir, "-n", nodeNameOptional.get(), "--node-name", nodeNameOptional.get());
    }
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
              options.getConfigFile(),
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
