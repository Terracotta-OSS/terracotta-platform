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

import org.terracotta.dynamic_config.cli.command.Command;
import org.terracotta.dynamic_config.cli.config_tool.parsing.ActivateCommand;
import org.terracotta.dynamic_config.cli.config_tool.parsing.AttachCommand;
import org.terracotta.dynamic_config.cli.config_tool.parsing.DetachCommand;
import org.terracotta.dynamic_config.cli.config_tool.parsing.DiagnosticCommand;
import org.terracotta.dynamic_config.cli.config_tool.parsing.ExportCommand;
import org.terracotta.dynamic_config.cli.config_tool.parsing.GetCommand;
import org.terracotta.dynamic_config.cli.config_tool.parsing.ImportCommand;
import org.terracotta.dynamic_config.cli.config_tool.parsing.LockConfigCommand;
import org.terracotta.dynamic_config.cli.config_tool.parsing.LogCommand;
import org.terracotta.dynamic_config.cli.config_tool.parsing.RepairCommand;
import org.terracotta.dynamic_config.cli.config_tool.parsing.SetCommand;
import org.terracotta.dynamic_config.cli.config_tool.parsing.UnlockConfigCommand;
import org.terracotta.dynamic_config.cli.config_tool.parsing.UnsetCommand;
import org.terracotta.dynamic_config.cli.config_tool.parsing.deprecated.DeprecatedActivateCommand;
import org.terracotta.dynamic_config.cli.config_tool.parsing.deprecated.DeprecatedAttachCommand;
import org.terracotta.dynamic_config.cli.config_tool.parsing.deprecated.DeprecatedDetachCommand;
import org.terracotta.dynamic_config.cli.config_tool.parsing.deprecated.DeprecatedDiagnosticCommand;
import org.terracotta.dynamic_config.cli.config_tool.parsing.deprecated.DeprecatedExportCommand;
import org.terracotta.dynamic_config.cli.config_tool.parsing.deprecated.DeprecatedGetCommand;
import org.terracotta.dynamic_config.cli.config_tool.parsing.deprecated.DeprecatedImportCommand;
import org.terracotta.dynamic_config.cli.config_tool.parsing.deprecated.DeprecatedLockConfigCommand;
import org.terracotta.dynamic_config.cli.config_tool.parsing.deprecated.DeprecatedLogCommand;
import org.terracotta.dynamic_config.cli.config_tool.parsing.deprecated.DeprecatedRepairCommand;
import org.terracotta.dynamic_config.cli.config_tool.parsing.deprecated.DeprecatedSetCommand;
import org.terracotta.dynamic_config.cli.config_tool.parsing.deprecated.DeprecatedUnlockConfigCommand;
import org.terracotta.dynamic_config.cli.config_tool.parsing.deprecated.DeprecatedUnsetCommand;

import java.util.HashSet;
import java.util.Set;

public class OssCommandProvider implements CommandProvider {
  @Override
  public Set<Command> getCommands() {
    Set<Command> commands = new HashSet<>();
    commands.add(new ActivateCommand());
    commands.add(new DeprecatedActivateCommand());
    commands.add(new AttachCommand());
    commands.add(new DeprecatedAttachCommand());
    commands.add(new DetachCommand());
    commands.add(new DeprecatedDetachCommand());
    commands.add(new ImportCommand());
    commands.add(new DeprecatedImportCommand());
    commands.add(new ExportCommand());
    commands.add(new DeprecatedExportCommand());
    commands.add(new GetCommand());
    commands.add(new DeprecatedGetCommand());
    commands.add(new SetCommand());
    commands.add(new DeprecatedSetCommand());
    commands.add(new UnsetCommand());
    commands.add(new DeprecatedUnsetCommand());
    commands.add(new DiagnosticCommand());
    commands.add(new DeprecatedDiagnosticCommand());
    commands.add(new RepairCommand());
    commands.add(new DeprecatedRepairCommand());
    commands.add(new LogCommand());
    commands.add(new DeprecatedLogCommand());
    commands.add(new LockConfigCommand());
    commands.add(new DeprecatedLockConfigCommand());
    commands.add(new UnlockConfigCommand());
    commands.add(new DeprecatedUnlockConfigCommand());
    return commands;
  }
}
