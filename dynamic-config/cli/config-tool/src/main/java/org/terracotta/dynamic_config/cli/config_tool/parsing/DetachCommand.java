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
import org.terracotta.dynamic_config.api.model.Identifier;
import org.terracotta.dynamic_config.cli.api.command.DetachAction;
import org.terracotta.dynamic_config.cli.api.command.Injector.Inject;
import org.terracotta.dynamic_config.cli.api.converter.OperationType;
import org.terracotta.dynamic_config.cli.command.Command;
import org.terracotta.dynamic_config.cli.command.Usage;
import org.terracotta.dynamic_config.cli.converter.IdentifierConverter;
import org.terracotta.dynamic_config.cli.converter.InetSocketAddressConverter;
import org.terracotta.dynamic_config.cli.converter.TimeUnitConverter;

import java.net.InetSocketAddress;

@Parameters(commandDescription = "Detach a node from a stripe, or a stripe from a cluster")
@Usage("(-from-cluster <hostname[:port]> -stripe [<hostname[:port]>|uid|name] | -from-stripe <hostname[:port]> -node [<hostname[:port]>|uid|name]) [-stop-wait-time <stop-wait-time>] [-stop-delay <stop-delay>]")
public class DetachCommand extends Command {

  @Parameter(names = {"-from-cluster"}, description = "Cluster to detach from", converter = InetSocketAddressConverter.class)
  protected InetSocketAddress destinationClusterAddress;

  @Parameter(names = {"-stripe"}, description = "Source node or stripe (address, name or UID)", converter = IdentifierConverter.class)
  protected Identifier sourceStripeIdentifier;

  @Parameter(names = {"-from-stripe"}, description = "Stripe to detach from", converter = InetSocketAddressConverter.class)
  protected InetSocketAddress destinationStripeAddress;

  @Parameter(names = {"-node"}, description = "Node to be detached", converter = IdentifierConverter.class)
  protected Identifier sourceNodeIdentifier;

  @Parameter(names = {"-stop-wait-time"}, description = "Maximum time to wait for the nodes to stop. Default: 120s", converter = TimeUnitConverter.class)
  protected Measure<TimeUnit> stopWaitTime = Measure.of(120, TimeUnit.SECONDS);

  @Parameter(names = {"-stop-delay"}, description = "Delay before the server stops itself. Default: 2s", converter = TimeUnitConverter.class)
  protected Measure<TimeUnit> stopDelay = Measure.of(2, TimeUnit.SECONDS);

  @Parameter(names = {"-force"}, description = "Force the operation", hidden = true)
  protected boolean force;

  @Inject
  public final DetachAction action;

  public DetachCommand() {
    this(new DetachAction());
  }

  public DetachCommand(DetachAction action) {
    this.action = action;
  }

  @Override
  public void run() {
    if ((destinationClusterAddress != null && sourceStripeIdentifier == null) ||
        (destinationClusterAddress == null && sourceStripeIdentifier != null)) {
      throw new IllegalArgumentException("Both -from-cluster and -stripe must be provided for stripe detachment from cluster");
    }
    if ((destinationStripeAddress != null && sourceNodeIdentifier == null) ||
        (destinationStripeAddress == null && sourceNodeIdentifier != null)) {
      throw new IllegalArgumentException("Both -from-stripe and -node must be provided for node deletion from cluster");
    }
    if (destinationClusterAddress != null && destinationStripeAddress != null) {
      throw new IllegalArgumentException("Either you can perform stripe deletion from the cluster or node deletion from the stripe");
    }
    if (destinationClusterAddress != null) {
      action.setOperationType(OperationType.STRIPE);
      action.setDestinationAddress(destinationClusterAddress);
      action.setSourceIdentifier(sourceStripeIdentifier);
    } else if (destinationStripeAddress != null) {
      action.setOperationType(OperationType.NODE);
      action.setDestinationAddress(destinationStripeAddress);
      action.setSourceIdentifier(sourceNodeIdentifier);
    }
    action.setForce(force);
    action.setStopWaitTime(stopWaitTime);
    action.setStopDelay(stopDelay);

    action.run();
  }
}
