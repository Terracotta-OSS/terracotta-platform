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

import org.terracotta.dynamic_config.cli.command.JCommanderCommand;
import org.terracotta.dynamic_config.cli.config_tool.parsing.ActivateJCommanderCommand;
import org.terracotta.dynamic_config.cli.config_tool.parsing.AttachJCommanderCommand;
import org.terracotta.dynamic_config.cli.config_tool.parsing.DetachJCommanderCommand;
import org.terracotta.dynamic_config.cli.config_tool.parsing.DiagnosticJCommanderCommand;
import org.terracotta.dynamic_config.cli.config_tool.parsing.ExportJCommanderCommand;
import org.terracotta.dynamic_config.cli.config_tool.parsing.GetJCommanderCommand;
import org.terracotta.dynamic_config.cli.config_tool.parsing.ImportJCommanderCommand;
import org.terracotta.dynamic_config.cli.config_tool.parsing.LockConfigJCommanderCommand;
import org.terracotta.dynamic_config.cli.config_tool.parsing.LogJCommanderCommand;
import org.terracotta.dynamic_config.cli.config_tool.parsing.RepairJCommanderCommand;
import org.terracotta.dynamic_config.cli.config_tool.parsing.SetJCommanderCommand;
import org.terracotta.dynamic_config.cli.config_tool.parsing.UnlockConfigJCommanderCommand;
import org.terracotta.dynamic_config.cli.config_tool.parsing.UnsetJCommanderCommand;
import org.terracotta.dynamic_config.cli.config_tool.parsing.deprecated.DeprecatedActivateJCommanderCommand;
import org.terracotta.dynamic_config.cli.config_tool.parsing.deprecated.DeprecatedAttachJCommanderCommand;
import org.terracotta.dynamic_config.cli.config_tool.parsing.deprecated.DeprecatedDetachJCommanderCommand;
import org.terracotta.dynamic_config.cli.config_tool.parsing.deprecated.DeprecatedDiagnosticJCommanderCommand;
import org.terracotta.dynamic_config.cli.config_tool.parsing.deprecated.DeprecatedExportJCommanderCommand;
import org.terracotta.dynamic_config.cli.config_tool.parsing.deprecated.DeprecatedGetJCommanderCommand;
import org.terracotta.dynamic_config.cli.config_tool.parsing.deprecated.DeprecatedImportJCommanderCommand;
import org.terracotta.dynamic_config.cli.config_tool.parsing.deprecated.DeprecatedLockConfigJCommanderCommand;
import org.terracotta.dynamic_config.cli.config_tool.parsing.deprecated.DeprecatedLogJCommanderCommand;
import org.terracotta.dynamic_config.cli.config_tool.parsing.deprecated.DeprecatedRepairJCommanderCommand;
import org.terracotta.dynamic_config.cli.config_tool.parsing.deprecated.DeprecatedSetJCommanderCommand;
import org.terracotta.dynamic_config.cli.config_tool.parsing.deprecated.DeprecatedUnlockConfigJCommanderCommand;
import org.terracotta.dynamic_config.cli.config_tool.parsing.deprecated.DeprecatedUnsetJCommanderCommand;

import java.util.HashSet;
import java.util.Set;

public class OssCommandProvider implements JCommanderCommandProvider {
  @Override
  public Set<JCommanderCommand> getCommands() {
    Set<JCommanderCommand> commands = new HashSet<>();
    commands.add(new ActivateJCommanderCommand());
    commands.add(new DeprecatedActivateJCommanderCommand());
    commands.add(new AttachJCommanderCommand());
    commands.add(new DeprecatedAttachJCommanderCommand());
    commands.add(new DetachJCommanderCommand());
    commands.add(new DeprecatedDetachJCommanderCommand());
    commands.add(new ImportJCommanderCommand());
    commands.add(new DeprecatedImportJCommanderCommand());
    commands.add(new ExportJCommanderCommand());
    commands.add(new DeprecatedExportJCommanderCommand());
    commands.add(new GetJCommanderCommand());
    commands.add(new DeprecatedGetJCommanderCommand());
    commands.add(new SetJCommanderCommand());
    commands.add(new DeprecatedSetJCommanderCommand());
    commands.add(new UnsetJCommanderCommand());
    commands.add(new DeprecatedUnsetJCommanderCommand());
    commands.add(new DiagnosticJCommanderCommand());
    commands.add(new DeprecatedDiagnosticJCommanderCommand());
    commands.add(new RepairJCommanderCommand());
    commands.add(new DeprecatedRepairJCommanderCommand());
    commands.add(new LogJCommanderCommand());
    commands.add(new DeprecatedLogJCommanderCommand());
    commands.add(new LockConfigJCommanderCommand());
    commands.add(new DeprecatedLockConfigJCommanderCommand());
    commands.add(new UnlockConfigJCommanderCommand());
    commands.add(new DeprecatedUnlockConfigJCommanderCommand());
    return commands;
  }
}
