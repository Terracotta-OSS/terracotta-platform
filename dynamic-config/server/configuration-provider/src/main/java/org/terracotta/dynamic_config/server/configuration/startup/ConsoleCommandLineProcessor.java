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
import org.terracotta.dynamic_config.api.service.ClusterFactory;
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;

import static java.util.Objects.requireNonNull;

public class ConsoleCommandLineProcessor implements CommandLineProcessor {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleCommandLineProcessor.class);

  private final Options options;
  private final ClusterFactory clusterCreator;
  private final ConfigurationGeneratorVisitor configurationGeneratorVisitor;
  private final IParameterSubstitutor parameterSubstitutor;

  ConsoleCommandLineProcessor(Options options,
                              ClusterFactory clusterCreator,
                              ConfigurationGeneratorVisitor configurationGeneratorVisitor,
                              IParameterSubstitutor parameterSubstitutor) {
    this.options = options;
    this.clusterCreator = clusterCreator;
    this.configurationGeneratorVisitor = configurationGeneratorVisitor;
    this.parameterSubstitutor = parameterSubstitutor;
  }

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  @Override
  public void process() {
    LOGGER.info("Starting node from command-line parameters");
    Cluster cluster = clusterCreator.create(options.getTopologyOptions(), parameterSubstitutor);
    Node node = cluster.getSingleNode().get(); // Cluster object will have only 1 node, just get that

    if (options.getLicenseFile() != null) {
      requireNonNull(cluster.getName(), "Cluster name is required with license file");
    }
    if (cluster.getName() != null) {
      configurationGeneratorVisitor.startActivated(new NodeContext(cluster, node.getNodeAddress()), options.getLicenseFile(), options.getNodeRepositoryDir());
    } else {
      configurationGeneratorVisitor.startUnconfigured(new NodeContext(cluster, node.getNodeAddress()), options.getNodeRepositoryDir());
    }
  }
}
