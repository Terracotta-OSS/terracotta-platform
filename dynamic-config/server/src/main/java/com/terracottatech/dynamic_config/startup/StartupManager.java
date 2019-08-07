/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.startup;

import com.tc.server.TCServerMain;
import com.terracottatech.diagnostic.server.DiagnosticServices;
import com.terracottatech.dynamic_config.diagnostic.TopologyService;
import com.terracottatech.dynamic_config.diagnostic.TopologyServiceImpl;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.dynamic_config.model.NodeContext;
import com.terracottatech.dynamic_config.nomad.ClusterActivationNomadChange;
import com.terracottatech.dynamic_config.nomad.NomadBootstrapper;
import com.terracottatech.dynamic_config.repository.NomadRepositoryManager;
import com.terracottatech.dynamic_config.xml.XmlConfigMapper;
import com.terracottatech.nomad.client.NamedNomadServer;
import com.terracottatech.nomad.client.NomadClient;
import com.terracottatech.nomad.client.results.NomadFailureRecorder;
import com.terracottatech.utilities.PathResolver;
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

import static com.terracottatech.dynamic_config.DynamicConfigConstants.DEFAULT_REPOSITORY_DIR;
import static com.terracottatech.dynamic_config.DynamicConfigConstants.DEFAULT_HOSTNAME;
import static com.terracottatech.dynamic_config.DynamicConfigConstants.DEFAULT_PORT;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.NODE_HOSTNAME;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.NODE_PORT;
import static com.terracottatech.dynamic_config.util.ParameterSubstitutor.substitute;
import static java.util.Collections.singleton;
import static java.util.Objects.requireNonNull;

public class StartupManager {
  private static final Logger logger = LoggerFactory.getLogger(StartupManager.class);

  private final PathResolver pathResolver = new PathResolver(Paths.get("%(user.dir)"));

  void startUnconfigured(Cluster cluster, Node node) {
    String nodeName = node.getNodeName();
    logger.info("Starting node {} in UNCONFIGURED state", nodeName);
    Path noderepositoryDir = getOrDefaultRepositoryDir(node.getNodeRepositoryDir().toString());
    NomadBootstrapper.NomadServerManager nomadServerManager = NomadBootstrapper.bootstrap(noderepositoryDir, nodeName);
    registerTopologyService(new NodeContext(cluster, node), false, nomadServerManager);
    Path configPath = new TransientTcConfig(node, pathResolver).createTempTcConfigFile();
    startServer(
        "-r", node.getNodeRepositoryDir().toString(),
        "--config-consistency",
        "--config", configPath.toAbsolutePath().toString(),
        "--node-name", nodeName
    );
  }

  void startPreactivated(Cluster cluster, Node node, String licenseFile) {
    String nodeName = node.getNodeName();
    logger.info("Starting node {} in CONFIGURED state", nodeName);
    Path noderepositoryDir = getOrDefaultRepositoryDir(node.getNodeRepositoryDir().toString());
    NomadBootstrapper.NomadServerManager nomadServerManager = NomadBootstrapper.bootstrap(noderepositoryDir, nodeName);
    createConfigRepository(cluster, node, nomadServerManager);
    TopologyService topologyService = registerTopologyService(new NodeContext(cluster, node), true, nomadServerManager);
    topologyService.installLicense(read(licenseFile));
    startServer("-r", noderepositoryDir.toString(), "-n", nodeName, "--node-name", nodeName);
  }

  void startUsingConfigRepo(Path repositoryDir, String nodeName) {
    Path substituted = substitute(repositoryDir);
    logger.info("Starting node {} from config repository: {}", nodeName, substituted);
    NomadBootstrapper.NomadServerManager nomadServerManager = NomadBootstrapper.bootstrap(substituted, nodeName);
    XmlConfigMapper xmlConfigMapper = new XmlConfigMapper(pathResolver);
    NodeContext nodeContext = xmlConfigMapper.fromXml(nodeName, nomadServerManager.getConfiguration());
    nomadServerManager.upgradeForWrite(nodeContext.getStripeId(), nodeName);
    registerTopologyService(nodeContext, true, nomadServerManager);
    startServer("-r", repositoryDir.toString(), "-n", nodeName, "--node-name", nodeName);
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

  Path getOrDefaultRepositoryDir(String repositoryDir) {
    return Paths.get(repositoryDir != null ? repositoryDir : DEFAULT_REPOSITORY_DIR);
  }

  private void startServer(String... args) {
    TCServerMain.main(args);
  }

  Optional<String> findNodeName(Path repositoryDir) {
    return NomadRepositoryManager.findNodeName(repositoryDir);
  }

  private TopologyService registerTopologyService(NodeContext nodeContext, boolean clusterActivated, NomadBootstrapper.NomadServerManager nomadServerManager) {
    logger.info("Registering TopologyService with DiagnosticServices");
    TopologyService topologyService = new TopologyServiceImpl(nodeContext, clusterActivated, nomadServerManager);
    DiagnosticServices.register(TopologyService.class, topologyService);
    return topologyService;
  }

  private void createConfigRepository(Cluster cluster, Node node, NomadBootstrapper.NomadServerManager nomadServerManager) {
    logger.debug("Creating node config repository at: {}", substitute(node.getNodeRepositoryDir().toAbsolutePath()));

    nomadServerManager.upgradeForWrite(cluster.getStripeId(node).get(), node.getNodeName());
    logger.debug("Setting nomad writable successful");

    String nomadServerName = substitute(node.getNodeAddress().toString());
    NomadClient<String> nomadClient = new NomadClient<>(
        singleton(new NamedNomadServer<>(nomadServerName, nomadServerManager.getNomadServer())),
        substitute(node.getNodeHostname()),
        "SYSTEM"
    );
    NomadFailureRecorder<String> failureRecorder = new NomadFailureRecorder<>();
    nomadClient.tryApplyChange(failureRecorder, new ClusterActivationNomadChange(cluster));
    failureRecorder.reThrow();
    logger.debug("Nomad change run successful");
  }

  private static String read(String path) {
    try {
      return new String(Files.readAllBytes(Paths.get(substitute(path))), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

}
