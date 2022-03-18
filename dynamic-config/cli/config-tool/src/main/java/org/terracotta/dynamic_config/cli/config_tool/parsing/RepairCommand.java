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
import org.terracotta.dynamic_config.cli.api.command.Injector.Inject;
import org.terracotta.dynamic_config.cli.api.command.RepairAction;
import org.terracotta.dynamic_config.cli.api.converter.RepairMethod;
import org.terracotta.dynamic_config.cli.command.Command;
import org.terracotta.dynamic_config.cli.command.Usage;
import org.terracotta.dynamic_config.cli.converter.InetSocketAddressConverter;
import org.terracotta.dynamic_config.cli.converter.RepairActionConverter;

import java.net.InetSocketAddress;

@Parameters(commandDescription = "Repair a cluster configuration")
@Usage("-connect-to <hostname[:port]> [-force commit|rollback|reset|unlock]")
public class RepairCommand extends Command {

  @Parameter(names = {"-connect-to"}, description = "Node to connect to", required = true, converter = InetSocketAddressConverter.class)
  InetSocketAddress node;

  @Parameter(names = {"-force"}, description = "Repair action to force: commit, rollback, reset, unlock", converter = RepairActionConverter.class)
  RepairMethod forcedRepairMethod;

  @Inject
  public final RepairAction action;

  public RepairCommand() {
    this(new RepairAction());
  }

  public RepairCommand(RepairAction action) {
    this.action = action;
  }

  @Override
  public void run() {
    action.setNode(node);
    action.setForcedRepairAction(forcedRepairMethod);

    action.run();
  }
}
