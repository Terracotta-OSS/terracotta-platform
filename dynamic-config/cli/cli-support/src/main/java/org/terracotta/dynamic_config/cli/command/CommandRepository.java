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
package org.terracotta.dynamic_config.cli.command;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class CommandRepository {
  private final Map<String, Command> commandMap = new LinkedHashMap<>();

  public void addAll(Set<Command> commands) {
    commands.forEach(command -> commandMap.put(Metadata.getName(command), command));
  }

  public Collection<Command> getCommands() {
    return Collections.unmodifiableCollection(commandMap.values());
  }

  public Command getCommand(String name) {
    if (!commandMap.containsKey(name)) {
      throw new IllegalArgumentException("Command not found: " + name);
    }
    return commandMap.get(name);
  }

  public void inject(Object[] services) {
    commandMap.values().forEach(command -> Injector.inject(command, services));
  }

  public void inject(Collection<Object> services) {
    commandMap.values().forEach(command -> Injector.inject(command, services));
  }
}
