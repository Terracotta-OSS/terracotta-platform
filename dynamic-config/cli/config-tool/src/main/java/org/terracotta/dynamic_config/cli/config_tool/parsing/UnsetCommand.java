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
package org.terracotta.dynamic_config.cli.config_tool.parsing;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.terracotta.common.struct.Measure;
import org.terracotta.common.struct.TimeUnit;
import org.terracotta.dynamic_config.api.model.Configuration;
import org.terracotta.dynamic_config.cli.api.command.Injector.Inject;
import org.terracotta.dynamic_config.cli.api.command.UnsetAction;
import org.terracotta.dynamic_config.cli.command.Command;
import org.terracotta.dynamic_config.cli.command.Usage;
import org.terracotta.dynamic_config.cli.converter.ConfigurationConverter;
import org.terracotta.dynamic_config.cli.converter.InetSocketAddressConverter;
import org.terracotta.dynamic_config.cli.converter.MultiConfigCommaSplitter;
import org.terracotta.dynamic_config.cli.converter.TimeUnitConverter;

import java.net.InetSocketAddress;
import java.util.List;

@Parameters(commandDescription = "Unset configuration properties")
@Usage("-connect-to <hostname[:port]> -setting <[namespace:]property> -setting <[namespace:]property> ... [-auto-restart] [-restart-wait-time <restart-wait-time>] [-restart-delay <restart-delay>]")
public class UnsetCommand extends Command {

  @Parameter(names = {"-connect-to"}, description = "Node to connect to", required = true, converter = InetSocketAddressConverter.class)
  InetSocketAddress node;

  @Parameter(names = {"-setting"}, description = "Configuration properties", splitter = MultiConfigCommaSplitter.class, required = true, converter = ConfigurationConverter.class)
  List<Configuration> configurations;

  @Parameter(names = {"-auto-restart"}, description = "If a change requires some nodes to be restarted, the command will try to restart them if there are at least 2 nodes online per stripe.")
  boolean autoRestart = false;

  @Parameter(names = {"-restart-wait-time"}, description = "Maximum time to wait for the nodes to restart. Default: 120s", converter = TimeUnitConverter.class)
  Measure<TimeUnit> restartWaitTime = Measure.of(120, TimeUnit.SECONDS);

  @Parameter(names = {"-restart-delay"}, description = "Delay before the server restarts itself. Default: 2s", converter = TimeUnitConverter.class)
  Measure<TimeUnit> restartDelay = Measure.of(2, TimeUnit.SECONDS);

  @Inject
  public final UnsetAction action;

  public UnsetCommand() {
    this(new UnsetAction());
  }

  public UnsetCommand(UnsetAction action) {
    this.action = action;
  }

  @Override
  public void run() {
    action.setNode(node);
    action.setConfigurations(configurations);
    action.setAutoRestart(autoRestart);
    action.setRestartDelay(restartDelay);
    action.setRestartWaitTime(restartWaitTime);

    action.run();
  }
}
