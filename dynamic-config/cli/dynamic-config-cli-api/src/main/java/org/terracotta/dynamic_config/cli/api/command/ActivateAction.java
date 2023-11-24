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
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Node.Endpoint;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.service.ClusterFactory;
import org.terracotta.dynamic_config.api.service.ClusterValidator;
import org.terracotta.dynamic_config.api.service.NameGenerator;
import org.terracotta.dynamic_config.api.service.Props;
import org.terracotta.inet.HostPort;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import static java.util.stream.Collectors.toList;

public class ActivateAction extends RemoteAction {

  private List<HostPort> nodes = Collections.emptyList();
  private Path configPropertiesFile;
  private String clusterName;
  private Path licenseFile;
  private Measure<TimeUnit> restartWaitTime = Measure.of(120, TimeUnit.SECONDS);
  private Measure<TimeUnit> restartDelay = Measure.of(2, TimeUnit.SECONDS);
  private boolean restrictedActivation;
  private List<Map.Entry<Collection<HostPort>, String>> shape = Collections.emptyList();

  public void setNodes(List<HostPort> nodes) {
    this.nodes = nodes;
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

    if (shape.isEmpty()) {
      // loads the cluster from the config or remotely
      cluster = loadTopologyFromConfig()
          .orElseGet(() -> loadTopologyFromNode()
              .orElseThrow(() -> new IllegalArgumentException("One of node or config properties file must be specified")));

    } else {
      // create the cluster topology live by loading the node's settings and read the shape

      // 1. load the topology from a node and only keep the cluster-wide settings
      cluster = getRuntimeCluster(shape.get(0).getKey().iterator().next());
      cluster.setName(null); // fast activation requires '-cluster-name'
      cluster.removeStripes();
      final Properties clusterProperties = cluster.toProperties(false, false, false);

      // 2. reconstruct the cluster by fetching node's settings
      cluster.setStripes(shape.stream()
          .map(entry -> new Stripe()
              .setUID(cluster.newUID())
              .setName(entry.getValue()) // can be null
              .setNodes(entry.getKey()
                  .stream()
                  .map(hostPort -> {
                    final Cluster c = getRuntimeCluster(hostPort);
                    c.setName(null); // fast activation requires '-cluster-name'

                    // validate that no topology modifications were done on this done
                    if (c.getNodeCount() != 1) {
                      throw new IllegalArgumentException("Host: " + hostPort + " already contains a topology with 2 nodes or more so it cannot be used in a fast activation");
                    }

                    // extract the node and change its uid
                    final Node node = c.getSingleNode().get().setUID(cluster.newUID());

                    // validate that the node name does not exist
                    cluster.getNodeByName(node.getName()).ifPresent(existing -> {
                      throw new IllegalArgumentException("Host: " + hostPort + " points to a node having the same name of an other node: " + existing);
                    });

                    // validate that the cluster-wide settings are the same for this node
                    c.removeStripes();
                    final Properties props = c.toProperties(false, false, false);
                    if (!clusterProperties.equals(props)) {
                      throw new IllegalArgumentException("Host: " + hostPort + " has been started with cluster settings:\n" + Props.toString(props) + "\nBut we expected:\n" + Props.toString(clusterProperties));
                    }

                    // return it, it will be added to the stripe
                    return node;
                  })
                  .collect(toList())))
          .collect(toList()));
      // ensure our objects have names
      NameGenerator.assignFriendlyNames(cluster);
    }

    if (clusterName != null) {
      cluster.setName(clusterName);
    }

    if (cluster.getName() == null) {
      throw new IllegalArgumentException("Cluster name is missing");
    }

    new ClusterValidator(cluster).validate(ClusterState.ACTIVATED);

    // getting the list of nodes where to push the same topology

    Collection<Endpoint> runtimePeers = restrictedActivation ?
        nodes.stream().map(this::getEndpoint).collect(toList()) : // if restrictive activation, we only activate the node supplied
        cluster.determineEndpoints(nodes); // if normal activation the nodes to activate are those found in the config file or in the topology loaded from the node

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
    return nodes.stream().map(hostPort -> {
      Cluster cluster = getUpcomingCluster(hostPort);
      output.info("Cluster topology loaded from: " + hostPort + ": " + cluster.toShapeString());
      return cluster;
    }).findFirst();
  }

  Cluster getCluster() {
    return cluster;
  }

  public void setShape(List<Map.Entry<Collection<HostPort>, String>> shape) {
    this.shape = shape;
  }
}
