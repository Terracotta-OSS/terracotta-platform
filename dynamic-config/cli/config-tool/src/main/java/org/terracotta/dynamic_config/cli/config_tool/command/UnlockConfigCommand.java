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
package org.terracotta.dynamic_config.cli.config_tool.command;

import org.terracotta.diagnostic.model.LogicalServerState;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Node.Endpoint;
import org.terracotta.dynamic_config.cli.command.Usage;
import org.terracotta.dynamic_config.cli.converter.InetSocketAddressConverter;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import java.net.InetSocketAddress;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

@Parameters(commandNames = "unlock-config", commandDescription = "Unlocks the config", hidden = true)
@Usage("unlock-config -s <hostname[:port]>")
public class UnlockConfigCommand extends RemoteCommand {

  @Parameter(names = {"-s"}, description = "Node to connect to", required = true, converter = InetSocketAddressConverter.class)
  InetSocketAddress node;

  @Override
  public void validate() {
    requireNonNull(node);
  }

  @Override
  public final void run() {
    Map<Endpoint, LogicalServerState> allNodes = findRuntimePeersStatus(node);
    LinkedHashMap<Endpoint, LogicalServerState> onlineNodes = filterOnlineNodes(allNodes);
    Cluster cluster = getRuntimeCluster(node);
    unlock(cluster, onlineNodes);
  }
}