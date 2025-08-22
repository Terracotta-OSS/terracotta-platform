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
import org.terracotta.dynamic_config.cli.api.command.AttachAction;
import org.terracotta.dynamic_config.cli.api.command.Injector.Inject;
import org.terracotta.dynamic_config.cli.api.converter.OperationType;
import org.terracotta.dynamic_config.cli.command.RestartCommand;
import org.terracotta.dynamic_config.cli.command.Usage;
import org.terracotta.dynamic_config.cli.converter.HostPortConverter;
import org.terracotta.dynamic_config.cli.converter.ShapeConverter;
import org.terracotta.inet.HostPort;

import java.util.Collection;
import java.util.Map;

@Parameters(commandDescription = "Attach a node to a stripe, or a stripe to a cluster")
@Usage("(-stripe-shape [name/]hostname[:port]|hostname[:port] -to-cluster <hostname[:port]> | -stripe <hostname[:port]> -to-cluster <hostname[:port]> | -node <hostname[:port]> -to-stripe <hostname[:port]>) [-restart-wait-time <restart-wait-time>] [-restart-delay <restart-delay>]")
public class AttachCommand extends RestartCommand {

  @Parameter(names = {"-to-cluster"}, description = "Cluster to attach to", converter = HostPortConverter.class)
  protected HostPort destinationCluster;

  @Parameter(names = {"-stripe"}, description = "Stripe to be attached", converter = HostPortConverter.class)
  protected HostPort sourceStripe;

  // Allows to quickly attach a stripe when all nodes are already started
  // Examples:
  // config-tool attach -stripe-shape node-2-1:9410|node-2-2 -to-cluster node-1-1:9410
  // config-tool attach -stripe-shape stripe2/node-2-1:9410|node-2-2  -to-cluster node-1-1:9410
  @Parameter(names = {"-stripe-shape"}, description = "Stripe shape to be attached", converter = ShapeConverter.class)
  private Map.Entry<Collection<HostPort>, String> sourceStripeShape;

  @Parameter(names = {"-to-stripe"}, description = "Stripe to attach to", converter = HostPortConverter.class)
  protected HostPort destinationStripe;

  @Parameter(names = {"-node"}, description = "Node to be attached", converter = HostPortConverter.class)
  protected HostPort sourceNode;

  @Parameter(names = {"-force"}, description = "Force the operation", hidden = true)
  protected boolean force;

  @Parameter(names = {"-lock"}, description = "Create a lock before executing the Nomad operation", hidden = true)
  protected boolean lock;

  @Parameter(names = {"-unlock"}, description = "Unlock after executing the Nomad operation", hidden = true)
  protected boolean unlock;

  @Inject
  public AttachAction action;

  public AttachCommand(AttachAction action) {
    this.action = action;
  }

  public AttachCommand() {
    this(new AttachAction());
  }

  @Override
  public void run() {
    if (destinationCluster != null && destinationStripe != null) {
      throw new IllegalArgumentException("Either you can perform stripe addition to the cluster or node addition to the stripe");
    }
    if (destinationCluster == null && destinationStripe == null) {
      throw new IllegalArgumentException("One of -to-cluster or -to-stripe is missing");
    }

    // node addition
    if (destinationStripe != null) {
      if (sourceNode == null) {
        throw new IllegalArgumentException("Both -to-stripe and -node must be provided for node addition to cluster");
      }
      if (sourceStripe != null || sourceStripeShape != null) {
        throw new IllegalArgumentException("-to-stripe cannot be used with -stripe and -stripe-shape");
      }

      action.setOperationType(OperationType.NODE);
      action.setDestinationHostPort(destinationStripe);
      action.setSourceHostPort(sourceNode);

    } else { // destinationCluster != null
      if (sourceStripe == null && sourceStripeShape == null) {
        throw new IllegalArgumentException("-to-cluster requires one of -stripe or -stripe-shape");
      }
      if (sourceStripe != null && sourceStripeShape != null) {
        throw new IllegalArgumentException("-to-cluster requires either -stripe or -stripe-shape");
      }
      if (sourceNode != null) {
        throw new IllegalArgumentException("-to-cluster cannot be used with -node");
      }

      action.setOperationType(OperationType.STRIPE);
      action.setDestinationHostPort(destinationCluster);

      if (sourceStripe != null) {
        action.setStripeFromSource(sourceStripe);
      } else { // sourceStripeShape != null
        action.setStripeFromShape(sourceStripeShape.getKey(), sourceStripeShape.getValue());
      }
    }

    action.setForce(force);
    action.setLock(lock);
    action.setUnlock(unlock);
    action.setRestartWaitTime(getRestartWaitTime());
    action.setRestartDelay(getRestartDelay());

    action.run();
  }
}
