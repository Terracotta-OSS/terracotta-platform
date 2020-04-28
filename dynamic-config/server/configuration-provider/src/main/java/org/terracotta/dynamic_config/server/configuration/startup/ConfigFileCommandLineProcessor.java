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

import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigFileCommandLineProcessor implements CommandLineProcessor {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigFileCommandLineProcessor.class);

  private final Options options;
  private final ClusterFactory clusterCreator;
  private final CommandLineProcessor nextStarter;
  private final ConfigurationGeneratorVisitor configurationGeneratorVisitor;
  private final IParameterSubstitutor parameterSubstitutor;

  ConfigFileCommandLineProcessor(CommandLineProcessor nextStarter, Options options, ClusterFactory clusterCreator, ConfigurationGeneratorVisitor configurationGeneratorVisitor, IParameterSubstitutor parameterSubstitutor) {
    this.options = options;
    this.clusterCreator = clusterCreator;
    this.nextStarter = nextStarter;
    this.configurationGeneratorVisitor = configurationGeneratorVisitor;
    this.parameterSubstitutor = parameterSubstitutor;
  }

  @Override
  public void process() {
    if (options.getConfigFile() == null) {
      // If config file wasn't specified - pass the responsibility to the next starter
      nextStarter.process();
      return;
    }

    Path substitutedConfigFile = Paths.get(parameterSubstitutor.substitute(options.getConfigFile()));
    LOGGER.info("Starting node from config file: {}", substitutedConfigFile);
    Cluster cluster = clusterCreator.create(substitutedConfigFile);

    Node node = configurationGeneratorVisitor.getMatchingNodeFromConfigFile(options.getNodeHostname(), options.getNodePort(), options.getConfigFile(), cluster);

    if (cluster.getName() != null) {
      if (cluster.getStripeCount() > 1) {
        throw new UnsupportedOperationException("Cannot start a pre-activated multi-stripe cluster");
      }
      configurationGeneratorVisitor.startActivated(new NodeContext(cluster, node.getNodeAddress()), options.getLicenseFile(), options.getNodeRepositoryDir());
    } else {
      configurationGeneratorVisitor.startUnconfigured(new NodeContext(cluster, node.getNodeAddress()), options.getNodeRepositoryDir());
    }
  }
}
