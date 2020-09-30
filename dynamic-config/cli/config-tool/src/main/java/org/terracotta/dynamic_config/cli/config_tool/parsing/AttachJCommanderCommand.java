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
import org.terracotta.common.struct.Measure;
import org.terracotta.common.struct.TimeUnit;
import org.terracotta.dynamic_config.cli.command.Command;
import org.terracotta.dynamic_config.cli.command.JCommanderCommand;
import org.terracotta.dynamic_config.cli.command.Usage;
import org.terracotta.dynamic_config.cli.config_tool.command.AttachCommand;
import org.terracotta.dynamic_config.cli.config_tool.converter.OperationType;
import org.terracotta.dynamic_config.cli.converter.InetSocketAddressConverter;
import org.terracotta.dynamic_config.cli.converter.TimeUnitConverter;

import java.net.InetSocketAddress;

@Parameters(commandNames = "attach", commandDescription = "Attach a node to a stripe, or a stripe to a cluster")
@Usage("attach (-to-cluster <hostname[:port]> -stripe <hostname[:port]> | -to-stripe <hostname[:port]> -node <hostname[:port]>) [-force] [-restart-wait-time <restart-wait-time>] [-restart-delay <restart-delay>]")
public class AttachJCommanderCommand extends JCommanderCommand {

  @Parameter(names = {"-to-cluster"}, description = "Cluster to attach to", converter = InetSocketAddressConverter.class)
  protected InetSocketAddress destinationClusterAddress;

  @Parameter(names = {"-stripe"}, description = "Stripe to be attached", converter = InetSocketAddressConverter.class)
  protected InetSocketAddress sourceStripeAddress;

  @Parameter(names = {"-to-stripe"}, description = "Stripe to attach to", converter = InetSocketAddressConverter.class)
  protected InetSocketAddress destinationStripeAddress;

  @Parameter(names = {"-node"}, description = "Node to be attached", converter = InetSocketAddressConverter.class)
  protected InetSocketAddress sourceNodeAddress;

  @Parameter(names = {"-restart-wait-time"}, description = "Maximum time to wait for the nodes to restart. Default: 120s", converter = TimeUnitConverter.class)
  protected Measure<TimeUnit> restartWaitTime = Measure.of(120, TimeUnit.SECONDS);

  @Parameter(names = {"-restart-delay"}, description = "Delay before the server restarts itself. Default: 2s", converter = TimeUnitConverter.class)
  protected Measure<TimeUnit> restartDelay = Measure.of(2, TimeUnit.SECONDS);

  @Parameter(names = {"-force"}, description = "Force the operation")
  protected boolean force;

  private final AttachCommand underlying = new AttachCommand();

  @Override
  public void validate() {
    if ((destinationClusterAddress != null && sourceStripeAddress == null) ||
        (destinationClusterAddress == null && sourceStripeAddress != null)) {
      throw new IllegalArgumentException("Both -to-cluster and -stripe must be provided for stripe addition to cluster");
    }
    if ((destinationStripeAddress != null && sourceNodeAddress == null) ||
        (destinationStripeAddress == null && sourceNodeAddress != null)) {
      throw new IllegalArgumentException("Both -to-stripe and -node must be provided for node addition to cluster");
    }
    if ((destinationClusterAddress != null || sourceStripeAddress != null) &&
        (destinationStripeAddress != null || (sourceNodeAddress != null))) {
      throw new IllegalArgumentException("Either you can perform stripe addition to the cluster or node addition to the stripe");
    }
    if (destinationClusterAddress != null) {
      underlying.setOperationType(OperationType.STRIPE);
      underlying.setDestinationAddress(destinationClusterAddress);
      underlying.setSourceAddress(sourceStripeAddress);
    } else if (destinationStripeAddress != null) {
      underlying.setOperationType(OperationType.NODE);
      underlying.setDestinationAddress(destinationStripeAddress);
      underlying.setSourceAddress(sourceNodeAddress);
    }
    underlying.setForce(force);
    underlying.setRestartWaitTime(restartWaitTime);
    underlying.setRestartDelay(restartDelay);
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
