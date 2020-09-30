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
import com.beust.jcommander.converters.PathConverter;
import org.terracotta.dynamic_config.cli.command.Command;
import org.terracotta.dynamic_config.cli.command.JCommanderCommand;
import org.terracotta.dynamic_config.cli.command.Usage;
import org.terracotta.dynamic_config.cli.config_tool.command.ImportCommand;
import org.terracotta.dynamic_config.cli.converter.InetSocketAddressConverter;

import java.net.InetSocketAddress;
import java.nio.file.Path;

@Parameters(commandNames = "import", commandDescription = "Import a cluster configuration")
@Usage("import -config-file <config-file> [-connect-to <hostname[:port]>]")
public class ImportJCommanderCommand extends JCommanderCommand {

  @Parameter(names = {"-connect-to"}, description = "Node to connect to", converter = InetSocketAddressConverter.class)
  private InetSocketAddress node;

  @Parameter(names = {"-config-file"}, description = "Config file", required = true, converter = PathConverter.class)
  private Path configFile;

  private final ImportCommand underlying = new ImportCommand();

  @Override
  public void validate() {
    underlying.setNode(node);
    underlying.setConfigPropertiesFile(configFile);
  }

  @Override
  public void run() {
    underlying.run();
  }

  @Override
  public Command getCommand() {
    return underlying;
  }

  @Override
  public boolean isDeprecated() {
    return false;
  }
}