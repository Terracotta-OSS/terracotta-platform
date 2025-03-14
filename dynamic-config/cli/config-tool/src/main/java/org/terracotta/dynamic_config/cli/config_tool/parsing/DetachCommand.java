/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
import org.terracotta.dynamic_config.cli.converter.HostPortConverter;
import org.terracotta.dynamic_config.cli.converter.IdentifierConverter;
import org.terracotta.dynamic_config.cli.converter.TimeUnitConverter;
import org.terracotta.inet.HostPort;

@Parameters(commandDescription = "Detach a node from a stripe, or a stripe from a cluster")
@Usage("(-stripe [<hostname[:port]>|uid|name] -from-cluster <hostname[:port]> | -node [<hostname[:port]>|uid|name] -from-stripe <hostname[:port]>) [-stop-wait-time <stop-wait-time>] [-stop-delay <stop-delay>]")
public class DetachCommand extends Command {

  @Parameter(names = {"-from-cluster"}, description = "Cluster to detach from", converter = HostPortConverter.class)
  protected HostPort destinationCluster;

  @Parameter(names = {"-stripe"}, description = "Source node or stripe (address, name or UID)", converter = IdentifierConverter.class)
  protected Identifier sourceStripeIdentifier;

  @Parameter(names = {"-from-stripe"}, description = "Stripe to detach from", converter = HostPortConverter.class)
  protected HostPort destinationStripe;

  @Parameter(names = {"-node"}, description = "Node to be detached", converter = IdentifierConverter.class)
  protected Identifier sourceNodeIdentifier;

  @Parameter(names = {"-stop-wait-time"}, description = "Maximum time to wait for the nodes to stop. Default: 120s", converter = TimeUnitConverter.class)
  protected Measure<TimeUnit> stopWaitTime = Measure.of(120, TimeUnit.SECONDS);

  @Parameter(names = {"-stop-delay"}, description = "Delay before the server stops itself. Default: 2s", converter = TimeUnitConverter.class)
  protected Measure<TimeUnit> stopDelay = Measure.of(2, TimeUnit.SECONDS);

  @Parameter(names = {"-force"}, description = "Force the operation", hidden = true)
  protected boolean force;

  @Parameter(names = {"-lock"}, description = "Create a lock before executing the Nomad operation", hidden = true)
  protected boolean lock;

  @Parameter(names = {"-unlock"}, description = "Unlock after executing the Nomad operation", hidden = true)
  protected boolean unlock;

  @Inject
  public DetachAction action;

  public DetachCommand() {
    this(new DetachAction());
  }

  public DetachCommand(DetachAction action) {
    this.action = action;
  }

  @Override
  public void run() {
    if ((destinationCluster != null && sourceStripeIdentifier == null) ||
        (destinationCluster == null && sourceStripeIdentifier != null)) {
      throw new IllegalArgumentException("Both -from-cluster and -stripe must be provided for stripe detachment from cluster");
    }
    if ((destinationStripe != null && sourceNodeIdentifier == null) ||
        (destinationStripe == null && sourceNodeIdentifier != null)) {
      throw new IllegalArgumentException("Both -from-stripe and -node must be provided for node deletion from cluster");
    }
    if (destinationCluster != null && destinationStripe != null) {
      throw new IllegalArgumentException("Either you can perform stripe deletion from the cluster or node deletion from the stripe");
    }
    if (destinationCluster != null) {
      action.setOperationType(OperationType.STRIPE);
      action.setDestinationHostPort(destinationCluster);
      action.setSourceIdentifier(sourceStripeIdentifier);
    } else if (destinationStripe != null) {
      action.setOperationType(OperationType.NODE);
      action.setDestinationHostPort(destinationStripe);
      action.setSourceIdentifier(sourceNodeIdentifier);
    }
    action.setForce(force);
    action.setUnlock(unlock);
    action.setLock(lock);
    action.setStopWaitTime(stopWaitTime);
    action.setStopDelay(stopDelay);

    action.run();
  }
}
