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
import com.beust.jcommander.converters.BooleanConverter;
import org.terracotta.dynamic_config.cli.api.command.ConfigurationInput;
import org.terracotta.dynamic_config.cli.api.command.GetAction;
import org.terracotta.dynamic_config.cli.api.command.Injector.Inject;
import org.terracotta.dynamic_config.cli.api.converter.OutputFormatGet;
import org.terracotta.dynamic_config.cli.command.Command;
import org.terracotta.dynamic_config.cli.command.Usage;
import org.terracotta.dynamic_config.cli.converter.ConfigurationInputConverter;
import org.terracotta.dynamic_config.cli.converter.FormatGetConverter;
import org.terracotta.dynamic_config.cli.converter.InetSocketAddressConverter;
import org.terracotta.dynamic_config.cli.converter.MultiConfigCommaSplitter;

import java.net.InetSocketAddress;
import java.util.List;

@Parameters(commandDescription = "Read configuration properties")
@Usage("-connect-to <hostname[:port]> [-runtime] [-outputformat name|index] -setting <[namespace:]setting> -setting <[namespace:]setting> ...")
public class GetCommand extends Command {

  @Parameter(names = {"-connect-to"}, description = "Node to connect to", required = true, converter = InetSocketAddressConverter.class)
  InetSocketAddress node;

  @Parameter(names = {"-setting"}, description = "Configuration properties", splitter = MultiConfigCommaSplitter.class, required = true, converter = ConfigurationInputConverter.class)
  List<ConfigurationInput> inputs;

  @Parameter(names = {"-runtime"}, description = "Read the properties from the current runtime configuration instead of reading them from the last configuration saved on disk", converter = BooleanConverter.class)
  private boolean wantsRuntimeConfig;

  @Parameter(names = {"-outputformat"}, description = "Output Format (name|index). Default: name", converter = FormatGetConverter.class)
  private OutputFormatGet outputFormat = OutputFormatGet.NAME;

  @Inject
  public final GetAction action;

  public GetCommand() {
    this(new GetAction());
  }

  public GetCommand(GetAction action) {
    this.action = action;
  }

  @Override
  public void run() {
    action.setNode(node);
    action.setConfigurationInputs(inputs);
    action.setRuntimeConfig(wantsRuntimeConfig);
    action.setOutputFormat(outputFormat);

    action.run();
  }
}
