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
import org.terracotta.dynamic_config.cli.config_tool.command.UnsetCommand;

import static java.util.Objects.requireNonNull;

@Parameters(commandNames = "unset", commandDescription = "Unset configuration properties")
@DeprecatedUsage("unset -s <hostname[:port]> -c <[namespace:]property>,<[namespace:]property>...")
public class DeprecatedUnsetJCommanderCommand extends DeprecatedConfigurationJCommanderCommand {

  private final UnsetCommand underlying = new UnsetCommand();

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
