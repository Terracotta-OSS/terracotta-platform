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
package org.terracotta.dynamic_config.test_support.handler;

import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Configuration;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.server.api.ConfigChangeHandler;
import org.terracotta.dynamic_config.server.api.InvalidConfigChangeException;

public class GroupPortSimulateHandler implements ConfigChangeHandler {
  @Override
  public void validate(NodeContext nodeContext, Configuration change) throws InvalidConfigChangeException {
    Cluster cluster = nodeContext.getCluster();
    String configVal = change.getValue().get();
    String[] tmpConfig = configVal.split("#");
    String serverName = tmpConfig[0];
    String groupPort = tmpConfig[1];
    Node node = cluster.getNodeByName(serverName).get();
    node.setGroupPort(Integer.parseInt(groupPort));
    //TODO [DYNAMIC-CONFIG]: no return anymore. So find another way to hack the config repo xml file to update the bind port. Idea: directly update the xml file written on disk (server section only)
  }
}
