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
package org.terracotta.management.entity.management;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionFactory;
import org.terracotta.management.entity.management.client.ManagementAgentEntityClientService;
import org.terracotta.management.entity.management.client.ManagementAgentEntityFactory;
import org.terracotta.management.entity.management.client.ManagementAgentService;
import org.terracotta.management.entity.management.server.ManagementAgentEntityServerService;
import org.terracotta.management.entity.monitoring.client.MonitoringServiceEntity;
import org.terracotta.management.entity.monitoring.client.MonitoringServiceEntityClientService;
import org.terracotta.management.entity.monitoring.client.MonitoringServiceEntityFactory;
import org.terracotta.management.entity.monitoring.server.MonitoringServiceEntityServerService;
import org.terracotta.management.model.call.ContextualReturn;
import org.terracotta.management.model.call.Parameter;
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
import org.terracotta.passthrough.PassthroughClusterControl;
import org.terracotta.passthrough.PassthroughServer;

import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Mathieu Carbou
 */
@RunWith(JUnit4.class)
public class ManagementAgentServiceTest {

  private static MonitoringServiceEntity consumer;

  PassthroughClusterControl stripeControl;

  ExecutorService executorService = Executors.newCachedThreadPool();

  @Before
  public void setUp() throws Exception {
    PassthroughServer activeServer = new PassthroughServer();
    activeServer.setServerName("server-1");
    activeServer.setBindPort(9510);
    activeServer.setGroupPort(9610);
    activeServer.registerClientEntityService(new ManagementAgentEntityClientService());
    activeServer.registerServerEntityService(new ManagementAgentEntityServerService());
    activeServer.registerServerEntityService(new MonitoringServiceEntityServerService());
    activeServer.registerClientEntityService(new MonitoringServiceEntityClientService());
    stripeControl = new PassthroughClusterControl("server-1", activeServer);

    consumer = new MonitoringServiceEntityFactory(ConnectionFactory.connect(URI.create("passthrough://server-1:9510/cluster-1"), new Properties())).retrieveOrCreate("MonitoringConsumerEntity");
  }

  @After
  public void tearDown() throws Exception {
    executorService.shutdown();
    if (stripeControl != null) {
      stripeControl.tearDown();
    }
  }


