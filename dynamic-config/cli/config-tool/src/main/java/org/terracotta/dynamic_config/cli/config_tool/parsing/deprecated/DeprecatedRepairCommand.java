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

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.terracotta.dynamic_config.cli.api.command.Injector.Inject;
import org.terracotta.dynamic_config.cli.api.command.RepairAction;
import org.terracotta.dynamic_config.cli.api.converter.RepairMethod;
import org.terracotta.dynamic_config.cli.command.Command;
import org.terracotta.dynamic_config.cli.command.Usage;
import org.terracotta.dynamic_config.cli.converter.HostPortConverter;
import org.terracotta.dynamic_config.cli.converter.RepairActionConverter;
import org.terracotta.inet.HostPort;

@Parameters(commandDescription = "Repair a cluster configuration")
@Usage("-s <hostname[:port]> [-f commit|rollback|reset|unlock]")
public class DeprecatedRepairCommand extends Command {

  @Parameter(names = {"-s"}, description = "Node to connect to", required = true, converter = HostPortConverter.class)
  HostPort node;

  @Parameter(names = {"-f"}, description = "Repair action to force: commit, rollback, reset, unlock", converter = RepairActionConverter.class)
  RepairMethod forcedRepairMethod = RepairMethod.AUTO;

  @Inject
  public final RepairAction action;

  public DeprecatedRepairCommand() {
    this(new RepairAction());
  }

  public DeprecatedRepairCommand(RepairAction action) {
    this.action = action;
  }

  @Override
  public void run() {
    action.setNode(node);
    action.repairMethod(forcedRepairMethod);

    action.run();
  }
}
