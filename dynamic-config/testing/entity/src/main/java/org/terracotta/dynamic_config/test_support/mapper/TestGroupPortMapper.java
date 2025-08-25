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
package org.terracotta.dynamic_config.test_support.mapper;

import com.tc.classloader.OverrideService;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.server.api.GroupPortMapper;

@OverrideService("org.terracotta.dynamic_config.server.configuration.startup.DefaultGroupPortMapperImpl")
public class TestGroupPortMapper implements GroupPortMapper {
  private static final String key = "test-proxy-group-port";

  @Override
  public int getPeerGroupPort(Node peerNode, Node thisNode) {
    if (thisNode.getTcProperties().orDefault().containsKey(key)) {
      String proxyGroupPorts = thisNode.getTcProperties().orDefault().get(key);
      String[] nodeIdGroupPorts = proxyGroupPorts.split("#");
      for (int i = 0; i < nodeIdGroupPorts.length; ++i) {
        nodeIdGroupPorts[i] = nodeIdGroupPorts[i].replaceAll("\"", "");
        String[] idPortPair = nodeIdGroupPorts[i].split("->");
        if (idPortPair[0].equals(peerNode.getName())) {
          return Integer.parseInt(idPortPair[1]);
        }
      }
    }
    return peerNode.getGroupPort().orDefault();
  }
}
