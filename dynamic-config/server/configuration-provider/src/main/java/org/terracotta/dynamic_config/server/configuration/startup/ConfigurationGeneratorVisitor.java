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

import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.RawPath;
import org.terracotta.dynamic_config.api.model.Setting;
import org.terracotta.dynamic_config.api.model.Version;
import org.terracotta.dynamic_config.api.model.nomad.ClusterActivationNomadChange;
import org.terracotta.dynamic_config.api.service.DynamicConfigService;
import org.terracotta.dynamic_config.api.service.FormatUpgrade;
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.server.api.PathResolver;
import org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadConfigurationManager;
import org.terracotta.dynamic_config.server.configuration.service.NomadServerManager;
import org.terracotta.inet.InetSocketAddressUtils;
import org.terracotta.json.ObjectMapperFactory;
import org.terracotta.nomad.NomadEnvironment;
import org.terracotta.nomad.client.NomadClient;
import org.terracotta.nomad.client.NomadEndpoint;
import org.terracotta.nomad.client.results.NomadFailureReceiver;
import org.terracotta.server.Server;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.System.lineSeparator;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.terracotta.dynamic_config.api.model.SettingName.NODE_HOSTNAME;
import static org.terracotta.dynamic_config.api.model.SettingName.NODE_NAME;
import static org.terracotta.dynamic_config.api.model.SettingName.NODE_PORT;

public class ConfigurationGeneratorVisitor {
  private final IParameterSubstitutor parameterSubstitutor;
  private final NomadServerManager nomadServerManager;
  private final ClassLoader classLoader;
  private final PathResolver pathResolver;
  private final ObjectMapperFactory objectMapperFactory;
  private final Server server;

  private NodeContext nodeContext;
  private boolean repairMode;
  private boolean unConfiguredMode;

  public ConfigurationGeneratorVisitor(IParameterSubstitutor parameterSubstitutor,
                                       NomadServerManager nomadServerManager,
                                       ClassLoader classLoader,
                                       PathResolver pathResolver,
                                       ObjectMapperFactory objectMapperFactory,
                                       Server server) {
    this.parameterSubstitutor = requireNonNull(parameterSubstitutor);
    this.nomadServerManager = requireNonNull(nomadServerManager);
    this.classLoader = requireNonNull(classLoader);
    this.pathResolver = requireNonNull(pathResolver);
    this.objectMapperFactory = requireNonNull(objectMapperFactory);
    this.server = server;
  }

  public TopologyService getTopologyService() {
    return nomadServerManager.getTopologyService();
  }

  public StartupConfiguration generateConfiguration() {
    requireNonNull(nomadServerManager);
    requireNonNull(nodeContext);

    if (unConfiguredMode || repairMode) {
      // in diagnostic / unconfigured / repair mode, make sure we make platform think that the node is alone...
      // - the node won't be activated (Nomad 2 phase commit system won't be available)
      // - the diagnostic port will be available for the repair command to be able to rewrite the append log
      // - the config created will be stripped to make platform think this node is alone;
      return new StartupConfiguration(() -> nodeContext.alone(), unConfiguredMode, repairMode, classLoader, pathResolver, parameterSubstitutor, objectMapperFactory, server);
    } else {
      // configured mode
      return new StartupConfiguration(
          () -> nomadServerManager.getConfiguration()
              .orElseThrow(() -> new IllegalStateException("Node has not been activated or migrated properly: unable find any committed configuration to use at startup. Please delete the configuration directory and try again.")),
          unConfiguredMode, repairMode, classLoader, pathResolver, parameterSubstitutor, objectMapperFactory, server);
    }
  }

  void startUnconfigured(NodeContext nodeContext, String optionalNodeConfigurationDirFromCLI) {
    String nodeName = nodeContext.getNode().getName();
    server.console("Starting unconfigured node: {}", nodeName);
    Path nodeConfigurationDir = getOrDefaultConfigurationDirectory(optionalNodeConfigurationDirFromCLI);

    nomadServerManager.configure(nodeConfigurationDir, nodeContext);

    this.nodeContext = nodeContext;
    this.repairMode = false;
    this.unConfiguredMode = true;
  }

