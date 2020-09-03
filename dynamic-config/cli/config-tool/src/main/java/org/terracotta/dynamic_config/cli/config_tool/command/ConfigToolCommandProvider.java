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
import org.terracotta.dynamic_config.cli.command.CommandProvider;

import java.util.HashSet;
import java.util.Set;

public class ConfigToolCommandProvider implements CommandProvider {
  @Override
  public Set<Command> getCommands() {
    Set<Command> commands = new HashSet<>();
    commands.add(new ActivateCommand());
    commands.add(new AttachCommand());
    commands.add(new DetachCommand());
    commands.add(new ImportCommand());
    commands.add(new ExportCommand());
    commands.add(new GetCommand());
    commands.add(new SetCommand());
    commands.add(new UnsetCommand());
    commands.add(new DiagnosticCommand());
    commands.add(new RepairCommand());
    commands.add(new LogCommand());
    commands.add(new LockConfigCommand());
    commands.add(new UnlockConfigCommand());
    return commands;
  }
}
