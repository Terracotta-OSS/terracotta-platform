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
import org.terracotta.dynamic_config.api.model.Identifier;
import org.terracotta.dynamic_config.cli.command.Command;
import org.terracotta.dynamic_config.cli.command.DeprecatedUsage;
import org.terracotta.dynamic_config.cli.command.JCommanderCommand;
import org.terracotta.dynamic_config.cli.config_tool.command.DetachCommand;
import org.terracotta.dynamic_config.cli.config_tool.converter.OperationType;
import org.terracotta.dynamic_config.cli.converter.IdentifierConverter;
import org.terracotta.dynamic_config.cli.converter.InetSocketAddressConverter;
import org.terracotta.dynamic_config.cli.converter.TimeUnitConverter;

import java.net.InetSocketAddress;

@Parameters(commandNames = "detach", commandDescription = "Detach a node from a stripe, or a stripe from a cluster")
@DeprecatedUsage("detach [-t node|stripe] -d <hostname[:port]> -s [<hostname[:port]>|uid|name] [-f] [-W <stop-wait-time>] [-D <stop-delay>]")
public class DeprecatedDetachJCommanderCommand extends JCommanderCommand {

  @Parameter(names = {"-t"}, description = "Determine if the sources are nodes or stripes. Default: node", converter = OperationType.TypeConverter.class)
  protected OperationType operationType = OperationType.NODE;

  @Parameter(required = true, names = {"-d"}, description = "Destination stripe or cluster", converter = InetSocketAddressConverter.class)
  protected InetSocketAddress destinationAddress;

  @Parameter(names = {"-f"}, description = "Force the operation")
  protected boolean force;

  @Parameter(names = {"-W"}, description = "Maximum time to wait for the nodes to stop. Default: 120s", converter = TimeUnitConverter.class)
  protected Measure<TimeUnit> stopWaitTime = Measure.of(120, TimeUnit.SECONDS);

  @Parameter(names = {"-D"}, description = "Delay before the server stops itself. Default: 2s", converter = TimeUnitConverter.class)
  protected Measure<TimeUnit> stopDelay = Measure.of(2, TimeUnit.SECONDS);

  @Parameter(required = true, names = {"-s"}, description = "Source node or stripe (address, name or UID)", converter = IdentifierConverter.class)
  protected Identifier sourceIdentifier;

  private final DetachCommand underlying = new DetachCommand();

  @Override
  public void validate() {
    underlying.setOperationType(operationType);
    underlying.setDestinationAddress(destinationAddress);
    underlying.setForce(force);
    underlying.setSourceIdentifier(sourceIdentifier);
    underlying.setStopWaitTime(stopWaitTime);
    underlying.setStopDelay(stopDelay);
  }

  @Override
  public void run() {
    underlying.run();
  }

  @Override
  public Command getCommand() {
    return underlying;
  }
}