  void startActivated(NodeContext nodeContext, String optionalLicenseFile, String optionalNodeConfigurationDirectoryFromCLI) {
    String clusterName = nodeContext.getCluster().getName();
    if (clusterName == null) {
      throw new IllegalArgumentException("Cluster name is required to pre-activate a node");
    }

    if (nodeContext.getCluster().getStripeCount() > 1) {
      throw new UnsupportedOperationException("Cannot start a pre-activated multi-stripe cluster");
    }

    // IMPORTANT: UID
    // When we start with --auto-activate, the node can be started with a topology containing a stripe and several nodes.
    // If it is the case, the node will start, become either active or will try to join an existing active.
    // To be able to sync with this active, the topology (and consequently the UIDs) need to be generated equally
    // So this special case of starting a server will require to rewrite the generated UIDs when parsing the CLI or config file,
    // and this generation will be done with a controlled random.
    // Note: UIDs cannot be given from the CLI, they are system generated settings.

    nodeContext = nodeContext.withCluster(new FormatUpgrade().upgrade(nodeContext.getCluster(), Version.V1)).get();

    String nodeName = nodeContext.getNode().getName();
    server.console("Starting node: {} in cluster: {}", nodeName, clusterName);
    Path nodeConfigurationDir = getOrDefaultConfigurationDirectory(optionalNodeConfigurationDirectoryFromCLI);
    server.console("Creating node configuration directory at: {}", parameterSubstitutor.substitute(nodeConfigurationDir).toAbsolutePath());
    nomadServerManager.configure(nodeConfigurationDir, nodeContext);

    DynamicConfigService dynamicConfigService = nomadServerManager.getDynamicConfigService();
    dynamicConfigService.activate(nodeContext.getCluster(), optionalLicenseFile == null ? null : read(optionalLicenseFile));
    runNomadActivation(nodeContext.getCluster(), nodeContext.getNode(), nomadServerManager, nodeConfigurationDir);

    this.nodeContext = nodeContext;
    this.repairMode = false;
    this.unConfiguredMode = false;
  }

  void startUsingConfigRepo(Path nodeConfigurationDir, String nodeName, boolean repairMode, NodeContext alternate) {
    server.console("Starting node: {} from configuration directory: {}", nodeName, parameterSubstitutor.substitute(nodeConfigurationDir));
    nomadServerManager.reload(nodeConfigurationDir, nodeName, alternate);

    DynamicConfigService dynamicConfigService = nomadServerManager.getDynamicConfigService();
    TopologyService topologyService = nomadServerManager.getTopologyService();
    if (!repairMode) {
      // if not in repair mode, ensure we do not start a node with an empty or prepared activate change that has not been completed
      if (!nomadServerManager.getConfiguration().isPresent()) {
        throw new IllegalStateException("Node has not been activated or migrated properly: unable find any committed configuration to use at startup. Please delete the configuration directory and try again. Location: " + nodeConfigurationDir);
      }
      dynamicConfigService.activate(topologyService.getUpcomingNodeContext().getCluster(), dynamicConfigService.getLicenseContent().orElse(null));
    } else {
      // If repair mode mode is ON:
      // - the node won't be activated (Nomad 2 phase commit system won't be available)
      // - the diagnostic port will be available for the repair command to be able to rewrite the append log
      // - the TcConfig created will be stripped to make platform think this node is alone
      server.console(lineSeparator() + lineSeparator()
          + "=================================================================================================================" + lineSeparator()
          + "Node is starting in repair mode. This mode is used to manually repair a broken configuration on a node.      " + lineSeparator()
          + "No further configuration change can happen on the cluster while this node is in repair mode and not repaired." + lineSeparator()
          + "=================================================================================================================" + lineSeparator()
      );
    }

    this.nodeContext = topologyService.getRuntimeNodeContext();
    this.repairMode = repairMode;
    this.unConfiguredMode = false;

  }

