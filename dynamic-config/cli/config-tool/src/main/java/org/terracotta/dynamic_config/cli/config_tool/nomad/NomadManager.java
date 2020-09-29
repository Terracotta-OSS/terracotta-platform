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
import org.terracotta.dynamic_config.api.service.ConsistencyAnalyzer;
import org.terracotta.nomad.client.change.ChangeResultReceiver;
import org.terracotta.nomad.client.recovery.RecoveryResultReceiver;
import org.terracotta.nomad.client.results.DiscoverResultsReceiver;
import org.terracotta.nomad.server.ChangeRequestState;

import java.util.Collection;
import java.util.Map;

public interface NomadManager<T> {
  void runConfigurationDiscovery(Map<Endpoint, LogicalServerState> nodes, DiscoverResultsReceiver<T> results);

  void runClusterActivation(Collection<Endpoint> nodes, Cluster cluster, ChangeResultReceiver<T> results);

  void runConfigurationChange(Cluster destinationCluster, Map<Endpoint, LogicalServerState> onlineNodes, DynamicConfigNomadChange changes, ChangeResultReceiver<T> results);

  void runConfigurationRepair(ConsistencyAnalyzer consistencyAnalyzer, RecoveryResultReceiver<T> results, ChangeRequestState forcedState);
}
