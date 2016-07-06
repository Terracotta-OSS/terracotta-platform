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
package org.terracotta.management.entity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.terracotta.connection.Connection;
import org.terracotta.entity.ServiceProviderConfiguration;
import org.terracotta.exception.EntityAlreadyExistsException;
import org.terracotta.exception.EntityNotFoundException;
import org.terracotta.exception.EntityNotProvidedException;
import org.terracotta.exception.EntityVersionMismatchException;
import org.terracotta.management.entity.client.ManagementAgentEntityClientService;
import org.terracotta.management.entity.client.ManagementAgentEntityFactory;
import org.terracotta.management.entity.client.ManagementAgentService;
import org.terracotta.management.entity.server.ManagementAgentEntityServerService;
import org.terracotta.management.model.capabilities.Capability;
import org.terracotta.management.model.cluster.ClientIdentifier;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.context.ContextContainer;
import org.terracotta.management.model.notification.ContextualNotification;
import org.terracotta.management.model.stats.ContextualStatistics;
import org.terracotta.management.model.stats.NumberUnit;
import org.terracotta.management.model.stats.primitive.Counter;
import org.terracotta.management.registry.AbstractManagementRegistry;
import org.terracotta.management.registry.ManagementRegistry;
import org.terracotta.management.service.monitoring.IMonitoringConsumer;
import org.terracotta.management.service.monitoring.MonitoringConsumerConfiguration;
import org.terracotta.management.service.monitoring.MonitoringServiceConfiguration;
import org.terracotta.management.service.monitoring.MonitoringServiceProvider;
import org.terracotta.passthrough.IClusterControl;
import org.terracotta.passthrough.PassthroughClusterControl;
import org.terracotta.passthrough.PassthroughServer;

import java.io.IOException;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * @author Mathieu Carbou
 */
@RunWith(JUnit4.class)
public class ManagementAgentServiceTest {

  private static IMonitoringConsumer consumer;

  IClusterControl stripeControl;

  @Before
  public void setUp() throws Exception {
    PassthroughServer activeServer = new PassthroughServer();
    activeServer.setServerName("server-1");
    activeServer.setBindPort(9510);
    activeServer.setGroupPort(9610);
    activeServer.registerClientEntityService(new ManagementAgentEntityClientService());
    activeServer.registerServerEntityService(new ManagementAgentEntityServerService());
    activeServer.registerServiceProvider(new HackedMonitoringServiceProvider(), new MonitoringServiceConfiguration().setDebug(true));
    stripeControl = new PassthroughClusterControl("server-1", activeServer);
  }

  @After
  public void tearDown() throws Exception {
    stripeControl.tearDown();
  }


  @Test
  public void test_expose() throws EntityNotProvidedException, EntityVersionMismatchException, EntityAlreadyExistsException, EntityNotFoundException, IOException, ExecutionException, InterruptedException {
    ManagementRegistry registry = new AbstractManagementRegistry() {
      @Override
      public ContextContainer getContextContainer() {
        return new ContextContainer("cacheManagerName", "my-cm-name");
      }
    };
    registry.addManagementProvider(new MyManagementProvider());
    registry.register(new MyObject("myCacheManagerName", "myCacheName1"));
    registry.register(new MyObject("myCacheManagerName", "myCacheName2"));

    try (Connection connection = stripeControl.createConnectionToActive()) {

      ManagementAgentService managementAgent = new ManagementAgentService(new ManagementAgentEntityFactory(connection).retrieveOrCreate(new ManagementAgentConfig()));

      ClientIdentifier clientIdentifier = managementAgent.getClientIdentifier();
      System.out.println(clientIdentifier);
      assertEquals(Long.parseLong(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]), clientIdentifier.getPid());
      assertEquals("UNKNOWN", clientIdentifier.getName());
      assertNotNull(clientIdentifier.getConnectionUid());

      managementAgent.setTags("EhcachePounder", "webapp-1", "app-server-node-1");
      managementAgent.setCapabilities(registry.getContextContainer(), registry.getCapabilities());

      ContextualNotification notif = new ContextualNotification(Context.create("key", "value"), "EXPLODED");
      ContextualStatistics stat = new ContextualStatistics("my-capability", Context.create("key", "value"), Collections.singletonMap("my-stat", new Counter(1L, NumberUnit.COUNT)));

      managementAgent.pushNotification(notif);
      managementAgent.pushStatistics(stat, stat);

      Collection<String> names = consumer.getChildNamesForNode(new String[]{"management", "clients"}).get();
      assertEquals(1, names.size());
      assertEquals(clientIdentifier.getClientId(), names.iterator().next());

      names = consumer.getChildNamesForNode(new String[]{"management", "clients", clientIdentifier.getClientId()}).get();
      assertEquals(2, names.size());
      assertThat(names, hasItem("tags"));
      assertThat(names, hasItem("registry"));

      assertArrayEquals(
          new String[]{"EhcachePounder", "webapp-1", "app-server-node-1"},
          consumer.getValueForNode(new String[]{"management", "clients", clientIdentifier.getClientId()}, "tags", String[].class).get());

      Map<String, Object> children = consumer.getChildValuesForNode(new String[]{"management", "clients", clientIdentifier.getClientId()}, "registry").get();
      assertEquals(2, children.size());
      assertArrayEquals(registry.getCapabilities().toArray(new Capability[0]), (Capability[]) children.get("capabilities"));
      assertEquals(registry.getContextContainer(), children.get("contextContainer"));

      BlockingQueue<Serializable[]> notifs = consumer.getValueForNode(new String[]{"management", "notifications"}, BlockingQueue.class).get();
      BlockingQueue<Serializable[]> stats = consumer.getValueForNode(new String[]{"management", "statistics"}, BlockingQueue.class).get();

      assertThat(notifs.poll()[1], equalTo(notif));
      assertThat(stats.poll()[1], equalTo(new ContextualStatistics[]{stat, stat}));
    }
  }

  // to be able to access the IMonitoringConsumer interface outside Voltron
  public static class HackedMonitoringServiceProvider extends MonitoringServiceProvider {
    @Override
    public boolean initialize(ServiceProviderConfiguration configuration) {
      super.initialize(configuration);
      consumer = getService(0, new MonitoringConsumerConfiguration());
      return true;
    }
  }

}