  Node getMatchingNodeFromConfigFileUsingHostPort(String specifiedHostName, String specifiedPort, String configFilePath, Cluster cluster) {
    requireNonNull(configFilePath);

    boolean isHostnameSpecified = specifiedHostName != null;
    boolean isPortSpecified = specifiedPort != null;

    String substitutedHost = parameterSubstitutor.substitute(isHostnameSpecified ? specifiedHostName : Setting.NODE_HOSTNAME.getDefaultValue());
    int port = isPortSpecified ? Integer.parseInt(specifiedPort) : Setting.NODE_PORT.getDefaultValue();
    InetSocketAddress specifiedSockAddr = InetSocketAddress.createUnresolved(substitutedHost, port);

    Collection<Node> allNodes = cluster.getNodes();
    Optional<Node> matchingNode = allNodes.stream()
        .filter(node1 -> InetSocketAddressUtils.areEqual(node1.getInternalSocketAddress(), specifiedSockAddr))
        .findAny();

    HashMap<String, String> logParams = new HashMap<>();
    logParams.put(NODE_HOSTNAME, substitutedHost);
    logParams.put(NODE_PORT, String.valueOf(port));

    Node node;
    // See if we find a match for a node based on the specified logParams. If not, we see if the config file contains just one node
    if (!isHostnameSpecified && !isPortSpecified && allNodes.size() == 1) {
      server.console("Found only one node information in config file: {}", configFilePath);
      node = allNodes.iterator().next();
    } else if (matchingNode.isPresent()) {
      server.console(log("Found matching node entry", configFilePath, logParams));
      node = matchingNode.get();
    } else {
      throw new IllegalArgumentException(log("Did not find a matching node entry", configFilePath, logParams));
    }
    return node;
  }

  Node getMatchingNodeFromConfigFileUsingNodeName(String specifiedNodeName, String configFilePath, Cluster cluster) {
    requireNonNull(configFilePath);
    requireNonNull(specifiedNodeName);

    Collection<Node> allNodes = cluster.getNodes();
    List<Node> matchingNodes = allNodes.stream()
        .filter(node -> node.getName().equals(specifiedNodeName))
        .collect(Collectors.toList());

    HashMap<String, String> logParams = new HashMap<>();
    logParams.put(NODE_NAME, specifiedNodeName);
    if (matchingNodes.size() == 1) {
      server.console(log("Found matching node entry", configFilePath, logParams));
      return matchingNodes.get(0);
    } else if (matchingNodes.size() > 1) {
      throw new IllegalArgumentException(log("Found multiple matching node entries", configFilePath, logParams));
    } else {
      throw new IllegalArgumentException(log("Did not find a matching node entry", configFilePath, logParams));
    }
  }

  Path getOrDefaultConfigurationDirectory(String configPath) {
    configPath = parameterSubstitutor.substitute(configPath != null ? configPath : Setting.NODE_CONFIG_DIR.<RawPath>getDefaultValue().getValue());
    return pathResolver.resolve(Paths.get(configPath));
  }

  Optional<String> findNodeName(Path configPath, IParameterSubstitutor parameterSubstitutor) {
    return NomadConfigurationManager.findNodeName(configPath, parameterSubstitutor);
  }

  private String log(String msgFragment, String configFilePath, Map<String, String> params) {
    return String.format(
        "%s in config file: %s based on %s",
        msgFragment,
        parameterSubstitutor.substitute(configFilePath),
        params
    );
  }

  private void runNomadActivation(Cluster cluster, Node node, NomadServerManager nomadServerManager, Path nodeConfigurationDir) {
    requireNonNull(nodeConfigurationDir);
    NomadEnvironment environment = new NomadEnvironment();
    // Note: do NOT close this nomad client - it would close the server and sanskrit!
    NomadClient<NodeContext> nomadClient = new NomadClient<>(singletonList(new NomadEndpoint<>(node.getInternalSocketAddress(), nomadServerManager.getNomadServer())), environment.getHost(), environment.getUser(), Clock.systemUTC());
    NomadFailureReceiver<NodeContext> failureRecorder = new NomadFailureReceiver<>();
    nomadClient.tryApplyChange(failureRecorder, new ClusterActivationNomadChange(cluster));
    failureRecorder.reThrowErrors();
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
