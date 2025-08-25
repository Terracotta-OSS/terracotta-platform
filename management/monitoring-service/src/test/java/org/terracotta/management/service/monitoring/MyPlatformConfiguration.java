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
package org.terracotta.management.service.monitoring;

import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.Testing;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.entity.PlatformConfiguration;

import java.util.Collection;
import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.terracotta.dynamic_config.api.model.Testing.N_UIDS;
import static org.terracotta.dynamic_config.api.model.Testing.newTestCluster;

public class MyPlatformConfiguration implements PlatformConfiguration {

  private final String serverName;
  private final String host;
  private final int port;

  private final NodeContext topology = new NodeContext(newTestCluster("my-cluster", new Stripe()
      .setName("stripe[0]")
      .addNode(Testing.newTestNode("bar", "localhost"))), N_UIDS[1]);
  private final TopologyService topologyService = mock(TopologyService.class);

  public MyPlatformConfiguration(String serverName, String host, int port) {
    this.serverName = serverName;
    this.host = host;
    this.port = port;

    when(topologyService.getRuntimeNodeContext()).thenReturn(topology);
  }

  @Override
  public String getServerName() {
    return serverName;
  }

  @Override
  public String getHost() {
    return host;
  }

  @Override
  public int getTsaPort() {
    return port;
  }

  @Override
  public <T> Collection<T> getExtendedConfiguration(Class<T> aClass) {
    if (TopologyService.class == aClass) {
      return Collections.singletonList(aClass.cast(topologyService));
    }
    return Collections.emptyList();
  }
}


