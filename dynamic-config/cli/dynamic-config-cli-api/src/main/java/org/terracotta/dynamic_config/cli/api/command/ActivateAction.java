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
package org.terracotta.dynamic_config.cli.api.command;

import org.terracotta.common.struct.Measure;
import org.terracotta.common.struct.TimeUnit;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.ClusterState;
import org.terracotta.dynamic_config.api.model.Node.Endpoint;
import org.terracotta.dynamic_config.api.service.ClusterFactory;
import org.terracotta.dynamic_config.api.service.ClusterValidator;
import org.terracotta.dynamic_config.api.service.NameGenerator;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;

import static java.util.Collections.singletonList;

public class ActivateAction extends RemoteAction {

  private InetSocketAddress node;
  private Path configPropertiesFile;
  private String clusterName;
  private Path licenseFile;
  private Measure<TimeUnit> restartWaitTime = Measure.of(120, TimeUnit.SECONDS);
  private Measure<TimeUnit> restartDelay = Measure.of(2, TimeUnit.SECONDS);
  protected boolean restrictedActivation;

  public void setNode(InetSocketAddress node) {
    this.node = node;
  }

  public void setConfigPropertiesFile(Path configPropertiesFile) {
    this.configPropertiesFile = configPropertiesFile;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  public void setLicenseFile(Path licenseFile) {
    this.licenseFile = licenseFile;
  }

  public void setRestartWaitTime(Measure<TimeUnit> restartWaitTime) {
    this.restartWaitTime = restartWaitTime;
  }

  public void setRestartDelay(Measure<TimeUnit> restartDelay) {
    this.restartDelay = restartDelay;
  }

  public void setRestrictedActivation(boolean restrictedActivation) {
    this.restrictedActivation = restrictedActivation;
  }

  Cluster cluster;

  @Override
  public final void run() {
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

    new ClusterValidator(cluster).validate(ClusterState.ACTIVATED);

    // getting the list of nodes where to push the same topology

    Collection<Endpoint> runtimePeers = restrictedActivation ?
        singletonList(getEndpoint(node)) : // if restrictive activation, we only activate the node supplied
        cluster.getEndpoints(node); // if normal activation the nodes to activate are those found in the config file or in the topology loaded from the node

    // verify the activated state of the nodes
    if (areAllNodesActivated(runtimePeers)) {
      throw new IllegalStateException("Nodes are already activated: " + toString(runtimePeers));
    }
    if (!restrictedActivation) {
      NameGenerator.assignFriendlyNames(cluster);
    }
    activateNodes(runtimePeers, cluster, licenseFile, restartDelay, restartWaitTime);
    output.info("Command successful!");
  }

  private Optional<Cluster> loadTopologyFromConfig() {
    return Optional.ofNullable(configPropertiesFile).map(path -> {
      ClusterFactory clusterCreator = new ClusterFactory();
      Cluster cluster = clusterCreator.create(configPropertiesFile);
      output.info("Cluster topology loaded and validated from configuration file: " + cluster.toShapeString());
      return cluster;
    });
  }

  private Optional<Cluster> loadTopologyFromNode() {
    return Optional.ofNullable(node).map(node -> {
      Cluster cluster = getUpcomingCluster(node);
      output.info("Cluster topology loaded from node: " + cluster.toShapeString());
      return cluster;
    });
  }

  Cluster getCluster() {
    return cluster;
  }
}
