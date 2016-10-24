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
import org.terracotta.management.entity.monitoring.client.MonitoringServiceEntityClientService;
import org.terracotta.management.entity.monitoring.client.MonitoringServiceEntityFactory;
import org.terracotta.management.entity.monitoring.client.MonitoringServiceProxyEntity;
import org.terracotta.management.entity.monitoring.server.MonitoringServiceEntityServerService;
import org.terracotta.management.model.call.ContextualReturn;
import org.terracotta.management.model.call.Parameter;
import org.terracotta.management.model.cluster.Client;
import org.terracotta.management.model.cluster.ClientIdentifier;
import org.terracotta.management.model.cluster.Cluster;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.context.ContextContainer;
import org.terracotta.management.model.message.Message;
import org.terracotta.management.model.notification.ContextualNotification;
import org.terracotta.management.model.stats.ContextualStatistics;
import org.terracotta.management.model.stats.NumberUnit;
import org.terracotta.management.model.stats.primitive.Counter;
import org.terracotta.management.registry.AbstractManagementRegistry;
import org.terracotta.management.registry.ManagementRegistry;
import org.terracotta.passthrough.PassthroughClusterControl;
import org.terracotta.passthrough.PassthroughServer;

import java.lang.management.ManagementFactory;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Mathieu Carbou
 */
@RunWith(JUnit4.class)
public class ManagementAgentServiceTest {

  private static MonitoringServiceProxyEntity consumer;

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
    stripeControl = new PassthroughClusterControl("stripe-1", activeServer);

    consumer = new MonitoringServiceEntityFactory(ConnectionFactory.connect(URI.create("passthrough://stripe-1:9510/cluster-1"), new Properties())).retrieveOrCreate("MonitoringConsumerEntity");
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

    consumer.createMessageBuffer(100);

    try (Connection connection = ConnectionFactory.connect(URI.create("passthrough://stripe-1:9510/cluster-1"), new Properties())) {

      ManagementAgentService managementAgent = new ManagementAgentService(new ManagementAgentEntityFactory(connection).retrieveOrCreate(new ManagementAgentConfig()));
      managementAgent.setManagementCallExecutor(executorService);

      ClientIdentifier clientIdentifier = managementAgent.getClientIdentifier();
      //System.out.println(clientIdentifier);
      assertEquals(Long.parseLong(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]), clientIdentifier.getPid());
      assertEquals("UNKNOWN", clientIdentifier.getName());
      assertNotNull(clientIdentifier.getConnectionUid());

      managementAgent.bridge(registry);

      List<Message> messages = consumer.drainMessageBuffer();
      assertThat(types(messages), equalTo(Arrays.asList("NOTIFICATION", "NOTIFICATION", "NOTIFICATION", "NOTIFICATION")));
      assertThat(notificationTypes(messages), equalTo(Arrays.asList("CLIENT_CONNECTED", "SERVER_ENTITY_CREATED", "SERVER_ENTITY_FETCHED", "CLIENT_REGISTRY_AVAILABLE")));
      assertThat(messages.get(0).unwrap(ContextualNotification.class).get(0).getContext().get(Client.KEY), equalTo(clientIdentifier.getClientId()));
      assertThat(messages.get(3).unwrap(ContextualNotification.class).get(0).getContext().get(Client.KEY), equalTo(clientIdentifier.getClientId()));

      managementAgent.setTags("EhcachePounder", "webapp-1", "app-server-node-1");

      messages = consumer.drainMessageBuffer();
      assertThat(messages.size(), equalTo(1));
      assertThat(types(messages), equalTo(Collections.singletonList("NOTIFICATION")));
      assertThat(notificationTypes(messages), equalTo(Collections.singletonList("CLIENT_TAGS_UPDATED")));
      assertThat(messages.get(0).unwrap(ContextualNotification.class).get(0).getContext().get(Client.KEY), equalTo(clientIdentifier.getClientId()));

      ContextualNotification notif = new ContextualNotification(Context.create("key", "value"), "EXPLODED");
      ContextualStatistics stat = new ContextualStatistics("my-capability", Context.create("key", "value"), Collections.singletonMap("my-stat", new Counter(1L, NumberUnit.COUNT)));

      managementAgent.pushNotification(notif);
      managementAgent.pushStatistics(stat, stat);

      Cluster cluster = consumer.readTopology();
      assertThat(cluster.getClientCount(), equalTo(2));

      Client me = cluster.getClient(managementAgent.getClientIdentifier()).get();
      assertThat(me.getTags(), hasItems("EhcachePounder", "webapp-1", "app-server-node-1"));
      assertThat(me.getManagementRegistry().isPresent(), is(true));
      assertThat(registry.getCapabilities(), equalTo(me.getManagementRegistry().get().getCapabilities()));
      assertThat(registry.getContextContainer(), equalTo(me.getManagementRegistry().get().getContextContainer()));

      assertThat(consumer.readMessageBuffer().unwrap(ContextualNotification.class).get(0), equalTo(notif));
      assertThat(consumer.readMessageBuffer().unwrap(ContextualStatistics.class), equalTo(Arrays.asList(stat, stat)));

      runManagementCallFromAnotherClient(clientIdentifier);
    }
  }

  private void runManagementCallFromAnotherClient(ClientIdentifier targetClientIdentifier) throws Exception {
    try (Connection managementConnection = ConnectionFactory.connect(URI.create("passthrough://stripe-1:9510/cluster-1"), new Properties())) {
      ManagementAgentService agent = new ManagementAgentService(new ManagementAgentEntityFactory(managementConnection).retrieveOrCreate(new ManagementAgentConfig()));
      agent.setManagementCallExecutor(executorService);

      assertThat(agent.getManageableClients().size(), equalTo(1));
      assertThat(agent.getManageableClients(), hasItem(targetClientIdentifier));

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

  private static List<String> types(List<Message> messages) {
    return messages.stream()
        .map(Message::getType)
        .collect(Collectors.toList());
  }

  private static List<String> notificationTypes(List<Message> messages) {
    return messages.stream()
        .filter(message -> message.getType().equals("NOTIFICATION"))
        .flatMap(message -> message.unwrap(ContextualNotification.class).stream())
        .map(ContextualNotification::getType)
        .collect(Collectors.toList());
  }

}
