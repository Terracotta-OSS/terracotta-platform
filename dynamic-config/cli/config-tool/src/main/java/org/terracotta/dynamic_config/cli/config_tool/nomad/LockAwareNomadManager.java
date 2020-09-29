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
package org.terracotta.dynamic_config.cli.config_tool.nomad;

import org.terracotta.diagnostic.model.LogicalServerState;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node.Endpoint;
import org.terracotta.dynamic_config.api.model.nomad.DynamicConfigNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.LockAwareDynamicConfigNomadChange;
import org.terracotta.dynamic_config.api.service.ConsistencyAnalyzer;
import org.terracotta.nomad.client.change.ChangeResultReceiver;
import org.terracotta.nomad.client.recovery.RecoveryResultReceiver;
import org.terracotta.nomad.client.results.DiscoverResultsReceiver;
import org.terracotta.nomad.server.ChangeRequestState;

import java.util.Collection;
import java.util.Map;

public class LockAwareNomadManager<T> implements NomadManager<T> {
  private final String lockToken;
  private final NomadManager<T> underlying;

  public LockAwareNomadManager(String lockToken, NomadManager<T> underlying) {
    this.lockToken = lockToken;
    this.underlying = underlying;
  }

  @Override
  public void runConfigurationDiscovery(Map<Endpoint, LogicalServerState> nodes, DiscoverResultsReceiver<T> results) {
    this.underlying.runConfigurationDiscovery(nodes, results);
  }

  @Override
  public void runClusterActivation(Collection<Endpoint> nodes, Cluster cluster, ChangeResultReceiver<T> results) {
    this.underlying.runClusterActivation(nodes, cluster, results);
  }

  @Override
  public void runConfigurationChange(Cluster destinationCluster, Map<Endpoint, LogicalServerState> onlineNodes,
                                     DynamicConfigNomadChange changes, ChangeResultReceiver<T> results) {
    this.underlying.runConfigurationChange(destinationCluster, onlineNodes, new LockAwareDynamicConfigNomadChange(lockToken, changes), results);
  }

  @Override
  public void runConfigurationRepair(ConsistencyAnalyzer consistencyAnalyzer, RecoveryResultReceiver<T> results, ChangeRequestState forcedState) {
    this.underlying.runConfigurationRepair(consistencyAnalyzer, results, forcedState);
  }

  public NomadManager<T> getUnderlying() {
    return underlying;
  }
}
