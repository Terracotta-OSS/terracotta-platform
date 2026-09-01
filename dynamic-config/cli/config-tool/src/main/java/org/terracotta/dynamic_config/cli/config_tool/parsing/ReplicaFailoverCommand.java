/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2026
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
import org.terracotta.dynamic_config.cli.api.command.Injector.Inject;
import org.terracotta.dynamic_config.cli.api.command.ReplicaFailoverAction;
import org.terracotta.dynamic_config.cli.command.RestartCommand;
import org.terracotta.dynamic_config.cli.command.Usage;
import org.terracotta.dynamic_config.cli.converter.HostPortConverter;
import org.terracotta.inet.HostPort;

import java.util.Collections;
import java.util.List;

@Parameters(commandDescription = "Transition a DR replica cluster to active when the primary cluster is unavailable.")
@Usage("-connect-to <hostname[:port]>")

public class ReplicaFailoverCommand extends RestartCommand {

  @Parameter(names = {"-connect-to"}, description = "Node to connect to", required = true, converter = HostPortConverter.class)
  List<HostPort> nodes = Collections.emptyList();

  @Inject
  public ReplicaFailoverAction action;

  public ReplicaFailoverCommand() {
    this(new ReplicaFailoverAction());
  }

  public ReplicaFailoverCommand(ReplicaFailoverAction action) {
    this.action = action;
  }

  @Override
  public void run() {
    action.setNodes(nodes);
    action.run();
  }
}
