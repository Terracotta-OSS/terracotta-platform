/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.server.startup;

import com.tc.server.TCServerMain;
import com.terracottatech.dynamic_config.api.model.Cluster;
import com.terracottatech.dynamic_config.api.model.Node;
import com.terracottatech.dynamic_config.api.model.NodeContext;
import com.terracottatech.dynamic_config.api.model.Setting;
import com.terracottatech.dynamic_config.api.model.nomad.ClusterActivationNomadChange;
import com.terracottatech.dynamic_config.api.service.ConfigChangeHandlerManager;
import com.terracottatech.dynamic_config.api.service.IParameterSubstitutor;
import com.terracottatech.dynamic_config.api.service.PathResolver;
import com.terracottatech.dynamic_config.server.nomad.repository.NomadRepositoryManager;
import com.terracottatech.dynamic_config.server.service.DynamicConfigServiceImpl;
import com.terracottatech.ipv6.InetSocketAddressUtils;
import com.terracottatech.nomad.client.NomadClient;
import com.terracottatech.nomad.client.NomadEndpoint;
import com.terracottatech.nomad.client.results.NomadFailureReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.util.Collection;
import java.util.Optional;

import static com.terracottatech.dynamic_config.server.nomad.NomadBootstrapper.NomadServerManager;
import static com.terracottatech.dynamic_config.server.nomad.NomadBootstrapper.bootstrap;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

public class StartupManager {
  private static final Logger logger = LoggerFactory.getLogger(StartupManager.class);

  private final IParameterSubstitutor parameterSubstitutor;
  private final ConfigChangeHandlerManager changeHandlerManager;

  public StartupManager(IParameterSubstitutor parameterSubstitutor, ConfigChangeHandlerManager changeHandlerManager) {
    this.parameterSubstitutor = parameterSubstitutor;
    this.changeHandlerManager = changeHandlerManager;
  }

  boolean startUnconfigured(Cluster cluster, Node node, String optionalNodeRepositoryFromCLI) {
    String nodeName = node.getNodeName();
    logger.info("Starting unconfigured node: {}", nodeName);
    Path nodeRepositoryDir = getOrDefaultRepositoryDir(optionalNodeRepositoryFromCLI);
    bootstrap(nodeRepositoryDir, parameterSubstitutor, changeHandlerManager, new NodeContext(cluster, node.getNodeAddress()));
    // This resolver will make sure to rebase the relative given path only if the given path is not absolute
    // and this check happens after doing the substitution.
    // Note: the returned resolved path is not substituted and contains placeholders from both base directory and given path.
    PathResolver userDirResolver = new PathResolver(Paths.get("%(user.dir)"), parameterSubstitutor::substitute);
    Path configPath = new TransientTcConfig(node, userDirResolver, parameterSubstitutor).createTempTcConfigFile();
    return startServer(
        "-r", nodeRepositoryDir.toString(),
        "--config-consistency",
        "--config", configPath.toAbsolutePath().toString(),
        "--node-name", nodeName
    );
  }

  boolean startActivated(Cluster cluster, Node node, String licenseFile, String optionalNodeRepositoryFromCLI) {
    String nodeName = node.getNodeName();
    logger.info("Starting node: {} in cluster: {}", nodeName, cluster.getName());
    Path nodeRepositoryDir = getOrDefaultRepositoryDir(optionalNodeRepositoryFromCLI);
    logger.debug("Creating node config repository at: {}", parameterSubstitutor.substitute(nodeRepositoryDir.toAbsolutePath()));
    NomadServerManager nomadServerManager = bootstrap(nodeRepositoryDir, parameterSubstitutor, changeHandlerManager, new NodeContext(cluster, node.getNodeAddress()));
    DynamicConfigServiceImpl dynamicConfigService = nomadServerManager.getDynamicConfigService();
    dynamicConfigService.prepareActivation(cluster, read(licenseFile));
    runNomadChange(cluster, node, nomadServerManager, nodeRepositoryDir);
    return startServer(
        "-r", nodeRepositoryDir.toString(),
        "-n", nodeName,
        "--node-name", nodeName
    );
  }

  boolean startUsingConfigRepo(Path repositoryDir, String nodeName) {
    logger.info("Starting node: {} from config repository: {}", nodeName, parameterSubstitutor.substitute(repositoryDir));
    NomadServerManager nomadServerManager = bootstrap(repositoryDir, parameterSubstitutor, changeHandlerManager, nodeName);
    DynamicConfigServiceImpl dynamicConfigService = nomadServerManager.getDynamicConfigService();
    dynamicConfigService.activate();
    return startServer(
        "-r", repositoryDir.toString(),
        "-n", nodeName,
        "--node-name", nodeName
    );
  }

  Node getMatchingNodeFromConfigFile(String specifiedHostName, String specifiedPort, String configFilePath, Cluster cluster) {
    requireNonNull(configFilePath);

    boolean isHostnameSpecified = specifiedHostName != null;
    boolean isPortSpecified = specifiedPort != null;

    String substitutedHost = parameterSubstitutor.substitute(isHostnameSpecified ? specifiedHostName : Setting.NODE_HOSTNAME.getDefaultValue());
    int port = Integer.parseInt(isPortSpecified ? specifiedPort : Setting.NODE_PORT.getDefaultValue());
    InetSocketAddress specifiedSockAddr = InetSocketAddress.createUnresolved(substitutedHost, port);

    Collection<Node> allNodes = cluster.getNodes();
    Optional<Node> matchingNode = allNodes.stream()
        .filter(node1 -> InetSocketAddressUtils.areEqual(node1.getNodeInternalAddress(), specifiedSockAddr))
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
        Setting.NODE_HOSTNAME,
        hostname,
        Setting.NODE_PORT,
        port
    );
  }

  Path getOrDefaultRepositoryDir(String repositoryDir) {
    return Paths.get(repositoryDir != null ? repositoryDir : Setting.NODE_REPOSITORY_DIR.getDefaultValue());
  }

  private boolean startServer(String... args) {
    TCServerMain.main(args);
    return true;
  }

  Optional<String> findNodeName(Path repositoryDir) {
    return NomadRepositoryManager.findNodeName(repositoryDir);
  }

  private void runNomadChange(Cluster cluster, Node node, NomadServerManager nomadServerManager, Path nodeRepositoryDir) {
    requireNonNull(nodeRepositoryDir);
    NomadClient<NodeContext> nomadClient = new NomadClient<>(singletonList(new NomadEndpoint<>(node.getNodeAddress(), nomadServerManager.getNomadServer())), node.getNodeHostname(), "SYSTEM", Clock.systemUTC());
    NomadFailureReceiver<NodeContext> failureRecorder = new NomadFailureReceiver<>();
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
