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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.common.struct.Tuple2;
import org.terracotta.diagnostic.client.connection.DiagnosticServices;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.ClusterState;
import org.terracotta.dynamic_config.api.model.FailoverPriority;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.service.ClusterFactory;
import org.terracotta.dynamic_config.api.service.ClusterValidator;
import org.terracotta.inet.HostPort;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.toList;
import static org.terracotta.dynamic_config.api.model.FailoverPriority.Type.CONSISTENCY;

/**
 * @author Mathieu Carbou
 */
public class ImportAction extends RemoteAction {

  private static final Logger LOGGER = LoggerFactory.getLogger(ImportAction.class);

  private List<HostPort> nodes = Collections.emptyList();
  private Path configFile;

  public void setNodes(List<HostPort> nodes) {
    this.nodes = nodes;
  }

  public void setConfigFile(Path configFile) {
    this.configFile = configFile;
  }

  @Override
  public final void run() {
    Cluster cluster = loadCluster();
    FailoverPriority failoverPriority = cluster.getFailoverPriority().orElse(null);
    if (failoverPriority != null && failoverPriority.getType() == CONSISTENCY) {
      int voterCount = failoverPriority.getVoters();
      for (Stripe stripe : cluster.getStripes()) {
        int nodeCount = stripe.getNodes().size();
        int sum = voterCount + nodeCount;
        if (sum % 2 == 0) {
          LOGGER.warn(lineSeparator() +
              "=========================================================================================================" + lineSeparator() +
              "IMPORTANT: The sum (" + sum + ") of voter count (" + voterCount + ") and number of nodes " +
              "(" + nodeCount + ") in stripe " + stripe.getName() + lineSeparator() +
              "is an even number. An even-numbered configuration is more likely to experience split-brain situations." + lineSeparator() +
              "=========================================================================================================" + lineSeparator());
        }
      }
    }

    // validate the topology
    new ClusterValidator(cluster).validate(ClusterState.CONFIGURING);

    if (nodes.isEmpty()) {
      // import the cluster config to the nodes read from the config
      nodes = cluster.determineEndpoints().stream().map(Node.Endpoint::getHostPort).collect(toList());

    }

    for (HostPort node : nodes) {
      if (isActivated(node)) {
        throw new IllegalStateException("Node: " + node + " is already activated");
      }
    }

    output.info("Importing cluster configuration from config file: {} to nodes: {}", configFile, toString(nodes));

    try (DiagnosticServices<HostPort> diagnosticServices = multiDiagnosticServiceProvider.fetchOnlineDiagnosticServices(hostPortsToMap(nodes))) {
      dynamicConfigServices(diagnosticServices)
          .map(Tuple2::getT2)
          .forEach(service -> service.setUpcomingCluster(cluster));
    }

    output.info("Command successful!");
  }

  private Cluster loadCluster() {
    ClusterFactory clusterCreator = new ClusterFactory();
    Cluster cluster = clusterCreator.create(configFile);
    LOGGER.debug("Config property file parsed and cluster topology validation successful");
    return cluster;
  }
}
