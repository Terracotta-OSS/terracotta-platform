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
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.PathConverter;
import org.terracotta.common.struct.Tuple2;
import org.terracotta.diagnostic.client.connection.DiagnosticServices;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.FailoverPriority;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.service.ClusterFactory;
import org.terracotta.dynamic_config.api.service.ClusterValidator;
import org.terracotta.dynamic_config.cli.command.Usage;
import org.terracotta.dynamic_config.cli.converter.InetSocketAddressConverter;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;

import static java.lang.System.lineSeparator;
import static org.terracotta.dynamic_config.api.model.FailoverPriority.consistency;

/**
 * @author Mathieu Carbou
 */
@Parameters(commandNames = "import", commandDescription = "Import a cluster configuration")
@Usage("import -f <config-file> [-s <hostname[:port]>]")
public class ImportCommand extends RemoteCommand {

  @Parameter(names = {"-s"}, description = "Node to connect to", converter = InetSocketAddressConverter.class)
  private InetSocketAddress node;

  @Parameter(names = {"-f"}, description = "Configuration file", required = true, converter = PathConverter.class)
  private Path configPropertiesFile;

  private Cluster cluster;
  private Collection<InetSocketAddress> runtimePeers;

  @Override
  public void validate() {
    cluster = loadCluster();
    FailoverPriority failoverPriority = cluster.getFailoverPriority();
    if (failoverPriority.equals(consistency())) {
      int voterCount = failoverPriority.getVoters();
      for (Stripe stripe : cluster.getStripes()) {
        int nodeCount = stripe.getNodes().size();
        int sum = voterCount + nodeCount;
        if (sum % 2 == 0) {
          logger.warn(lineSeparator() +
                  "===========================================================================================" + lineSeparator() +
                  "IMPORTANT: The sum (" + sum + ") of voter count (" + voterCount + ") and number of nodes (" + nodeCount + ") " +
                  "in stripe " + lineSeparator() + "'" + stripe.getName() + "' is an even "  + "number. " + lineSeparator() +
                  "An even-numbered configuration is more likely to experience split-brain situations." + lineSeparator() +
                  "===========================================================================================" + lineSeparator());
        }
      }
    }

    runtimePeers = cluster.getNodeAddresses();

    // validate the topology
    new ClusterValidator(cluster).validate();

    if (node != null) {
      // verify the activated state of the nodes
      if (areAllNodesActivated(runtimePeers)) {
        throw new IllegalStateException("Cluster is already activated");

      } else {
        if (!runtimePeers.contains(node)) {
          throw new IllegalStateException("Node: " + node + " is not in cluster: " + cluster.toShapeString());
        }
        if (isActivated(node)) {
          throw new IllegalStateException("Node is already activated");
        }
        runtimePeers = Collections.singletonList(node);
      }
    }
  }

  @Override
  public final void run() {
    logger.info("Importing cluster configuration from config file: {} to nodes: {}", configPropertiesFile, toString(runtimePeers));

    try (DiagnosticServices diagnosticServices = multiDiagnosticServiceProvider.fetchOnlineDiagnosticServices(runtimePeers)) {
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
