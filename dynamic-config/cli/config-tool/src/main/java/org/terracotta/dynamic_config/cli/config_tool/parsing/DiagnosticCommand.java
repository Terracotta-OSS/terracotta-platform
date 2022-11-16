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
import org.terracotta.dynamic_config.cli.api.command.DiagnosticAction;
import org.terracotta.dynamic_config.cli.api.command.Injector.Inject;
import org.terracotta.dynamic_config.cli.command.Command;
import org.terracotta.dynamic_config.cli.command.Usage;
import org.terracotta.dynamic_config.cli.converter.HostPortConverter;
import org.terracotta.inet.HostPort;

import java.util.Collections;
import java.util.List;

@Parameters(commandDescription = "Diagnose a cluster configuration")
@Usage("-connect-to <hostname[:port]> [-output-format <text|json>]")
public class DiagnosticCommand extends Command {

  @Parameter(names = {"-connect-to"}, description = "Node to connect to", required = true, converter = HostPortConverter.class)
  private List<HostPort> nodes = Collections.emptyList();

  @Parameter(names = {"-output-format"}, description = "Output format for the diagnostic")
  private String outputFormat = "text";

  @Inject
  public final DiagnosticAction action;

  public DiagnosticCommand() {
    this(new DiagnosticAction());
  }

  public DiagnosticCommand(DiagnosticAction action) {
    this.action = action;
  }

  @Override
  public void run() {
    action.setNodes(nodes);
    action.setOutputFormat(outputFormat);

    action.run();
  }
}
