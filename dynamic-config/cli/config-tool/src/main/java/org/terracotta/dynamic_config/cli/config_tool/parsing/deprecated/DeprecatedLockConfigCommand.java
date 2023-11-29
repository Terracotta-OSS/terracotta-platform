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
import org.terracotta.dynamic_config.cli.api.command.LockConfigAction;
import org.terracotta.dynamic_config.cli.command.Command;
import org.terracotta.dynamic_config.cli.command.Usage;
import org.terracotta.dynamic_config.cli.converter.HostPortConverter;
import org.terracotta.inet.HostPort;

@Parameters(commandDescription = "Locks the config", hidden = true)
@Usage("-s <hostname[:port]> --lock-context <context>")
public class DeprecatedLockConfigCommand extends Command {
  @Parameter(names = {"-s"}, description = "Node to connect to", required = true, converter = HostPortConverter.class)
  HostPort node;

  @Parameter(names = {"--lock-context"}, description = "Lock context", required = true)
  String lockContext;

  @Inject
  public LockConfigAction action;

  public DeprecatedLockConfigCommand() {
    this(new LockConfigAction());
  }

  public DeprecatedLockConfigCommand(LockConfigAction action) {
    this.action = action;
  }

  @Override
  public void run() {
    action.setNode(node);
    action.setLockContext(lockContext);

    action.run();
  }
}
