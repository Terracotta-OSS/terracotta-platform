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
import org.terracotta.common.struct.Measure;
import org.terracotta.common.struct.TimeUnit;
import org.terracotta.dynamic_config.cli.api.command.ConfigurationInput;
import org.terracotta.dynamic_config.cli.api.command.Injector.Inject;
import org.terracotta.dynamic_config.cli.api.command.SetAction;
import org.terracotta.dynamic_config.cli.command.Command;
import org.terracotta.dynamic_config.cli.command.Usage;
import org.terracotta.dynamic_config.cli.converter.ConfigurationInputConverter;
import org.terracotta.dynamic_config.cli.converter.InetSocketAddressConverter;
import org.terracotta.dynamic_config.cli.converter.MultiConfigCommaSplitter;
import org.terracotta.dynamic_config.cli.converter.TimeUnitConverter;

import java.net.InetSocketAddress;
import java.util.List;

@Parameters(commandDescription = "Set configuration properties")
@Usage("-s <hostname[:port]> -c <[namespace:]property=value> -c <[namespace:]property=value> ...")
public class DeprecatedSetCommand extends Command {

  @Parameter(names = {"-s"}, description = "Node to connect to", required = true, converter = InetSocketAddressConverter.class)
  InetSocketAddress node;

  @Parameter(names = {"-c"}, description = "Configuration properties", splitter = MultiConfigCommaSplitter.class, required = true, converter = ConfigurationInputConverter.class)
  List<ConfigurationInput> inputs;

  @Parameter(names = {"-W"}, description = "Maximum time to wait for the nodes to restart. Default: 120s", converter = TimeUnitConverter.class)
  protected Measure<TimeUnit> restartWaitTime = Measure.of(120, TimeUnit.SECONDS);

  @Parameter(names = {"-D"}, description = "Delay before the server restarts itself. Default: 2s", converter = TimeUnitConverter.class)
  protected Measure<TimeUnit> restartDelay = Measure.of(2, TimeUnit.SECONDS);

  @Inject
  public final SetAction action;

  public DeprecatedSetCommand() {
    this(new SetAction());
  }

  public DeprecatedSetCommand(SetAction action) {
    this.action = action;
  }

  @Override
  public void run() {
    action.setNode(node);
    action.setConfigurationInputs(inputs);
    action.setRestartDelay(restartDelay);
    action.setRestartWaitTime(restartWaitTime);

    action.run();
  }
}
