/*
 * Copyright Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.terracotta.management.entity.monitoringconsumer;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.terracotta.connection.Connection;
import org.terracotta.management.entity.monitoringconsumer.client.MonitoringConsumerEntity;
import org.terracotta.management.entity.monitoringconsumer.client.MonitoringConsumerEntityClientService;
import org.terracotta.management.entity.monitoringconsumer.client.MonitoringConsumerEntityFactory;
import org.terracotta.management.entity.monitoringconsumer.server.MonitoringConsumerEntityServerService;
import org.terracotta.management.service.monitoring.MonitoringServiceConfiguration;
import org.terracotta.management.service.monitoring.MonitoringServiceProvider;
import org.terracotta.passthrough.IClusterControl;
import org.terracotta.passthrough.PassthroughClusterControl;
import org.terracotta.passthrough.PassthroughServer;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.terracotta.monitoring.PlatformMonitoringConstants.SERVERS_PATH;

/**
 * @author Mathieu Carbou
 */
@RunWith(JUnit4.class)
public class MonitoringConsumerTest {

  IClusterControl stripeControl;

  @Before
  public void setUp() throws Exception {
    PassthroughServer activeServer = new PassthroughServer(true);

    activeServer.registerServerEntityService(new MonitoringConsumerEntityServerService());
    activeServer.registerClientEntityService(new MonitoringConsumerEntityClientService());

    activeServer.registerServiceProvider(new MonitoringServiceProvider(), new MonitoringServiceConfiguration().setDebug(true));

    activeServer.start();
    stripeControl = new PassthroughClusterControl("server-1", activeServer, null);
  }

  @After
  public void tearDown() throws Exception {
    stripeControl.tearDown();
  }

  @Test
  public void test_read_tree() throws Exception {
    try (Connection connection = stripeControl.createConnectionToActive()) {

      // create, fetch and use the custom entity

      MonitoringConsumerEntityFactory monitoringConsumerEntityFactory = new MonitoringConsumerEntityFactory(connection);
      MonitoringConsumerEntity entity = monitoringConsumerEntityFactory.retrieveOrCreate(getClass().getSimpleName());

      Collection<String> serverNodeIds = entity.getChildNamesForNode(SERVERS_PATH);
      System.out.println(serverNodeIds);

      try {
        entity.getValueForNode(SERVERS_PATH, serverNodeIds.iterator().next());
        fail();
      } catch (Exception e) {
        assertEquals(UndeclaredThrowableException.class, e.getClass());
      }
    }
  }

}
