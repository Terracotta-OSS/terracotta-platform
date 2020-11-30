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
package org.terracotta.dynamic_config.cli.api.command;

import org.terracotta.diagnostic.model.LogicalServerState;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.LockContext;
import org.terracotta.dynamic_config.api.model.Node.Endpoint;

import java.net.InetSocketAddress;
import java.util.LinkedHashMap;
import java.util.Map;

public class LockConfigCommand extends RemoteCommand {

  private InetSocketAddress node;
  private String lockContext;

  public void setNode(InetSocketAddress node) {
    this.node = node;
  }

  public void setLockContext(String lockContext) {
    this.lockContext = lockContext;
  }

  @Override
  public final void run() {
    Map<Endpoint, LogicalServerState> allNodes = findRuntimePeersStatus(node);
    LinkedHashMap<Endpoint, LogicalServerState> onlineNodes = filterOnlineNodes(allNodes);
    Cluster cluster = getRuntimeCluster(node);
    lock(cluster, onlineNodes, LockContext.from(lockContext));
  }
}