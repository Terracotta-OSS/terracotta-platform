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

import com.tc.util.ManagedServiceLoader;
import org.terracotta.dynamic_config.cli.api.command.Configuration;
import org.terracotta.dynamic_config.cli.command.Command;
import org.terracotta.dynamic_config.cli.config_tool.parsing.RemoteMainCommand;

import java.util.Collection;
import java.util.Map;

public interface CommandProvider {

  RemoteMainCommand getMainCommand(Configuration configuration);

  Map<String, Command> getCommands();

  Map<String, Command> getDeprecatedCommands();

  static CommandProvider get() {
    Collection<CommandProvider> services = ManagedServiceLoader.loadServices(CommandProvider.class, CommandProvider.class.getClassLoader());
    if (services.size() != 1) {
      throw new AssertionError("expected exactly one command provider, but found :" + services.size());
    }
    return services.iterator().next();
  }
}
