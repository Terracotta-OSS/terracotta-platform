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
import org.terracotta.dynamic_config.cli.config_tool.parsing.RemoteMainCommand;
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

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.unmodifiableMap;

public class OssCommandProvider implements CommandProvider {

  @Override
  public RemoteMainCommand getMainCommand() {
    return new RemoteMainCommand();
  }

  @Override
  public Map<String, Command> getCommands() {
    Map<String, Command> commands = new HashMap<>();
    commands.put("activate", new ActivateCommand());
    commands.put("attach", new AttachCommand());
    commands.put("detach", new DetachCommand());
    commands.put("import", new ImportCommand());
    commands.put("export", new ExportCommand());
    commands.put("get", new GetCommand());
    commands.put("set", new SetCommand());
    commands.put("unset", new UnsetCommand());
    commands.put("diagnostic", new DiagnosticCommand());
    commands.put("repair", new RepairCommand());
    commands.put("log", new LogCommand());
    commands.put("lock-config", new LockConfigCommand());
    commands.put("unlock-config", new UnlockConfigCommand());
    return unmodifiableMap(commands);
  }

  @Override
  public Map<String, Command> getDeprecatedCommands() {
    Map<String, Command> commands = new HashMap<>();
    commands.put("activate", new DeprecatedActivateCommand());
    commands.put("attach", new DeprecatedAttachCommand());
    commands.put("detach", new DeprecatedDetachCommand());
    commands.put("import", new DeprecatedImportCommand());
    commands.put("export", new DeprecatedExportCommand());
    commands.put("get", new DeprecatedGetCommand());
    commands.put("set", new DeprecatedSetCommand());
    commands.put("unset", new DeprecatedUnsetCommand());
    commands.put("diagnostic", new DeprecatedDiagnosticCommand());
    commands.put("repair", new DeprecatedRepairCommand());
    commands.put("log", new DeprecatedLogCommand());
    commands.put("lock-config", new DeprecatedLockConfigCommand());
    commands.put("unlock-config", new DeprecatedUnlockConfigCommand());
    return unmodifiableMap(commands);
  }
}
