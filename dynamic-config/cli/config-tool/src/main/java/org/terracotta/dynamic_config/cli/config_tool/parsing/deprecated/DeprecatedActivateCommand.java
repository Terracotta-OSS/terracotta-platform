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
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.PathConverter;
import org.terracotta.common.struct.Measure;
import org.terracotta.common.struct.TimeUnit;
import org.terracotta.dynamic_config.cli.api.command.ActivateAction;
import org.terracotta.dynamic_config.cli.api.command.Injector.Inject;
import org.terracotta.dynamic_config.cli.command.Command;
import org.terracotta.dynamic_config.cli.command.Usage;
import org.terracotta.dynamic_config.cli.converter.HostPortConverter;
import org.terracotta.dynamic_config.cli.converter.TimeUnitConverter;
import org.terracotta.inet.HostPort;

import java.nio.file.Path;

import static java.util.Collections.singletonList;

@Parameters(commandDescription = "Activate a cluster")
@Usage("(-s <hostname[:port]> | -f <config.cfg|config.properties>) [-n <cluster-name>] [-R] [-l <license-file>] [-W <restart-wait-time>] [-D <restart-delay>]")
public class DeprecatedActivateCommand extends Command {
  @Parameter(names = {"-s"}, description = "Node to connect to", converter = HostPortConverter.class)
  private HostPort node;

  @Parameter(names = {"-f"}, description = "Configuration properties file containing nodes to be activated", converter = PathConverter.class)
  private Path configPropertiesFile;

  @Parameter(names = {"-n"}, description = "Cluster name")
  private String clusterName;

  @Parameter(names = {"-l"}, description = "License file", converter = PathConverter.class)
  private Path licenseFile;

  @Parameter(names = {"-W"}, description = "Maximum time to wait for the nodes to restart. Default: 120s", converter = TimeUnitConverter.class)
  private Measure<TimeUnit> restartWaitTime = Measure.of(120, TimeUnit.SECONDS);

  @Parameter(names = {"-D"}, description = "Delay before the server restarts itself. Default: 2s", converter = TimeUnitConverter.class)
  private Measure<TimeUnit> restartDelay = Measure.of(2, TimeUnit.SECONDS);

  @Parameter(names = {"-R"}, description = "Restrict the activation process to the node only")
  protected boolean restrictedActivation = false;

  @Inject
  public ActivateAction action;

  public DeprecatedActivateCommand(ActivateAction action) {
    this.action = action;
  }

  public DeprecatedActivateCommand() {
    this(new ActivateAction());
  }

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
    action.setNodes(singletonList(node));
    action.setConfigPropertiesFile(configPropertiesFile);
    action.setClusterName(clusterName);
    action.setLicenseFile(licenseFile);
    action.setRestartWaitTime(restartWaitTime);
    action.setRestartDelay(restartDelay);
    action.setRestrictedActivation(restrictedActivation);

    action.run();
  }
}
