/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
import org.terracotta.dynamic_config.cli.api.command.ConfigurationInput;
import org.terracotta.dynamic_config.cli.api.command.Injector.Inject;
import org.terracotta.dynamic_config.cli.api.command.UnsetAction;
import org.terracotta.dynamic_config.cli.command.RestartCommand;
import org.terracotta.dynamic_config.cli.command.Usage;
import org.terracotta.dynamic_config.cli.converter.ConfigurationInputConverter;
import org.terracotta.dynamic_config.cli.converter.HostPortConverter;
import org.terracotta.dynamic_config.cli.converter.MultiConfigCommaSplitter;
import org.terracotta.inet.HostPort;

import java.util.Collections;
import java.util.List;

@Parameters(commandDescription = "Unset configuration properties")
@Usage("-connect-to <hostname[:port]> -setting <[namespace:]property> -setting <[namespace:]property> ... [-auto-restart] [-restart-wait-time <restart-wait-time>] [-restart-delay <restart-delay>]")
public class UnsetCommand extends RestartCommand {

  @Parameter(names = {"-connect-to"}, description = "Node to connect to", required = true, converter = HostPortConverter.class)
  List<HostPort> nodes = Collections.emptyList();

  @Parameter(names = {"-setting"}, description = "Configuration properties", splitter = MultiConfigCommaSplitter.class, required = true, converter = ConfigurationInputConverter.class)
  List<ConfigurationInput> inputs;

  @Parameter(names = {"-auto-restart"}, description = "If a change requires some nodes to be restarted, the command will try to restart them if there are at least 2 nodes online per stripe. Default: false")
  boolean autoRestart = false;

  @Parameter(names = {"-force"}, description = "Force the operation", hidden = true)
  protected boolean force;

  @Inject
  public UnsetAction action;

  public UnsetCommand() {
    this(new UnsetAction());
  }

  public UnsetCommand(UnsetAction action) {
    this.action = action;
  }

  @Override
  public void run() {
    action.setNodes(nodes);
    action.setConfigurationInputs(inputs);
    action.setAutoRestart(autoRestart);
    action.setRestartWaitTime(getRestartWaitTime());
    action.setRestartDelay(getRestartDelay());
    action.setForce(force);

    action.run();
  }
}
