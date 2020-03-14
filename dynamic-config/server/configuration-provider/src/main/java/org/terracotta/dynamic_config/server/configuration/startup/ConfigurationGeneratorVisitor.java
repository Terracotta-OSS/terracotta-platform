/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.dynamic_config.server.configuration.startup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Setting;
import org.terracotta.dynamic_config.api.model.nomad.ClusterActivationNomadChange;
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;
import org.terracotta.dynamic_config.server.api.PathResolver;
import org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadRepositoryManager;
import org.terracotta.dynamic_config.server.configuration.service.DynamicConfigServiceImpl;
import org.terracotta.dynamic_config.server.configuration.service.NomadServerManager;
import org.terracotta.inet.InetSocketAddressUtils;
import org.terracotta.nomad.NomadEnvironment;
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
import java.util.Collection;
import java.util.Optional;

import static java.lang.System.lineSeparator;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

public class ConfigurationGeneratorVisitor {
  private static final Logger logger = LoggerFactory.getLogger(ConfigurationGeneratorVisitor.class);

  private final IParameterSubstitutor parameterSubstitutor;
  private final NomadServerManager nomadServerManager;
  private final ClassLoader classLoader;
  private final PathResolver pathResolver;

  private NodeContext nodeContext;
  private boolean diagnosticMode;
  private boolean unConfiguredMode;

  public ConfigurationGeneratorVisitor(IParameterSubstitutor parameterSubstitutor,
                                       NomadServerManager nomadServerManager,
                                       ClassLoader classLoader,
                                       PathResolver pathResolver) {
    this.parameterSubstitutor = requireNonNull(parameterSubstitutor);
    this.nomadServerManager = requireNonNull(nomadServerManager);
    this.classLoader = requireNonNull(classLoader);
    this.pathResolver = requireNonNull(pathResolver);
  }

  public boolean isUnConfiguredMode() {
    return unConfiguredMode;
  }

  public boolean isDiagnosticMode() {
    return diagnosticMode;
  }

  public DynamicConfigConfiguration generateConfiguration() {
    requireNonNull(nomadServerManager);
    requireNonNull(nodeContext);

    if (unConfiguredMode) {
      return new DynamicConfigConfiguration(nodeContext, true, classLoader, pathResolver, parameterSubstitutor);
    }

    NodeContext nodeContext = nomadServerManager.getConfiguration()
        .orElseThrow(() -> new IllegalStateException("Node has not been activated or migrated properly: unable find the latest committed configuration to use at startup. Please delete the repository folder and try again."));

    if (diagnosticMode) {
      // If diagnostic mode is ON:
      // - the node won't be activated (Nomad 2 phase commit system won't be available)
      // - the diagnostic port will be available for the repair command to be able to rewrite the append log
      // - the config created will be stripped to make platform think this node is alone;
      nodeContext = nodeContext.alone();
    }

    return new DynamicConfigConfiguration(nodeContext, diagnosticMode, classLoader, pathResolver, parameterSubstitutor);
  }

  void startUnconfigured(NodeContext nodeContext, String optionalNodeRepositoryFromCLI) {
    String nodeName = nodeContext.getNodeName();
    logger.info("Starting unconfigured node: {}", nodeName);
    Path nodeRepositoryDir = getOrDefaultRepositoryDir(optionalNodeRepositoryFromCLI);
    nomadServerManager.init(nodeRepositoryDir, nodeContext);

    this.nodeContext = nodeContext;
    this.diagnosticMode = false;
    this.unConfiguredMode = true;
  }

  void startActivated(NodeContext nodeContext, String optionalLicenseFile, String optionalNodeRepositoryFromCLI) {
    String nodeName = nodeContext.getNodeName();
    logger.info("Starting node: {} in cluster: {}", nodeName, nodeContext.getCluster().getName());
    Path nodeRepositoryDir = getOrDefaultRepositoryDir(optionalNodeRepositoryFromCLI);
    logger.debug("Creating node config repository at: {}", parameterSubstitutor.substitute(nodeRepositoryDir.toAbsolutePath()));
    nomadServerManager.init(nodeRepositoryDir, nodeContext);

    DynamicConfigServiceImpl dynamicConfigService = nomadServerManager.getDynamicConfigService();
    dynamicConfigService.activate(nodeContext.getCluster(), optionalLicenseFile == null ? null : read(optionalLicenseFile));
    runNomadActivation(nodeContext.getCluster(), nodeContext.getNode(), nomadServerManager, nodeRepositoryDir);

    this.nodeContext = nodeContext;
    this.diagnosticMode = false;
    this.unConfiguredMode = false;
  }

  void startUsingConfigRepo(Path nodeRepositoryDir, String nodeName, boolean diagnosticMode) {
    logger.info("Starting node: {} from config repository: {}", nodeName, parameterSubstitutor.substitute(nodeRepositoryDir));
    nomadServerManager.init(nodeRepositoryDir, nodeName);

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

    this.nodeContext = dynamicConfigService.getRuntimeNodeContext();
    this.diagnosticMode = diagnosticMode;
    this.unConfiguredMode = false;

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

  Optional<String> findNodeName(Path repositoryDir, IParameterSubstitutor parameterSubstitutor) {
    return NomadRepositoryManager.findNodeName(repositoryDir, parameterSubstitutor);
  }

  private void runNomadActivation(Cluster cluster, Node node, NomadServerManager nomadServerManager, Path nodeRepositoryDir) {
    requireNonNull(nodeRepositoryDir);
    NomadEnvironment environment = new NomadEnvironment();
    NomadClient<NodeContext> nomadClient = new NomadClient<>(singletonList(new NomadEndpoint<>(node.getNodeAddress(), nomadServerManager.getNomadServer())), environment.getHost(), environment.getUser(), Clock.systemUTC());
    NomadFailureReceiver<NodeContext> failureRecorder = new NomadFailureReceiver<>();
    nomadClient.tryApplyChange(failureRecorder, new ClusterActivationNomadChange(cluster));
    failureRecorder.reThrow();
    logger.debug("Nomad activation run successful");
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
