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
package org.terracotta.dynamic_config.cli.config_tool.parsing.deprecated;

import com.beust.jcommander.Parameters;
import org.terracotta.dynamic_config.cli.command.Command;
import org.terracotta.dynamic_config.cli.command.DeprecatedUsage;
import org.terracotta.dynamic_config.cli.config_tool.command.SetCommand;

import static java.util.Objects.requireNonNull;

@Parameters(commandNames = "set", commandDescription = "Set configuration properties")
@DeprecatedUsage("set -s <hostname[:port]> -c <[namespace:]property=value>,<[namespace:]property=value>...")
public class DeprecatedSetJCommanderCommand extends DeprecatedConfigurationJCommanderCommand {

  private final SetCommand underlying = new SetCommand();

  @Override
  public void validate() {
    requireNonNull(node);
    requireNonNull(configurations);
    underlying.setNode(node);
    underlying.setConfigurations(configurations);
  }

  @Override
  public void run() {
    underlying.run();
  }

  @Override
  public boolean isDeprecated() {
    return true;
  }

  @Override
  public Command getCommand() {
    return underlying;
  }
}
