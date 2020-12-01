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
import org.terracotta.dynamic_config.cli.api.command.AttachCommand;
import org.terracotta.dynamic_config.cli.api.command.Command;
import org.terracotta.dynamic_config.cli.api.converter.OperationType;
import org.terracotta.dynamic_config.cli.command.DeprecatedUsage;
import org.terracotta.dynamic_config.cli.command.JCommanderCommand;
import org.terracotta.dynamic_config.cli.converter.InetSocketAddressConverter;
import org.terracotta.dynamic_config.cli.converter.TimeUnitConverter;
import org.terracotta.dynamic_config.cli.converter.TypeConverter;

import java.net.InetSocketAddress;

@Parameters(commandNames = "attach", commandDescription = "Attach a node to a stripe, or a stripe to a cluster")
@DeprecatedUsage("attach [-t node|stripe] -d <hostname[:port]> -s <hostname[:port]> [-f] [-W <restart-wait-time>] [-D <restart-delay>]")
public class DeprecatedAttachJCommanderCommand extends JCommanderCommand {

  @Parameter(names = {"-t"}, description = "Determine if the sources are nodes or stripes. Default: node", converter = TypeConverter.class)
  protected OperationType operationType = OperationType.NODE;

  @Parameter(required = true, names = {"-d"}, description = "Destination stripe or cluster", converter = InetSocketAddressConverter.class)
  protected InetSocketAddress destinationAddress;

  @Parameter(names = {"-f"}, description = "Force the operation")
  protected boolean force;

  @Parameter(names = {"-W"}, description = "Maximum time to wait for the nodes to restart. Default: 120s", converter = TimeUnitConverter.class)
  protected Measure<TimeUnit> restartWaitTime = Measure.of(120, TimeUnit.SECONDS);

  @Parameter(names = {"-D"}, description = "Delay before the server restarts itself. Default: 2s", converter = TimeUnitConverter.class)
  protected Measure<TimeUnit> restartDelay = Measure.of(2, TimeUnit.SECONDS);

  @Parameter(required = true, names = {"-s"}, description = "Source node or stripe", converter = InetSocketAddressConverter.class)
  protected InetSocketAddress sourceAddress;

  private final AttachCommand underlying = new AttachCommand();

  @Override
  public void run() {
    underlying.setOperationType(operationType);
    underlying.setDestinationAddress(destinationAddress);
    underlying.setForce(force);
    underlying.setSourceAddress(sourceAddress);
    underlying.setRestartWaitTime(restartWaitTime);
    underlying.setRestartDelay(restartDelay);

    underlying.run();
  }

  @Override
  public Command getCommand() {
    return underlying;
  }
}
