/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.server.startup;

import com.tc.server.TCServerMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Setting;
import org.terracotta.dynamic_config.api.model.nomad.ClusterActivationNomadChange;
import org.terracotta.dynamic_config.api.service.ConfigChangeHandlerManager;
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;
import org.terracotta.dynamic_config.api.service.PathResolver;
import org.terracotta.dynamic_config.server.nomad.NomadBootstrapper;
import org.terracotta.dynamic_config.server.nomad.repository.NomadRepositoryManager;
import org.terracotta.dynamic_config.server.service.DynamicConfigServiceImpl;
import org.terracotta.inet.InetSocketAddressUtils;
import org.terracotta.nomad.client.NomadClient;
import org.terracotta.nomad.client.NomadEndpoint;
import org.terracotta.nomad.client.results.NomadFailureReceiver;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static com.tc.util.Assert.assertNotNull;
import static java.lang.System.lineSeparator;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.terracotta.dynamic_config.api.model.SettingName.DIAGNOSTIC_MODE;
import static org.terracotta.dynamic_config.api.model.SettingName.NODE_NAME;
import static org.terracotta.dynamic_config.api.model.SettingName.NODE_REPOSITORY_DIR;
import static org.terracotta.dynamic_config.server.nomad.NomadBootstrapper.NomadServerManager;
import static org.terracotta.dynamic_config.server.nomad.NomadBootstrapper.bootstrap;

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
    Path temporaryTcConfigPath = new TransientTcConfig(node, userDirResolver, parameterSubstitutor).createTempTcConfigFile();
    return startServer(nodeRepositoryDir, nodeName, true, temporaryTcConfigPath);
  }

  boolean startActivated(Cluster cluster, Node node, String optionalLicenseFile, String optionalNodeRepositoryFromCLI) {
    String nodeName = node.getNodeName();
    logger.info("Starting node: {} in cluster: {}", nodeName, cluster.getName());
    Path nodeRepositoryDir = getOrDefaultRepositoryDir(optionalNodeRepositoryFromCLI);
    logger.debug("Creating node config repository at: {}", parameterSubstitutor.substitute(nodeRepositoryDir.toAbsolutePath()));
    NomadServerManager nomadServerManager = bootstrap(nodeRepositoryDir, parameterSubstitutor, changeHandlerManager, new NodeContext(cluster, node.getNodeAddress()));
    DynamicConfigServiceImpl dynamicConfigService = nomadServerManager.getDynamicConfigService();
    dynamicConfigService.activate(cluster, optionalLicenseFile == null ? null : read(optionalLicenseFile));
    runNomadChange(cluster, node, nomadServerManager, nodeRepositoryDir);
    return startServer(nodeRepositoryDir, nodeName, false, null);
  }

  boolean startUsingConfigRepo(Path nodeRepositoryDir, String nodeName, boolean diagnosticMode) {
    logger.info("Starting node: {} from config repository: {}", nodeName, parameterSubstitutor.substitute(nodeRepositoryDir));
    NomadServerManager nomadServerManager = bootstrap(nodeRepositoryDir, parameterSubstitutor, changeHandlerManager, nodeName);
    DynamicConfigServiceImpl dynamicConfigService = nomadServerManager.getDynamicConfigService();
    if (!diagnosticMode) {
      dynamicConfigService.activate();
    } else {
      // If diagnostic mode is ON:
      // - the node won't be activated (Nomad 2 phase commit system won't be available)
      // - the diagnostic port will be available for the repair command to be able to rewrite the append log
      // - the TcConfig created will be stripped to make platform think this node is alone
      logger.warn(lineSeparator() + lineSeparator()
          + "=================================================================================================================" + lineSeparator()
          + "Node is starting in diagnostic mode. This mode is used to manually repair a broken configuration on a node.      " + lineSeparator()
          + "No further configuration change can happen on the cluster while this node is in diagnostic mode and not repaired." + lineSeparator()
          + "=================================================================================================================" + lineSeparator()
      );
    }
    return startServer(nodeRepositoryDir, nodeName, diagnosticMode, null);
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

  private boolean startServer(Path nodeRepositoryDir, String nodeName, boolean diagnosticMode, Path temporaryTcConfigPath) {
    List<String> args = new ArrayList<>();

    // required by TCServerMain to identify the server
    args.add("-n");
    args.add(nodeName);

    // - the node repository directory where to create the files upon activation
    // - or the node repository directory that has been created when auto-activating on start
    // - or the node repository directory that already exists on disk
    args.add("--" + NODE_REPOSITORY_DIR);
    args.add(nodeRepositoryDir.toString());

    // the node name to identify the config repository file
    args.add("--" + NODE_NAME);
    args.add(nodeName);

    if (diagnosticMode) {
      args.add("--" + DIAGNOSTIC_MODE);
    }

    if (temporaryTcConfigPath != null) {
      args.add("--tc-config-file");
      args.add(temporaryTcConfigPath.toAbsolutePath().toString());
    }

    // Nomad system must have been bootstrapped BEFORE any call to TCServerMain
    assertNotNull(NomadBootstrapper.getNomadServerManager());

    TCServerMain.main(args.toArray(new String[0]));

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
