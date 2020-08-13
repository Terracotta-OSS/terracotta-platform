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
package org.terracotta.dynamic_config.cli.config_tool;

import org.terracotta.dynamic_config.cli.command.Command;
import org.terracotta.dynamic_config.cli.config_tool.command.*;

import java.util.ArrayList;
import java.util.List;

public class OssCommandMapper implements CommandMapper {
  @Override
  public List<Command> getCommands() {
    List<Command> list = new ArrayList<>();
    list.add(new ActivateCommand());
    list.add(new AttachCommand());
    list.add(new DetachCommand());
    list.add(new ImportCommand());
    list.add(new GetCommand());
    list.add(new SetCommand());
    list.add(new UnsetCommand());
    list.add(new DiagnosticCommand());
    list.add(new RepairCommand());
    list.add(new LogCommand());
    return list;
  }
}
