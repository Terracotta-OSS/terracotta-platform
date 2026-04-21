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

import org.terracotta.diagnostic.model.LogicalServerState;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node.Endpoint;
import org.terracotta.inet.HostPort;

import java.util.Map;

public abstract class LockUnlockConfigAction extends RemoteAction {

  protected HostPort node;
  protected boolean force;

  protected Map<Endpoint, LogicalServerState> onlineNodes;
  protected Cluster cluster;

  public void setForce(boolean force) {
    this.force = force;
  }

  public void setNode(HostPort node) {
    this.node = node;
  }

  @Override
  public void run() {
    cluster = getRuntimeCluster(node);
    onlineNodes = findOnlineRuntimePeers(node);

    boolean allOnlineNodesActivated = areAllNodesActivated(onlineNodes.keySet());
    if (!allOnlineNodesActivated) {
      throw new IllegalStateException("The cluster is not fully activated: a lock or unlock operation can only be performed on an activated cluster.");
    }

    // Remove Relay nodes from the list: they do not take part of nomad tx
    removeRelayNodes(onlineNodes);

    // validate that all the online nodes are either actives or passives
    ensureNodesAreEitherActiveOrPassive(onlineNodes);

    ensureActivesAreAllOnline(cluster, onlineNodes);

    if (requiresAllNodesAlive()) {
      // Check passive nodes as well if the setting requires all nodes to be online
      ensurePassivesAreAllOnline(cluster, onlineNodes);
    }
  }

  protected boolean requiresAllNodesAlive() {
    // By default, we require all nodes to be up to allow a DC change: the consensus system needs all nodes to be able to vote and prepare the change.
    // This will also solve any potential partitioned config to happen (or loosing a change if a failover happens just after),
    // in the case a nomad change is prepared at the same time a passive is synchronizing the DC configuration from active server.
    // This can be overridden by the user if he knows what he is doing with: -force
    return !force;
  }
}
