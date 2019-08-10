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
import com.terracottatech.dynamic_config.nomad.NomadBootstrapper.NomadServerManager;
import com.terracottatech.dynamic_config.repository.NomadRepositoryManager;
import com.terracottatech.dynamic_config.util.IParameterSubstitutor;
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
import java.util.Collection;
import java.util.Optional;

import static com.terracottatech.dynamic_config.DynamicConfigConstants.DEFAULT_HOSTNAME;
import static com.terracottatech.dynamic_config.DynamicConfigConstants.DEFAULT_PORT;
import static com.terracottatech.dynamic_config.DynamicConfigConstants.DEFAULT_REPOSITORY_DIR;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.NODE_HOSTNAME;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.NODE_PORT;
import static java.util.Collections.singleton;
import static java.util.Objects.requireNonNull;

public class StartupManager {
  private static final Logger logger = LoggerFactory.getLogger(StartupManager.class);

  private final IParameterSubstitutor parameterSubstitutor;

  public StartupManager(IParameterSubstitutor parameterSubstitutor) {
    this.parameterSubstitutor = parameterSubstitutor;
  }

  void startUnconfigured(Cluster cluster, Node node, String optionalNodeRepositoryFromCLI) {
    String nodeName = node.getNodeName();
    logger.info("Starting node: {} in UNCONFIGURED state", nodeName);
    Path nodeRepositoryDir = getOrDefaultRepositoryDir(optionalNodeRepositoryFromCLI);
    NomadServerManager nomadServerManager = NomadBootstrapper.bootstrap(nodeRepositoryDir, nodeName, parameterSubstitutor);
    registerTopologyService(new NodeContext(cluster, node), false, nomadServerManager);
    // This resolver will make sure to rebase the relative given path only if the given path is not absolute
    // and this check happens after doing the substitution.
    // Note: the returned resolved path is not substituted and contains placeholders from both base directory and given path.
    PathResolver userDirResolver = new PathResolver(Paths.get("%(user.dir)"), parameterSubstitutor::substitute);
    Path configPath = new TransientTcConfig(node, userDirResolver, parameterSubstitutor).createTempTcConfigFile();
    startServer(
        "-r", nodeRepositoryDir.toString(),
        "--config-consistency",
        "--config", configPath.toAbsolutePath().toString(),
        "--node-name", nodeName
    );
  }

  void startPreactivated(Cluster cluster, Node node, String licenseFile, String optionalNodeRepositoryFromCLI) {
    String nodeName = node.getNodeName();
    logger.info("Starting node: {} in CONFIGURED state", nodeName);
    Path nodeRepositoryDir = getOrDefaultRepositoryDir(optionalNodeRepositoryFromCLI);
    NomadServerManager nomadServerManager = NomadBootstrapper.bootstrap(nodeRepositoryDir, nodeName, parameterSubstitutor);
    createConfigRepository(cluster, node, nomadServerManager, nodeRepositoryDir);
    TopologyService topologyService = registerTopologyService(new NodeContext(cluster, node), true, nomadServerManager);
    topologyService.installLicense(read(licenseFile));
    startServer(
        "-r", nodeRepositoryDir.toString(),
        "-n", nodeName,
        "--node-name", nodeName
    );
  }

  void startUsingConfigRepo(Path repositoryDir, String nodeName) {
    logger.info("Starting node: {} from config repository: {}", nodeName, parameterSubstitutor.substitute(repositoryDir));
    NomadServerManager nomadServerManager = NomadBootstrapper.bootstrap(repositoryDir, nodeName, parameterSubstitutor);
    NodeContext nodeContext = nomadServerManager.getConfiguration();
    nomadServerManager.upgradeForWrite(nodeContext.getStripeId(), nodeName);
    registerTopologyService(nodeContext, true, nomadServerManager);
    startServer(
        "-r", repositoryDir.toString(),
        "-n", nodeName,
        "--node-name", nodeName
    );
  }

  Node getMatchingNodeFromConfigFile(String specifiedHostName, String specifiedPort, String configFilePath, Cluster cluster) {
    requireNonNull(configFilePath);

    boolean isHostnameSpecified = specifiedHostName != null;
    boolean isPortSpecified = specifiedPort != null;

    String substitutedHost = parameterSubstitutor.substitute(isHostnameSpecified ? specifiedHostName : DEFAULT_HOSTNAME);
    int port = Integer.parseInt(isPortSpecified ? specifiedPort : DEFAULT_PORT);

    Collection<Node> allNodes = cluster.getNodes();
    Optional<Node> matchingNode = allNodes.stream()
        .filter(node1 -> substitutedHost.equals(node1.getNodeHostname()) && node1.getNodePort() == port)
        .findAny();

    Node node;
    // See if we find a match for a node based on the specified params. If not, we see if the config file contains just one node
    if (!isHostnameSpecified && !isPortSpecified && allNodes.size() == 1) {
      logger.info("Found only one node information in config file: {}", configFilePath);
      node = allNodes.iterator().next();
    } else if (matchingNode.isPresent()) {
      logger.info(errMsg(substitutedHost, configFilePath, port, "Found matching node entry"));
      node = matchingNode.get();
    } else {
      throw new IllegalArgumentException(errMsg(substitutedHost, configFilePath, port, "Did not find a matching node entry"));
    }
    return node;
  }

  private String errMsg(String hostname, String configFilePath, int port, String msgFragment) {
    return String.format(
        "%s in config file: %s based on %s=%s and %s=%d",
        msgFragment,
        parameterSubstitutor.substitute(configFilePath),
        NODE_HOSTNAME,
        hostname,
        NODE_PORT,
        port
    );
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

  private TopologyService registerTopologyService(NodeContext nodeContext, boolean clusterActivated, NomadServerManager nomadServerManager) {
    logger.info("Registering TopologyService with DiagnosticServices");
    TopologyService topologyService = new TopologyServiceImpl(nodeContext, clusterActivated, nomadServerManager);
    DiagnosticServices.register(TopologyService.class, topologyService);
    return topologyService;
  }

  private void createConfigRepository(Cluster cluster, Node node, NomadServerManager nomadServerManager, Path nodeRepositoryDir) {
    requireNonNull(nodeRepositoryDir);
    logger.debug("Creating node config repository at: {}", parameterSubstitutor.substitute(nodeRepositoryDir.toAbsolutePath()));

    nomadServerManager.upgradeForWrite(cluster.getStripeId(node).get(), node.getNodeName());
    logger.debug("Setting nomad writable successful");

    String nomadServerName = parameterSubstitutor.substitute(node.getNodeAddress().toString());
    NomadClient<NodeContext> nomadClient = new NomadClient<>(
        singleton(new NamedNomadServer<>(nomadServerName, nomadServerManager.getNomadServer())),
        node.getNodeHostname(),
        "SYSTEM"
    );
    NomadFailureRecorder<NodeContext> failureRecorder = new NomadFailureRecorder<>();
    nomadClient.tryApplyChange(failureRecorder, new ClusterActivationNomadChange(cluster));
    failureRecorder.reThrow();
    logger.debug("Nomad change run successful");
  }

  private String read(String path) {
    try {
      Path substitutedPath = Paths.get(parameterSubstitutor.substitute(path));
      return new String(Files.readAllBytes(substitutedPath), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
