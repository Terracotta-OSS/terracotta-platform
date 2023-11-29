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
import com.beust.jcommander.Parameters;
import org.terracotta.common.struct.Measure;
import org.terracotta.common.struct.TimeUnit;
import org.terracotta.dynamic_config.cli.api.command.AttachAction;
import org.terracotta.dynamic_config.cli.api.command.Injector.Inject;
import org.terracotta.dynamic_config.cli.api.converter.OperationType;
import org.terracotta.dynamic_config.cli.command.Command;
import org.terracotta.dynamic_config.cli.command.Usage;
import org.terracotta.dynamic_config.cli.converter.HostPortConverter;
import org.terracotta.dynamic_config.cli.converter.TimeUnitConverter;
import org.terracotta.dynamic_config.cli.converter.TypeConverter;
import org.terracotta.inet.HostPort;

@Parameters(commandDescription = "Attach a node to a stripe, or a stripe to a cluster")
@Usage("[-t node|stripe] -d <hostname[:port]> -s <hostname[:port]> [-f] [-W <restart-wait-time>] [-D <restart-delay>]")
public class DeprecatedAttachCommand extends Command {

  @Parameter(names = {"-t"}, description = "Determine if the sources are nodes or stripes. Default: node", converter = TypeConverter.class)
  protected OperationType operationType = OperationType.NODE;

  @Parameter(required = true, names = {"-d"}, description = "Destination stripe or cluster", converter = HostPortConverter.class)
  protected HostPort destinationHostPort;

  @Parameter(names = {"-f"}, description = "Force the operation")
  protected boolean force;

  @Parameter(names = {"-W"}, description = "Maximum time to wait for the nodes to restart. Default: 120s", converter = TimeUnitConverter.class)
  protected Measure<TimeUnit> restartWaitTime = Measure.of(120, TimeUnit.SECONDS);

  @Parameter(names = {"-D"}, description = "Delay before the server restarts itself. Default: 2s", converter = TimeUnitConverter.class)
  protected Measure<TimeUnit> restartDelay = Measure.of(2, TimeUnit.SECONDS);

  @Parameter(required = true, names = {"-s"}, description = "Source node or stripe", converter = HostPortConverter.class)
  protected HostPort sourceHostPort;

  @Inject
  public AttachAction action;

  public DeprecatedAttachCommand() {
    this(new AttachAction());
  }

  public DeprecatedAttachCommand(AttachAction action) {
    this.action = action;
  }

  @Override
  public void run() {
    action.setOperationType(operationType);
    action.setDestinationHostPort(destinationHostPort);

    action.setForce(force);
    action.setRestartWaitTime(restartWaitTime);
    action.setRestartDelay(restartDelay);

    switch (operationType) {
      case NODE:
        action.setSourceHostPort(sourceHostPort);
        break;
      case STRIPE:
        action.setStripeFromSource(sourceHostPort);
        break;
      default:
        throw new AssertionError();
    }

    action.run();
  }
}
