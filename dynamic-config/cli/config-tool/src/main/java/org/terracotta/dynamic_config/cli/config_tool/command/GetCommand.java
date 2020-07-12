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
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.BooleanConverter;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Configuration;
import org.terracotta.dynamic_config.api.model.Operation;
import org.terracotta.dynamic_config.cli.command.DeprecatedParameter;
import org.terracotta.dynamic_config.cli.command.DeprecatedUsage;
import org.terracotta.dynamic_config.cli.command.Usage;

import java.util.Properties;
import java.util.stream.Collectors;

@Parameters(commandNames = "get", commandDescription = "Read configuration properties")
@DeprecatedUsage("get -s <hostname[:port]> [-r] -c <[namespace:]property>,<[namespace:]property>...")
@Usage("get -connect-to <hostname[:port]> [-runtime] -setting <[namespace:]setting>,<[namespace:]setting>...")
public class GetCommand extends ConfigurationCommand {

  @DeprecatedParameter(names = "-r", description = "Read the runtime configuration instead of the last configuration saved on disk", converter = BooleanConverter.class)
  @Parameter(names = "-runtime", description = "Read the runtime configuration instead of the last configuration saved on disk", converter = BooleanConverter.class)
  private boolean wantsRuntimeConfig;

  public GetCommand() {
    super(Operation.GET);
  }

  @Override
  public void run() {
    Cluster cluster = wantsRuntimeConfig ? getRuntimeCluster(node) : getUpcomingCluster(node);
    Properties properties = new Properties();
    // we put both expanded and non expanded properties
    // and we will filter depending on what the user wanted
    properties.putAll(cluster.toProperties(false, true));
    properties.putAll(cluster.toProperties(true, true));
    // we filter the properties the user wants based on his input
    String output = properties.entrySet()
        .stream()
        .filter(e -> acceptKey(e.getKey().toString()))
        .map(e -> e.getKey() + "=" + e.getValue())
        .sorted()
        .reduce((result, line) -> result + System.lineSeparator() + line)
        .orElseThrow(() -> new ParameterException("No configuration found for: " + configurations.stream().map(Configuration::toString).collect(Collectors.joining(", "))));
    logger.info(output);
  }

  private boolean acceptKey(String key) {
    return configurations.stream().anyMatch(configuration -> configuration.matchConfigPropertyKey(key));
  }
}
