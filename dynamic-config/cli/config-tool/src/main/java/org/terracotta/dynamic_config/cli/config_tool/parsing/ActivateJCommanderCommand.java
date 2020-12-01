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
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.PathConverter;
import org.terracotta.common.struct.Measure;
import org.terracotta.common.struct.TimeUnit;
import org.terracotta.dynamic_config.cli.api.command.ActivateCommand;
import org.terracotta.dynamic_config.cli.api.command.Command;
import org.terracotta.dynamic_config.cli.command.JCommanderCommand;
import org.terracotta.dynamic_config.cli.command.Usage;
import org.terracotta.dynamic_config.cli.converter.InetSocketAddressConverter;
import org.terracotta.dynamic_config.cli.converter.TimeUnitConverter;

import java.net.InetSocketAddress;
import java.nio.file.Path;

@Parameters(commandNames = "activate", commandDescription = "Activate a cluster")
@Usage("activate (-connect-to <hostname[:port]> | -config-file <config-file>) [-cluster-name <cluster-name>] [-restrict] [-license-file <license-file>] [-restart-wait-time <restart-wait-time>] [-restart-delay <restart-delay>]")
public class ActivateJCommanderCommand extends JCommanderCommand {

  @Parameter(names = {"-connect-to"}, description = "Node to connect to", converter = InetSocketAddressConverter.class)
  private InetSocketAddress node;

  @Parameter(names = {"-config-file"}, description = "Configuration properties file containing nodes to be activated", converter = PathConverter.class)
  private Path configPropertiesFile;

  @Parameter(names = {"-cluster-name"}, description = "Cluster name")
  private String clusterName;

  @Parameter(names = {"-license-file"}, description = "License file", converter = PathConverter.class)
  private Path licenseFile;

  @Parameter(names = {"-restart-wait-time"}, description = "Maximum time to wait for the nodes to restart. Default: 120s", converter = TimeUnitConverter.class)
  private Measure<TimeUnit> restartWaitTime = Measure.of(120, TimeUnit.SECONDS);

  @Parameter(names = {"-restart-delay"}, description = "Delay before the server restarts itself. Default: 2s", converter = TimeUnitConverter.class)
  private Measure<TimeUnit> restartDelay = Measure.of(2, TimeUnit.SECONDS);

  @Parameter(names = {"-restrict"}, description = "Restrict the activation process to the node only")
  protected boolean restrictedActivation = false;

  private final ActivateCommand underlying = new ActivateCommand();

  @Override
  public void run() {
    // basic validations first

    if (!restrictedActivation && node != null && configPropertiesFile != null) {
      throw new IllegalArgumentException("Either node or config properties file should be specified, not both");
    }

    if (restrictedActivation && node == null) {
      throw new IllegalArgumentException("A node must be supplied for a restricted activation");
    }

    if (licenseFile != null && !licenseFile.toFile().exists()) {
      throw new ParameterException("License file not found: " + licenseFile);
    }
    underlying.setNode(node);
    underlying.setConfigPropertiesFile(configPropertiesFile);
    underlying.setClusterName(clusterName);
    underlying.setLicenseFile(licenseFile);
    underlying.setRestartWaitTime(restartWaitTime);
    underlying.setRestartDelay(restartDelay);
    underlying.setRestrictedActivation(restrictedActivation);

    underlying.run();
  }

  @Override
  public Command getCommand() {
    return underlying;
  }
}
