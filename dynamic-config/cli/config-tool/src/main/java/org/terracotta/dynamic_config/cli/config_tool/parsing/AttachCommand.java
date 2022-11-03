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
import org.terracotta.dynamic_config.cli.api.command.AttachAction;
import org.terracotta.dynamic_config.cli.api.command.Injector.Inject;
import org.terracotta.dynamic_config.cli.api.converter.OperationType;
import org.terracotta.dynamic_config.cli.command.RestartCommand;
import org.terracotta.dynamic_config.cli.command.Usage;
import org.terracotta.dynamic_config.cli.converter.InetSocketAddressConverter;

import java.net.InetSocketAddress;

@Parameters(commandDescription = "Attach a node to a stripe, or a stripe to a cluster")
@Usage("(-stripe <hostname[:port]> -to-cluster <hostname[:port]> | -node <hostname[:port]> -to-stripe <hostname[:port]>) [-restart-wait-time <restart-wait-time>] [-restart-delay <restart-delay>]")
public class AttachCommand extends RestartCommand {

  @Parameter(names = {"-to-cluster"}, description = "Cluster to attach to", converter = InetSocketAddressConverter.class)
  protected InetSocketAddress destinationClusterAddress;

  @Parameter(names = {"-stripe"}, description = "Stripe to be attached", converter = InetSocketAddressConverter.class)
  protected InetSocketAddress sourceStripeAddress;

  @Parameter(names = {"-to-stripe"}, description = "Stripe to attach to", converter = InetSocketAddressConverter.class)
  protected InetSocketAddress destinationStripeAddress;

  @Parameter(names = {"-node"}, description = "Node to be attached", converter = InetSocketAddressConverter.class)
  protected InetSocketAddress sourceNodeAddress;

  @Parameter(names = {"-force"}, description = "Force the operation", hidden = true)
  protected boolean force;

  @Inject
  public final AttachAction action;

  public AttachCommand(AttachAction action) {
    this.action = action;
  }

  public AttachCommand() {
    this(new AttachAction());
  }

  @Override
  public void run() {
    if ((destinationClusterAddress != null && sourceStripeAddress == null) ||
        (destinationClusterAddress == null && sourceStripeAddress != null)) {
      throw new IllegalArgumentException("Both -to-cluster and -stripe must be provided for stripe addition to cluster");
    }
    if ((destinationStripeAddress != null && sourceNodeAddress == null) ||
        (destinationStripeAddress == null && sourceNodeAddress != null)) {
      throw new IllegalArgumentException("Both -to-stripe and -node must be provided for node addition to cluster");
    }
    if (destinationClusterAddress != null && destinationStripeAddress != null) {
      throw new IllegalArgumentException("Either you can perform stripe addition to the cluster or node addition to the stripe");
    }
    if (destinationClusterAddress != null) {
      action.setOperationType(OperationType.STRIPE);
      action.setDestinationAddress(destinationClusterAddress);
      action.setSourceAddress(sourceStripeAddress);
    } else if (destinationStripeAddress != null) {
      action.setOperationType(OperationType.NODE);
      action.setDestinationAddress(destinationStripeAddress);
      action.setSourceAddress(sourceNodeAddress);
    }
    action.setForce(force);
    action.setRestartWaitTime(getRestartWaitTime());
    action.setRestartDelay(getRestartDelay());

    action.run();
  }
}
