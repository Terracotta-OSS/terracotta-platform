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
package org.terracotta.dynamic_config.cli.config_tool.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.PathConverter;
import org.terracotta.common.struct.Measure;
import org.terracotta.common.struct.TimeUnit;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.service.ClusterFactory;
import org.terracotta.dynamic_config.api.service.ClusterValidator;
import org.terracotta.dynamic_config.cli.command.Usage;
import org.terracotta.dynamic_config.cli.converter.InetSocketAddressConverter;
import org.terracotta.dynamic_config.cli.converter.TimeUnitConverter;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;

import static java.lang.System.lineSeparator;
import static java.util.Collections.singletonList;

@Parameters(commandNames = "activate", commandDescription = "Activate a cluster")
@Usage("activate (-s <hostname[:port]> | -f <config-file>) [-n <cluster-name>] [-R] [-l <license-file>] [-W <restart-wait-time>] [-D <restart-delay>]")
public class ActivateCommand extends RemoteCommand {

  @Parameter(names = {"-s"}, description = "Node to connect to", converter = InetSocketAddressConverter.class)
  private InetSocketAddress node;

  @Parameter(names = {"-f"}, description = "Configuration properties file containing nodes to be activated", converter = PathConverter.class)
  private Path configPropertiesFile;

  @Parameter(names = {"-n"}, description = "Cluster name")
  private String clusterName;

  @Parameter(names = {"-l"}, description = "License file", converter = PathConverter.class)
  private Path licenseFile;

  @Parameter(names = {"-W"}, description = "Maximum time to wait for the nodes to restart. Default: 60s", converter = TimeUnitConverter.class)
  private Measure<TimeUnit> restartWaitTime = Measure.of(60, TimeUnit.SECONDS);

  @Parameter(names = {"-D"}, description = "Delay before the server restarts itself. Default: 2s", converter = TimeUnitConverter.class)
  private Measure<TimeUnit> restartDelay = Measure.of(2, TimeUnit.SECONDS);

  @Parameter(names = {"-R"}, description = "Restrict the activation process to the node only")
  protected boolean restrictedActivation = false;

  private Cluster cluster;
  private Collection<InetSocketAddress> runtimePeers;

  @Override
  public void validate() {
    // basic validations first

    if (!restrictedActivation && node != null && configPropertiesFile != null) {
      throw new IllegalArgumentException("Either node or config properties file should be specified, not both");
    }

    if (restrictedActivation && node == null) {
      throw new IllegalArgumentException("A node must be supplied for a restricted activation");
    }

    if (licenseFile != null && !Files.exists(licenseFile)) {
      throw new ParameterException("License file not found: " + licenseFile);
    }

    if (node != null) {
      validateAddress(node);
    }

    // loading cluster from available sources, then validating

    cluster = loadTopologyFromConfig()
        .orElseGet(() -> loadTopologyFromNode()
            .orElseThrow(() -> new IllegalArgumentException("One of node or config properties file must be specified")));

    if (clusterName != null) {
      cluster.setName(clusterName);
    }

    if (cluster.getName() == null) {
      throw new IllegalArgumentException("Cluster name is missing");
    }

    if (node != null && !cluster.containsNode(node)) {
      throw new IllegalArgumentException("Node: " + node + " is not in cluster: " + cluster.toShapeString());
    }

    new ClusterValidator(cluster).validate();

    // getting the list of nodes where to push the same topology

    runtimePeers = restrictedActivation ?
        singletonList(node) : // if restrictive activation, we only activate the node supplied
        cluster.getNodeAddresses(); // if normal activation the nodes to activate are those found in the config file or in the topology loaded from the node

    // verify the activated state of the nodes
    if (areAllNodesActivated(runtimePeers)) {
      throw new IllegalStateException("Nodes are already activated: " + toString(runtimePeers));
    }
  }

  @Override
  public final void run() {
    activateNodes(runtimePeers, cluster, licenseFile, restartDelay, restartWaitTime);
    logger.info("Command successful!" + lineSeparator());
  }

  Cluster getCluster() {
    return cluster;
  }

  ActivateCommand setNode(InetSocketAddress node) {
    this.node = node;
    return this;
  }

  ActivateCommand setConfigPropertiesFile(Path configPropertiesFile) {
    this.configPropertiesFile = configPropertiesFile;
    return this;
  }

  ActivateCommand setClusterName(String clusterName) {
    this.clusterName = clusterName;
    return this;
  }

  private Optional<Cluster> loadTopologyFromConfig() {
    return Optional.ofNullable(configPropertiesFile).map(path -> {
      ClusterFactory clusterCreator = new ClusterFactory();
      Cluster cluster = clusterCreator.create(configPropertiesFile);
      logger.info("Cluster topology loaded and validated from configuration file: " + cluster.toShapeString());
      return cluster;
    });
  }

  private Optional<Cluster> loadTopologyFromNode() {
    return Optional.ofNullable(node).map(node -> {
      Cluster cluster = getUpcomingCluster(node);
      logger.debug("Cluster topology loaded from node: " + cluster.toShapeString());
      return cluster;
    });
  }
}