  @Test(timeout = 5000)
  public void test_expose() throws Exception {
    ManagementRegistry registry = new AbstractManagementRegistry() {
      @Override
      public ContextContainer getContextContainer() {
        return new ContextContainer("cacheManagerName", "my-cm-name");
      }
    };
    registry.addManagementProvider(new MyManagementProvider());
    registry.register(new MyObject("myCacheManagerName", "myCacheName1"));
    registry.register(new MyObject("myCacheManagerName", "myCacheName2"));

    consumer.createBestEffortBuffer("client-notifications", 100, Serializable[].class);
    consumer.createBestEffortBuffer("client-statistics", 100, Serializable[].class);

    try (Connection connection = ConnectionFactory.connect(URI.create("passthrough://server-1:9510/cluster-1"), new Properties())) {

      ManagementAgentService managementAgent = new ManagementAgentService(new ManagementAgentEntityFactory(connection).retrieveOrCreate(new ManagementAgentConfig()));
      managementAgent.setManagementCallExecutor(executorService);

      ClientIdentifier clientIdentifier = managementAgent.getClientIdentifier();
      //System.out.println(clientIdentifier);
      assertEquals(Long.parseLong(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]), clientIdentifier.getPid());
      assertEquals("UNKNOWN", clientIdentifier.getName());
      assertNotNull(clientIdentifier.getConnectionUid());

      managementAgent.bridge(registry);
      assertThat(consumer.readBuffer("client-notifications", Serializable[].class)[1], equalTo(new ContextualNotification(Context.create("clientId", clientIdentifier.getClientId()), "CLIENT_REGISTRY_UPDATED")));

      managementAgent.setTags("EhcachePounder", "webapp-1", "app-server-node-1");
      assertThat(consumer.readBuffer("client-notifications", Serializable[].class)[1], equalTo(new ContextualNotification(Context.create("clientId", clientIdentifier.getClientId()), "CLIENT_TAGS_UPDATED")));

      ContextualNotification notif = new ContextualNotification(Context.create("key", "value"), "EXPLODED");
      ContextualStatistics stat = new ContextualStatistics("my-capability", Context.create("key", "value"), Collections.singletonMap("my-stat", new Counter(1L, NumberUnit.COUNT)));

      managementAgent.pushNotification(notif);
      managementAgent.pushStatistics(stat, stat);

      long consumerId = consumer.getConsumerId(ManagementAgentConfig.ENTITY_TYPE, ManagementAgentEntityFactory.ENTITYNAME);

      Collection<String> names = consumer.getChildNamesForNode(consumerId, "management", "clients");
      assertEquals(1, names.size());
      assertEquals(clientIdentifier.getClientId(), names.iterator().next());

      names = consumer.getChildNamesForNode(consumerId, "management", "clients", clientIdentifier.getClientId());
      assertEquals(2, names.size());
      assertThat(names, hasItem("tags"));
      assertThat(names, hasItem("registry"));

      assertArrayEquals(
          new String[]{"EhcachePounder", "webapp-1", "app-server-node-1"},
          consumer.getValueForNode(consumerId, new String[]{"management", "clients", clientIdentifier.getClientId()}, "tags", String[].class));

      Map<String, Serializable> children = consumer.getChildValuesForNode(consumerId, new String[]{"management", "clients", clientIdentifier.getClientId()}, "registry");
      assertEquals(2, children.size());
      assertArrayEquals(registry.getCapabilities().toArray(new Capability[0]), (Capability[]) children.get("capabilities"));
      assertEquals(registry.getContextContainer(), children.get("contextContainer"));

      assertThat(consumer.readBuffer("client-notifications", Serializable[].class)[1], equalTo(notif));
      assertThat(consumer.readBuffer("client-statistics", Serializable[].class)[1], equalTo(new ContextualStatistics[]{stat, stat}));

      runManagementCallFromAnotherClient(clientIdentifier);
    }
  }

  private void runManagementCallFromAnotherClient(ClientIdentifier targetClientIdentifier) throws Exception {
    try (Connection managementConnection = ConnectionFactory.connect(URI.create("passthrough://server-1:9510/cluster-1"), new Properties())) {
      ManagementAgentService agent = new ManagementAgentService(new ManagementAgentEntityFactory(managementConnection).retrieveOrCreate(new ManagementAgentConfig()));
      agent.setManagementCallExecutor(executorService);

      assertThat(agent.getManageableClients().size(), equalTo(2));
      assertThat(agent.getManageableClients(), hasItem(targetClientIdentifier));
      assertThat(agent.getManageableClients(), hasItem(agent.getClientIdentifier()));

      AtomicReference<String> managementCallId = new AtomicReference<>();
      BlockingQueue<ContextualReturn<?>> returns = new LinkedBlockingQueue<>();

      agent.setContextualReturnListener((from, id, aReturn) -> {
        assertEquals(targetClientIdentifier, from);
        assertEquals(managementCallId.get(), id);
        returns.offer(aReturn);
      });

      managementCallId.set(agent.call(
          targetClientIdentifier,
          Context.empty()
              .with("cacheManagerName", "myCacheManagerName")
              .with("cacheName", "myCacheName1"),
          "TheActionProvider",
          "incr",
          int.class,
          new Parameter(Integer.MAX_VALUE, "int")));

      ContextualReturn<?> ret = returns.take();
      assertThat(ret.hasExecuted(), is(true));
      try {
        ret.getValue();
        fail();
      } catch (Exception e) {
        assertThat(e, instanceOf(ExecutionException.class));
        assertThat(e.getCause(), instanceOf(IllegalArgumentException.class));
      }

      managementCallId.set(agent.call(
          targetClientIdentifier,
          Context.empty()
              .with("cacheManagerName", "myCacheManagerName")
              .with("cacheName", "myCacheName1"),
          "TheActionProvider",
          "incr",
          int.class,
          new Parameter(1, "int")));

      ret = returns.take();
      assertThat(ret.hasExecuted(), is(true));
      assertThat(ret.getValue(), equalTo(2));
    }
  }

}
