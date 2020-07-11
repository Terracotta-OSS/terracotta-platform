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

import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;
import org.terracotta.server.ServerEnv;

import java.nio.file.Path;
import java.util.Optional;

public class ConfigRepoCommandLineProcessor implements CommandLineProcessor {
  private final Options options;
  private final CommandLineProcessor nextStarter;
  private final ConfigurationGeneratorVisitor configurationGeneratorVisitor;
  private final IParameterSubstitutor parameterSubstitutor;

  ConfigRepoCommandLineProcessor(CommandLineProcessor nextStarter, Options options, ConfigurationGeneratorVisitor configurationGeneratorVisitor, IParameterSubstitutor parameterSubstitutor) {
    this.options = options;
    this.nextStarter = nextStarter;
    this.configurationGeneratorVisitor = configurationGeneratorVisitor;
    this.parameterSubstitutor = parameterSubstitutor;
  }

  @Override
  public void process() {
    Path configPath = configurationGeneratorVisitor.getOrDefaultConfigurationDirectory(options.getNodeConfigDir());
    Optional<String> nodeName = configurationGeneratorVisitor.findNodeName(configPath, parameterSubstitutor);
    if (nodeName.isPresent()) {
      ServerEnv.getServer().console("Found configuration directory at: {}. Other parameters will be ignored",
          parameterSubstitutor.substitute(configPath));
      configurationGeneratorVisitor.startUsingConfigRepo(configPath, nodeName.get(), options.wantsRepairMode());
      return;
    }

    // Couldn't start node - pass the responsibility to the next starter
    ServerEnv.getServer().console("Did not find configuration directory at: {}", parameterSubstitutor.substitute(configPath));
    nextStarter.process();
  }
}
