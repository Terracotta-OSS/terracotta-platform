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
import org.terracotta.dynamic_config.api.model.ConfigFormat;
import org.terracotta.dynamic_config.cli.api.command.ExportAction;
import org.terracotta.dynamic_config.cli.api.command.Injector.Inject;
import org.terracotta.dynamic_config.cli.command.Command;
import org.terracotta.dynamic_config.cli.command.Usage;
import org.terracotta.dynamic_config.cli.converter.ConfigFormatConverter;
import org.terracotta.dynamic_config.cli.converter.HostPortConverter;
import org.terracotta.inet.HostPort;

import java.nio.file.Path;

@Parameters(commandDescription = "Export a cluster configuration")
@Usage("-connect-to <hostname[:port]> [-output-file <config.cfg|config.properties>] [-include-defaults] [-runtime]")
public class ExportCommand extends Command {

  @Parameter(names = {"-connect-to"}, required = true, description = "Node to connect to", converter = HostPortConverter.class)
  private HostPort node;

  @Parameter(names = {"-output-file"}, description = "Output configuration file", converter = PathConverter.class)
  private Path outputFile;

  @Parameter(names = {"-include-defaults"}, description = "Include default values. Default: false", converter = BooleanConverter.class)
  private boolean includeDefaultValues;

  @Parameter(names = {"-runtime"}, description = "Export the runtime configuration instead of the configuration saved on disk. Default: false", converter = BooleanConverter.class)
  private boolean wantsRuntimeConfig;

  // NOTE: this parameter is hidden and only usable when we output to the console
  @Parameter(names = {"-output-format", "-outputformat"}, hidden = true, description = "Output format <cfg|properties>. Default: cfg", converter = ConfigFormatConverter.class)
  private ConfigFormat outputFormat = ConfigFormat.CONFIG;

  @Inject
  public ExportAction action;

  public ExportCommand() {
    this(new ExportAction());
  }

  public ExportCommand(ExportAction action) {
    this.action = action;
  }

  @Override
  public void run() {
    action.setNode(node);
    action.setOutputFile(outputFile);
    action.setIncludeDefaultValues(includeDefaultValues);
    action.setWantsRuntimeConfig(wantsRuntimeConfig);
    action.setOutputFormat(outputFormat);

    action.run();
  }
}
