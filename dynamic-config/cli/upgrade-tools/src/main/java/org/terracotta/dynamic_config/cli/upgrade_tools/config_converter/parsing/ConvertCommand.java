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
package org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.parsing;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.PathConverter;
import org.terracotta.dynamic_config.cli.api.command.Injector.Inject;
import org.terracotta.dynamic_config.cli.command.Command;
import org.terracotta.dynamic_config.cli.command.Usage;
import org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.ConversionFormat;
import org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.command.ConvertAction;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.ConversionFormat.DIRECTORY;

@Parameters(commandDescription = "Convert tc-config files to configuration directory format")
@Usage("-tc-config <tc-config>,<tc-config>... [ -stripe-names <foo>,<bar>,<baz> ]  ( -format directory [-license <license-file>] -new-cluster-name <new-cluster-name> | -format properties [-new-cluster-name <new-cluster-name>]) [-destination <destination-dir>] [-force]")
public class ConvertCommand extends Command {

  @Parameter(names = {"-tc-config"}, required = true, description = "An ordered list of tc-config files", converter = PathConverter.class)
  private List<Path> tcConfigFiles;

  @Parameter(names = {"-stripe-names"}, required = false, description = "An ordered list of stripe names")
  private List<String> stripeNames;

  @Parameter(names = {"-license"}, description = "Path to license file", converter = PathConverter.class)
  private Path licensePath;

  @Parameter(names = {"-destination"}, description = "Destination directory to store converted config. Should not exist. Default: ${current-directory}/converted-configs", converter = PathConverter.class)
  private Path destinationDir = Paths.get(".").resolve("converted-configs");

  @Parameter(names = {"-new-cluster-name"}, description = "New cluster name")
  private String newClusterName;

  @Parameter(names = {"-format"}, description = "Conversion type (directory|properties). Default: directory", converter = ConversionFormat.FormatConverter.class)
  private ConversionFormat conversionFormat = DIRECTORY;

  @Parameter(names = {"-force"}, description = "Force a config conversion, ignoring warnings, if any")
  private boolean force;

  @Inject public final ConvertAction action;

  public ConvertCommand() {
    this(new ConvertAction());
  }

  public ConvertCommand(ConvertAction action) {
    this.action = action;
  }

  @Override
  public void run() {
    action.setTcConfigFiles(tcConfigFiles);
    action.setStripeNames(stripeNames);
    action.setLicensePath(licensePath);
    action.setDestinationDir(destinationDir);
    action.setNewClusterName(newClusterName);
    action.setConversionFormat(conversionFormat);
    action.setForce(force);

    action.run();
  }
}
