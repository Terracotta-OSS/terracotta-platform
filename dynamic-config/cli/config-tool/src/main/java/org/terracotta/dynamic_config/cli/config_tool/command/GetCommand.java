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
import com.beust.jcommander.converters.BooleanConverter;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Configuration;
import org.terracotta.dynamic_config.api.model.Operation;
import org.terracotta.dynamic_config.cli.command.Usage;

import java.util.Map;
import java.util.TreeMap;

@Parameters(commandNames = "get", commandDescription = "Read configuration properties")
@Usage("get -s <hostname[:port]> [-r] -c <[namespace:]property>,<[namespace:]property>...")
public class GetCommand extends ConfigurationCommand {

  @Parameter(names = {"-r"}, description = "Read the properties from the current runtime configuration instead of reading them from the last configuration saved on disk", converter = BooleanConverter.class)
  private boolean wantsRuntimeConfig;

  public GetCommand() {
    super(Operation.GET);
  }

  @Override
  public void run() {
    Cluster cluster = wantsRuntimeConfig ? getRuntimeCluster(node) : getUpcomingCluster(node);
    Map<String, String> properties = new TreeMap<>();
    // we put both expanded and non expanded properties
    // and we will filter depending on what the user wanted
    cluster.toProperties(false, true, false).forEach((k, v) -> properties.put((String) k, (String) v));
    cluster.toProperties(true, true, false).forEach((k, v) -> properties.put((String) k, (String) v));

    // for each configuration asked by the user we try to find it
    for (Configuration configuration : configurations) {
      String output = properties.entrySet()
          .stream()
          .filter(e -> configuration.matchConfigPropertyKey(e.getKey()))
          .map(e -> e.getKey() + "=" + e.getValue())
          .reduce((result, line) -> result + System.lineSeparator() + line)
          .orElse(configuration + "=");
      logger.info(output);
    }
  }
}
