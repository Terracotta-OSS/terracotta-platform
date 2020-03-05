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
import com.beust.jcommander.ParameterException;
import org.terracotta.dynamic_config.api.model.Configuration;
import org.terracotta.dynamic_config.api.model.Operation;
import org.terracotta.dynamic_config.cli.converter.ConfigurationConverter;
import org.terracotta.dynamic_config.cli.converter.InetSocketAddressConverter;
import org.terracotta.dynamic_config.cli.converter.MultiConfigCommaSplitter;

import java.net.InetSocketAddress;
import java.util.List;

import static java.util.Objects.requireNonNull;

public abstract class ConfigurationCommand extends RemoteCommand {

  @Parameter(names = {"-s"}, description = "Node to connect to", required = true, converter = InetSocketAddressConverter.class)
  InetSocketAddress node;

  @Parameter(names = {"-c"}, description = "Configuration properties", splitter = MultiConfigCommaSplitter.class, required = true, converter = ConfigurationConverter.class)
  List<Configuration> configurations;

  protected final Operation operation;

  protected ConfigurationCommand(Operation operation) {
    this.operation = operation;
  }

  @Override
  public void validate() {
    requireNonNull(node);
    requireNonNull(configurations);

    // validate all configurations passes on CLI
    for (Configuration configuration : configurations) {
      configuration.validate(operation);
    }

    // once valid, check for duplicates
    for (int i = 0; i < configurations.size(); i++) {
      Configuration first = configurations.get(i);
      for (int j = i + 1; j < configurations.size(); j++) {
        Configuration second = configurations.get(j);
        if (second.duplicates(first)) {
          throw new ParameterException("Duplicate configurations found: " + first + " and " + second);
        }
      }
    }

    validateAddress(node);
  }
}
