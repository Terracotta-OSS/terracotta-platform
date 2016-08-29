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
package org.terracotta.management.entity.monitoring;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionFactory;
import org.terracotta.management.entity.monitoring.client.MonitoringServiceEntity;
import org.terracotta.management.entity.monitoring.client.MonitoringServiceEntityClientService;
import org.terracotta.management.entity.monitoring.client.MonitoringServiceEntityFactory;
import org.terracotta.management.entity.monitoring.server.MonitoringServiceEntityServerService;
import org.terracotta.monitoring.PlatformServer;
import org.terracotta.passthrough.PassthroughClusterControl;
import org.terracotta.passthrough.PassthroughServer;

import java.net.URI;
import java.util.Collection;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.terracotta.monitoring.PlatformMonitoringConstants.SERVERS_PATH;

/**
 * @author Mathieu Carbou
 */
@RunWith(JUnit4.class)
public class MonitoringServiceTest {

  PassthroughClusterControl stripeControl;

  @Before
  public void setUp() throws Exception {
    PassthroughServer activeServer = new PassthroughServer();

    activeServer.registerServerEntityService(new MonitoringServiceEntityServerService());
    activeServer.registerClientEntityService(new MonitoringServiceEntityClientService());

    stripeControl = new PassthroughClusterControl("server-1", activeServer);
  }

  @After
  public void tearDown() throws Exception {
    stripeControl.tearDown();
  }

  @Test
  public void test_read_tree() throws Exception {
    try (Connection connection = ConnectionFactory.connect(URI.create("passthrough://server-1:9510/cluster-1"), new Properties())) {

      // create, fetch and use the custom entity

      MonitoringServiceEntityFactory monitoringServiceEntityFactory = new MonitoringServiceEntityFactory(connection);
      MonitoringServiceEntity entity = monitoringServiceEntityFactory.retrieveOrCreate(getClass().getSimpleName());

      Collection<String> serverNodeIds = entity.getChildNamesForNode(SERVERS_PATH);

      Object o = entity.getValueForNode(SERVERS_PATH, serverNodeIds.iterator().next());
      System.out.println(o);
      assertThat(o, instanceOf(PlatformServer.class));
    }
  }

}
