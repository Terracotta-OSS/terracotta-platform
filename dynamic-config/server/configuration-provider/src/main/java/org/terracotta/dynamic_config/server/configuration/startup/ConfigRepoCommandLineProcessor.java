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
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;

import java.nio.file.Path;
import java.util.Optional;

public class ConfigRepoCommandLineProcessor implements CommandLineProcessor {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigRepoCommandLineProcessor.class);

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
    Path repositoryDir = configurationGeneratorVisitor.getOrDefaultRepositoryDir(options.getNodeRepositoryDir());
    Optional<String> nodeName = configurationGeneratorVisitor.findNodeName(repositoryDir, parameterSubstitutor);
    if (nodeName.isPresent()) {
      configurationGeneratorVisitor.startUsingConfigRepo(repositoryDir, nodeName.get(), options.wantsRepairMode());
      return;
    }

    LOGGER.info("Did not find config repository at: " + parameterSubstitutor.substitute(repositoryDir));
    // Couldn't start node - pass the responsibility to the next starter
    nextStarter.process();
  }
}
