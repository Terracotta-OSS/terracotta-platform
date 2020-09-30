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
package org.terracotta.dynamic_config.cli.config_tool.parsing;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.terracotta.dynamic_config.cli.command.Command;
import org.terracotta.dynamic_config.cli.command.JCommanderCommand;
import org.terracotta.dynamic_config.cli.command.Usage;
import org.terracotta.dynamic_config.cli.config_tool.command.RepairCommand;
import org.terracotta.dynamic_config.cli.config_tool.converter.RepairAction;
import org.terracotta.dynamic_config.cli.converter.InetSocketAddressConverter;

import java.net.InetSocketAddress;

import static java.util.Objects.requireNonNull;

@Parameters(commandNames = "repair", commandDescription = "Repair a cluster configuration")
@Usage("repair -connect-to <hostname[:port]> [-force commit|rollback|reset|unlock]")
public class RepairJCommanderCommand extends JCommanderCommand {
  @Parameter(names = {"-connect-to"}, description = "Node to connect to", required = true, converter = InetSocketAddressConverter.class)
  InetSocketAddress node;

  @Parameter(names = {"-force"}, description = "Repair action to force: commit, rollback, reset, unlock", converter = RepairAction.RepairActionConverter.class)
  RepairAction forcedRepairAction;

  private final RepairCommand underlying = new RepairCommand();

  @Override
  public void validate() {
    requireNonNull(node);
    underlying.setNode(node);
    underlying.setForcedRepairAction(forcedRepairAction);
  }

  @Override
  public void run() {
    underlying.run();
  }

  @Override
  public boolean isDeprecated() {
    return false;
  }

  @Override
  public Command getCommand() {
    return underlying;
  }
}
