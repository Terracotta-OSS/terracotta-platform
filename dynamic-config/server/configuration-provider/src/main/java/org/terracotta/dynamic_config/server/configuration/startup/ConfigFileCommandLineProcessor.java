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
import org.terracotta.dynamic_config.api.service.ClusterFactory;
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;
import org.terracotta.server.Server;

import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigFileCommandLineProcessor implements CommandLineProcessor {
  private final Options options;
  private final ClusterFactory clusterCreator;
  private final CommandLineProcessor nextStarter;
  private final ConfigurationGeneratorVisitor configurationGeneratorVisitor;
  private final IParameterSubstitutor parameterSubstitutor;
  private final Server server;

  ConfigFileCommandLineProcessor(CommandLineProcessor nextStarter,
                                 Options options,
                                 ClusterFactory clusterCreator,
                                 ConfigurationGeneratorVisitor configurationGeneratorVisitor,
                                 IParameterSubstitutor parameterSubstitutor,
                                 Server server) {
    this.options = options;
    this.clusterCreator = clusterCreator;
    this.nextStarter = nextStarter;
    this.configurationGeneratorVisitor = configurationGeneratorVisitor;
    this.parameterSubstitutor = parameterSubstitutor;
    this.server = server;
  }

  @Override
  public void process() {
    if (options.getConfigFile() == null) {
      // If config file wasn't specified - pass the responsibility to the next starter
      nextStarter.process();
      return;
    }

    Path substitutedConfigFile = Paths.get(parameterSubstitutor.substitute(options.getConfigFile()));
    server.console("Starting node from config file: {}", substitutedConfigFile);
    Cluster cluster = clusterCreator.create(substitutedConfigFile);

    Node node;
    if (options.getNodeName() != null) {
      node = configurationGeneratorVisitor.getMatchingNodeFromConfigFileUsingNodeName(options.getNodeName(), options.getConfigFile(), cluster);
    } else {
      node = configurationGeneratorVisitor.getMatchingNodeFromConfigFileUsingHostPort(options.getHostname(), options.getPort(), options.getConfigFile(), cluster);
    }

    if (options.allowsAutoActivation()) {
      configurationGeneratorVisitor.startActivated(new NodeContext(cluster, node.getUID()), options.getLicenseFile(), options.getConfigDir());
    } else {
      configurationGeneratorVisitor.startUnconfigured(new NodeContext(cluster, node.getUID()), options.getConfigDir());
    }
  }
}
