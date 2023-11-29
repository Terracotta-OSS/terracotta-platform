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
import org.terracotta.dynamic_config.cli.api.command.ActivateAction;
import org.terracotta.dynamic_config.cli.api.command.Injector.Inject;
import org.terracotta.dynamic_config.cli.command.RestartCommand;
import org.terracotta.dynamic_config.cli.command.Usage;
import org.terracotta.dynamic_config.cli.converter.HostPortConverter;
import org.terracotta.dynamic_config.cli.converter.ShapeConverter;
import org.terracotta.inet.HostPort;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Parameters(commandDescription = "Activate a cluster")
@Usage("(-connect-to <hostname[:port]> | -config-file <config.cfg|config.properties> | -stripe-shape <[name/]hostname[:port]|hostname[:port]|...>) [-cluster-name <cluster-name>] [-restrict] [-license-file <license-file>] [-restart-wait-time <restart-wait-time>] [-restart-delay <restart-delay>]")
public class ActivateCommand extends RestartCommand {

  @Parameter(names = {"-connect-to"}, description = "Node to connect to", converter = HostPortConverter.class)
  private List<HostPort> nodes = Collections.emptyList();

  @Parameter(names = {"-config-file"}, description = "Configuration properties file containing nodes to be activated", converter = PathConverter.class)
  private Path configPropertiesFile;

  @Parameter(names = {"-cluster-name"}, description = "Cluster name")
  private String clusterName;

  // Allows to quickly activate a cluster when all nodes are already started with the right parameters
  // Examples:
  // config-tool -cluster-name tc-cluster -stripe node-1-1:9410|node-1-2,node-2-1:9410|node-2-2
  // config-tool -cluster-name tc-cluster -stripe node-1-1:9410|node-1-2 -stripe node-2-1:9410|node-2-2
  // config-tool -cluster-name tc-cluster -stripe stripe1/node-1-1:9410|node-1-2,stripe2/node-2-1:9410|node-2-2
  @Parameter(names = {"-stripe-shape", "-stripe"}, description = "Stripe shape", converter = ShapeConverter.class)
  private List<Map.Entry<Collection<HostPort>, String>> shape = Collections.emptyList();

  @Parameter(names = {"-license-file"}, description = "License file", converter = PathConverter.class)
  private Path licenseFile;

  @Parameter(names = {"-restrict"}, description = "Restrict the activation process to the specified nodes only")
  protected boolean restrictedActivation = false;

  @Inject
  public ActivateAction action;

  public ActivateCommand() {
    this(new ActivateAction());
  }

  public ActivateCommand(ActivateAction underlying) {
    this.action = underlying;
  }

  @Override
  public void run() {
    // basic validations first

    if (!shape.isEmpty() && (!nodes.isEmpty() || configPropertiesFile != null)) {
      throw new IllegalArgumentException("Fast activation with '-stripe' cannot be used with '-config-file' and '-connect-to'");
    }
    if (!shape.isEmpty() && clusterName == null) {
      throw new IllegalArgumentException("Fast activation with '-stripe' requires '-cluster-name' to be used");
    }

    if (!restrictedActivation && !nodes.isEmpty() && configPropertiesFile != null) {
      throw new IllegalArgumentException("Either node or config properties file should be specified, not both");
    }

    if (restrictedActivation && nodes.isEmpty()) {
      throw new IllegalArgumentException("A node must be supplied for a restricted activation");
    }

    if (licenseFile != null && !licenseFile.toFile().exists()) {
      throw new ParameterException("License file not found: " + licenseFile);
    }
    action.setNodes(nodes);
    action.setConfigPropertiesFile(configPropertiesFile);
    action.setClusterName(clusterName);
    action.setLicenseFile(licenseFile);
    action.setRestartWaitTime(getRestartWaitTime());
    action.setRestartDelay(getRestartDelay());
    action.setRestrictedActivation(restrictedActivation);
    action.setShape(shape);

    action.run();
  }
}
