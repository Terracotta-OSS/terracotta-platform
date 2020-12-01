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
import com.beust.jcommander.converters.PathConverter;
import org.terracotta.dynamic_config.cli.api.command.Command;
import org.terracotta.dynamic_config.cli.api.command.ExportCommand;
import org.terracotta.dynamic_config.cli.api.converter.OutputFormat;
import org.terracotta.dynamic_config.cli.command.JCommanderCommand;
import org.terracotta.dynamic_config.cli.command.Usage;
import org.terracotta.dynamic_config.cli.converter.FormatConverter;
import org.terracotta.dynamic_config.cli.converter.InetSocketAddressConverter;

import java.net.InetSocketAddress;
import java.nio.file.Path;

@Parameters(commandNames = "export", commandDescription = "Export a cluster configuration")
@Usage("export -connect-to <hostname[:port]> [-output-file <config-file>] [-include-defaults] [-runtime]")
public class ExportJCommanderCommand extends JCommanderCommand {

  @Parameter(names = {"-connect-to"}, required = true, description = "Node to connect to", converter = InetSocketAddressConverter.class)
  private InetSocketAddress node;

  @Parameter(names = {"-output-file"}, description = "Output configuration file", converter = PathConverter.class)
  private Path outputFile;

  @Parameter(names = {"-include-defaults"}, description = "Include default values. Default: false", converter = BooleanConverter.class)
  private boolean includeDefaultValues;

  @Parameter(names = {"-runtime"}, description = "Export the runtime configuration instead of the configuration saved on disk. Default: false", converter = BooleanConverter.class)
  private boolean wantsRuntimeConfig;

  @Parameter(names = {"-outputformat"}, hidden = true, description = "Output type (properties|json). Default: properties", converter = FormatConverter.class)
  private OutputFormat outputFormat = OutputFormat.PROPERTIES;

  private final ExportCommand underlying = new ExportCommand();

  @Override
  public void run() {
    underlying.setNode(node);
    underlying.setOutputFile(outputFile);
    underlying.setIncludeDefaultValues(includeDefaultValues);
    underlying.setWantsRuntimeConfig(wantsRuntimeConfig);
    underlying.setOutputFormat(outputFormat);

    underlying.run();
  }

  @Override
  public Command getCommand() {
    return underlying;
  }
}
