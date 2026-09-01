/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2026
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
package org.terracotta.dynamic_config.cli.api.command;

import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.SettingName;
import org.terracotta.dynamic_config.cli.api.command.Injector.Inject;
import org.terracotta.diagnostic.model.LogicalServerState;
import org.terracotta.dynamic_config.api.model.Node.Endpoint;
import org.terracotta.inet.HostPort;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyList;
import static org.terracotta.diagnostic.model.LogicalServerState.REPLICA;
import static org.terracotta.diagnostic.model.LogicalServerState.REPLICA_SUSPENDED;
import static org.terracotta.diagnostic.model.LogicalServerState.UNREACHABLE;

public class ReplicaFailoverAction extends RemoteAction {

  @Inject
  public UnsetAction unsetAction = new UnsetAction();

  private List<HostPort> nodes = emptyList();

  public void setNodes(List<HostPort> nodes) {
    this.nodes = nodes;
  }

  @Override
  public void run() {
    Map<Endpoint, LogicalServerState> allNodes = findRuntimePeersStatus(nodes);
    Set<Endpoint> unreachableNodes = filter(allNodes, (endpoint, state) -> state == UNREACHABLE).keySet();
    Set<Endpoint> replicaNodes = filter(allNodes, (endpoint, state) -> state == REPLICA).keySet();
    Set<Endpoint> replicaSuspendedNodes = filter(allNodes, (endpoint, state) -> state == REPLICA_SUSPENDED).keySet();
    Cluster originalCluster = getUpcomingCluster(nodes);
    if (!unreachableNodes.isEmpty()) {
      throw new IllegalStateException("Cannot execute replica-failover: the following nodes are unreachable: "
        + toString(unreachableNodes) + ". Ensure all nodes are online and try again.");
    }
    if (replicaNodes.size() + replicaSuspendedNodes.size() < originalCluster.getStripeCount()) {
      throw new IllegalStateException("Cannot trigger replica-failover as one or more nodes are not in the expected"
        + " PASSIVE-REPLICA or PASSIVE-REPLICA-START state. Please seek support if needed.");
    }
    if (!replicaSuspendedNodes.isEmpty()) {
      output.warn("The following nodes are in PASSIVE-REPLICA-START state (not fully synced): {}. The replica flag will be" +
        " unset on these nodes, please restart them, and if they fail to transition to ACTIVE, please seek support.", toString(replicaSuspendedNodes));
    }
    unsetAction.setNodes(nodes);
    List<ConfigurationInput> inputs = allNodes.keySet().stream()
      .map(endpoint -> new ConfigurationInput(endpoint.getNodeName() + ":" + SettingName.REPLICA))
      .toList();
    unsetAction.setConfigurationInputs(inputs);
    unsetAction.run();
  }

}
