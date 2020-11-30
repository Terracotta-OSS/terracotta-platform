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

import org.terracotta.common.struct.Tuple2;
import org.terracotta.diagnostic.client.connection.DiagnosticServices;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.FailoverPriority;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.UID;
import org.terracotta.dynamic_config.api.service.ClusterFactory;
import org.terracotta.dynamic_config.api.service.ClusterValidator;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;

import static java.lang.System.lineSeparator;
import static org.terracotta.dynamic_config.api.model.FailoverPriority.Type.CONSISTENCY;

/**
 * @author Mathieu Carbou
 */
public class ImportCommand extends RemoteCommand {

  private InetSocketAddress node;
  private Path configPropertiesFile;

  public void setNode(InetSocketAddress node) {
    this.node = node;
  }

  public void setConfigPropertiesFile(Path configPropertiesFile) {
    this.configPropertiesFile = configPropertiesFile;
  }

  @Override
  public final void run() {
    Cluster cluster = loadCluster();
    FailoverPriority failoverPriority = cluster.getFailoverPriority();
    if (failoverPriority.getType() == CONSISTENCY) {
      int voterCount = failoverPriority.getVoters();
      for (Stripe stripe : cluster.getStripes()) {
        int nodeCount = stripe.getNodes().size();
        int sum = voterCount + nodeCount;
        if (sum % 2 == 0) {
          logger.warn(lineSeparator() +
              "=========================================================================================================" + lineSeparator() +
              "IMPORTANT: The sum (" + sum + ") of voter count (" + voterCount + ") and number of nodes " +
              "(" + nodeCount + ") in stripe " + stripe.getName() + lineSeparator() +
              "is an even number. An even-numbered configuration is more likely to experience split-brain situations." + lineSeparator() +
              "=========================================================================================================" + lineSeparator());
        }
      }
    }

    Collection<Node.Endpoint> runtimePeers = cluster.getEndpoints(node);

    // validate the topology
    new ClusterValidator(cluster).validate();

    if (node != null) {
      // verify the activated state of the nodes
      if (areAllNodesActivated(runtimePeers)) {
        throw new IllegalStateException("Cluster is already activated");

      } else {
        if (isActivated(node)) {
          throw new IllegalStateException("Node is already activated");
        }
        runtimePeers = Collections.singletonList(getEndpoint(node));
      }
    }
    logger.info("Importing cluster configuration from config file: {} to nodes: {}", configPropertiesFile, toString(runtimePeers));

    try (DiagnosticServices<UID> diagnosticServices = multiDiagnosticServiceProvider.fetchOnlineDiagnosticServices(endpointsToMap(runtimePeers))) {
      dynamicConfigServices(diagnosticServices)
          .map(Tuple2::getT2)
          .forEach(service -> service.setUpcomingCluster(cluster));
    }

    logger.info("Command successful!" + lineSeparator());
  }

  private Cluster loadCluster() {
    ClusterFactory clusterCreator = new ClusterFactory();
    Cluster cluster = clusterCreator.create(configPropertiesFile);
    logger.debug("Config property file parsed and cluster topology validation successful");
    return cluster;
  }
}
