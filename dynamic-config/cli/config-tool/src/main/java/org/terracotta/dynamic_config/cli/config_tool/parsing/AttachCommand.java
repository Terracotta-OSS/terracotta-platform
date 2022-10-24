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
import org.terracotta.dynamic_config.cli.converter.HostPortConverter;
import org.terracotta.inet.HostPort;

@Parameters(commandDescription = "Attach a node to a stripe, or a stripe to a cluster")
@Usage("(-to-cluster <hostname[:port]> -stripe <hostname[:port]> | -to-stripe <hostname[:port]> -node <hostname[:port]>) [-restart-wait-time <restart-wait-time>] [-restart-delay <restart-delay>]")
public class AttachCommand extends RestartCommand {

  @Parameter(names = {"-to-cluster"}, description = "Cluster to attach to", converter = HostPortConverter.class)
  protected HostPort destinationCluster;

  @Parameter(names = {"-stripe"}, description = "Stripe to be attached", converter = HostPortConverter.class)
  protected HostPort sourceStripe;

  @Parameter(names = {"-to-stripe"}, description = "Stripe to attach to", converter = HostPortConverter.class)
  protected HostPort destinationStripe;

  @Parameter(names = {"-node"}, description = "Node to be attached", converter = HostPortConverter.class)
  protected HostPort sourceNode;

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
    if ((destinationCluster != null && sourceStripe == null) ||
        (destinationCluster == null && sourceStripe != null)) {
      throw new IllegalArgumentException("Both -to-cluster and -stripe must be provided for stripe addition to cluster");
    }
    if ((destinationStripe != null && sourceNode == null) ||
        (destinationStripe == null && sourceNode != null)) {
      throw new IllegalArgumentException("Both -to-stripe and -node must be provided for node addition to cluster");
    }
    if (destinationCluster != null && destinationStripe != null) {
      throw new IllegalArgumentException("Either you can perform stripe addition to the cluster or node addition to the stripe");
    }
    if (destinationCluster != null) {
      action.setOperationType(OperationType.STRIPE);
      action.setDestinationHostPort(destinationCluster);
      action.setSourceHostPort(sourceStripe);
    } else if (destinationStripe != null) {
      action.setOperationType(OperationType.NODE);
      action.setDestinationHostPort(destinationStripe);
      action.setSourceHostPort(sourceNode);
    }
    action.setForce(force);
    action.setRestartWaitTime(getRestartWaitTime());
    action.setRestartDelay(getRestartDelay());

    action.run();
  }
}
