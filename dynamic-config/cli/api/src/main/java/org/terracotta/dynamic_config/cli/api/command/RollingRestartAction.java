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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyList;

public class RollingRestartAction extends RemoteAction {

  private List<HostPort> nodes = emptyList();

  public void setNodes(List<HostPort> nodes) {
    this.nodes = nodes;
  }

  @Override
  public void run() {
    Cluster cluster = getRuntimeCluster(nodes);
    Map<Endpoint, LogicalServerState> allNodes = findRuntimePeersStatus(cluster, nodes);
    Set<Endpoint> ordinaryOnlineEndpoints = filter(allNodes, (endpoint, state) -> state.isActive() || state.isPassive()).keySet();
    rollingRestart(cluster, ordinaryOnlineEndpoints, new LinkedHashSet<>());
  }
}
