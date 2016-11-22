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
package org.terracotta.management.entity.tms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.terracotta.connection.ConnectionFactory;
import org.terracotta.management.entity.management.ManagementAgentConfig;
import org.terracotta.management.entity.management.client.ManagementAgentEntityClientService;
import org.terracotta.management.entity.management.client.ManagementAgentEntityFactory;
import org.terracotta.management.entity.management.client.ManagementAgentService;
import org.terracotta.management.entity.management.server.ManagementAgentEntityServerService;
import org.terracotta.management.entity.tms.client.TmsAgentEntity;
import org.terracotta.management.entity.tms.client.TmsAgentEntityClientService;
import org.terracotta.management.entity.tms.client.TmsAgentEntityFactory;
import org.terracotta.management.entity.tms.server.TmsAgentEntityServerService;
import org.terracotta.management.model.capabilities.DefaultCapability;
import org.terracotta.management.model.capabilities.context.CapabilityContext;
import org.terracotta.management.model.capabilities.descriptors.CallDescriptor;
import org.terracotta.management.model.cluster.Client;
import org.terracotta.management.model.cluster.ClientIdentifier;
import org.terracotta.management.model.cluster.Cluster;
import org.terracotta.management.model.cluster.Connection;
import org.terracotta.management.model.cluster.Endpoint;
import org.terracotta.management.model.cluster.Server;
import org.terracotta.management.model.cluster.ServerEntity;
import org.terracotta.management.model.cluster.Stripe;
import org.terracotta.management.model.context.ContextContainer;
import org.terracotta.management.model.message.Message;
import org.terracotta.management.model.notification.ContextualNotification;
import org.terracotta.management.registry.AbstractManagementRegistry;
import org.terracotta.management.registry.ManagementRegistry;
import org.terracotta.passthrough.PassthroughClusterControl;
import org.terracotta.passthrough.PassthroughServer;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Mathieu Carbou
 */
@RunWith(JUnit4.class)
public class TmsAgentTest {

  ExecutorService executorService = Executors.newCachedThreadPool();

  Cluster expectedCluster;
  Client client;
  Connection connection;
  ClientIdentifier clientIdentifier;
  PassthroughClusterControl stripeControl;

  @Before
  public void setUp() throws Exception {
    PassthroughServer activeServer = new PassthroughServer();
    activeServer.setServerName("server-1");
    activeServer.setBindPort(9510);
    activeServer.setGroupPort(9610);
    activeServer.registerServerEntityService(new TmsAgentEntityServerService());
    activeServer.registerClientEntityService(new TmsAgentEntityClientService());
    activeServer.registerClientEntityService(new ManagementAgentEntityClientService());
    activeServer.registerServerEntityService(new ManagementAgentEntityServerService());
    stripeControl = new PassthroughClusterControl("stripe-1", activeServer);

    clientIdentifier = ClientIdentifier.create(
        Long.parseLong(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]),
        InetAddress.getLocalHost().getHostAddress(),
        "UNKNOWN",
        "uuid");

    expectedCluster = Cluster.create()
        .addStripe(Stripe.create("SINGLE")
            .addServer(Server.create("server-1")
                .setBindAddress("0.0.0.0")
                .setBindPort(9510)
                .setGroupPort(9610)
                .setHostName("localhost")
                .setStartTime(0)
                .setActivateTime(0)
                .setHostAddress("127.0.0.1")
                .setVersion("Version Passthrough 5.0.0-SNAPSHOT")
                .setBuildId("Build ID")
                .setState(Server.State.ACTIVE)
                .addServerEntity(ServerEntity.create(getClass().getSimpleName(), TmsAgentEntity.class.getName())
                    .setConsumerId(1)
                    .setManagementRegistry(org.terracotta.management.model.cluster.ManagementRegistry.create(new ContextContainer("consumerId", "1"))
                        .addCapability(new DefaultCapability(
                            "StatisticCollectorCapability",
                            new CapabilityContext(new CapabilityContext.Attribute("consumerId", true)),
                            new CallDescriptor("stopStatisticCollector", "void"),
                            new CallDescriptor("startStatisticCollector", "void"),
                            new CallDescriptor(
                                "updateCollectedStatistics",
                                "void",
                                new CallDescriptor.Parameter("capabilityName", String.class.getName()),
                                new CallDescriptor.Parameter("statisticNames", Collection.class.getName()))
                            )
                        )
                    )
                )
            )
        )
        .addClient(Client.create(clientIdentifier)
            .setHostName(InetAddress.getLocalHost().getHostName()));

    client = expectedCluster.getClients().values().iterator().next();
    connection = Connection.create(
        "uuid",
        expectedCluster.getStripe("SINGLE").get().getServerByName("server-1").get(),
        Endpoint.create(InetAddress.getLocalHost().getHostAddress(), -1) // values set by passthrough system
    );
    client.addConnection(connection);

    assertTrue(connection.fetchServerEntity(getClass().getSimpleName(), TmsAgentConfig.ENTITY_TYPE));
  }

  @After
  public void tearDown() throws Exception {
    executorService.shutdown();
    if (stripeControl != null) {
      stripeControl.tearDown();
    }
  }

  @Test
  public void test_basic_tms_entity() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(SerializationFeature.INDENT_OUTPUT, true);

    try (org.terracotta.connection.Connection connection = ConnectionFactory.connect(URI.create("passthrough://stripe-1:9510/cluster-1"), new Properties())) {

      TmsAgentEntityFactory factory = new TmsAgentEntityFactory(connection, getClass().getSimpleName());
      TmsAgentEntity entity = factory.retrieveOrCreate(new TmsAgentConfig());

      Cluster cluster = entity.readTopology().get();

      // reset runtime data
      expectedCluster.serverStream().forEach(expectedServer -> {
        Server server = cluster.getSingleStripe().getServerByName(expectedServer.getServerName()).get();
        expectedServer.setUpTimeSec(server.getUpTimeSec());
        expectedServer.setStartTime(server.getStartTime());
        expectedServer.setActivateTime(server.getActivateTime());
        expectedServer.setBuildId(server.getBuildId());
      });
      long realConnectionPort = cluster.getClients().values().iterator().next().connectionStream().findFirst().get().getClientEndpoint().getPort();
      String uuid = cluster.getClients().values().iterator().next().getLogicalConnectionUid();
      String expected = mapper.writeValueAsString(expectedCluster.toMap())
          .replace("uuid", uuid)
          .replace(":-1", ":" + realConnectionPort)
          .replace(": -1", ": " + realConnectionPort);

      String actual = mapper.writeValueAsString(cluster.toMap());
      assertEquals(expected, actual);

      List<Message> messages = entity.readMessages().get();
      assertEquals(4, messages.size());

      // ensure a second read without any topology modifications leads to 0 messages
      assertEquals(0, entity.readMessages().get().size());

      assertEquals("TOPOLOGY", messages.get(messages.size() - 1).getType());
      assertEquals(cluster, messages.get(messages.size() - 1).unwrap(Cluster.class).get(0));

      assertEquals("NOTIFICATION", messages.get(0).getType());
      ContextualNotification firstNotif = messages.get(0).unwrap(ContextualNotification.class).get(0);
      assertEquals("SERVER_ENTITY_CREATED", firstNotif.getType());
      assertEquals(expectedCluster.serverEntityStream().findFirst().get().getContext(), firstNotif.getContext());

      assertEquals("NOTIFICATION", messages.get(1).getType());
      ContextualNotification secondNotif = messages.get(1).unwrap(ContextualNotification.class).get(0);
      assertEquals("ENTITY_REGISTRY_AVAILABLE", secondNotif.getType());

      ContextualNotification thirdNotif = messages.get(2).unwrap(ContextualNotification.class).get(0);
      assertEquals("SERVER_ENTITY_FETCHED", thirdNotif.getType());
      assertEquals(expectedCluster.serverEntityStream().findFirst().get().getContext(), firstNotif.getContext());
      assertEquals(
          expectedCluster.clientStream().findFirst().get().getClientId().replace("uuid", uuid),
          thirdNotif.getAttributes().get(Client.KEY));

      entity.readMessages().get();

      // not connects a client management registry

      ManagementRegistry registry = new AbstractManagementRegistry() {
        @Override
        public ContextContainer getContextContainer() {
          return new ContextContainer("cacheManagerName", "my-cm-name");
        }
      };
      registry.addManagementProvider(new MyManagementProvider());

      try (org.terracotta.connection.Connection secondConnection = ConnectionFactory.connect(URI.create("passthrough://stripe-1:9510/cluster-1"), new Properties())) {

        ManagementAgentService managementAgent = new ManagementAgentService(new ManagementAgentEntityFactory(secondConnection).retrieveOrCreate(new ManagementAgentConfig()));
        managementAgent.setManagementCallExecutor(executorService);
        managementAgent.setManagementRegistry(registry);
        managementAgent.init();

        ClientIdentifier clientIdentifier = managementAgent.getClientIdentifier();
        assertEquals(Long.parseLong(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]), clientIdentifier.getPid());
        assertEquals("UNKNOWN", clientIdentifier.getName());
        assertNotNull(clientIdentifier.getConnectionUid());

        managementAgent.setTags("EhcachePounder", "webapp-1", "app-server-node-1");

        messages = entity.readMessages().get();
        assertEquals(6, messages.size());
        for (int i = 0; i < 5; i++) {
          assertEquals("NOTIFICATION", messages.get(0).getType());
        }
        assertEquals("CLIENT_CONNECTED", messages.get(0).unwrap(ContextualNotification.class).get(0).getType());
        assertEquals("SERVER_ENTITY_CREATED", messages.get(1).unwrap(ContextualNotification.class).get(0).getType());
        assertEquals("SERVER_ENTITY_FETCHED", messages.get(2).unwrap(ContextualNotification.class).get(0).getType());
        assertEquals("CLIENT_REGISTRY_AVAILABLE", messages.get(3).unwrap(ContextualNotification.class).get(0).getType());
        assertEquals("CLIENT_TAGS_UPDATED", messages.get(4).unwrap(ContextualNotification.class).get(0).getType());
        assertEquals("TOPOLOGY", messages.get(5).getType());

        registry.register(new MyObject("myCacheManagerName", "myCacheName1"));
        registry.register(new MyObject("myCacheManagerName", "myCacheName2"));

        messages = entity.readMessages().get();
        assertEquals(2, messages.size());
        assertEquals("NOTIFICATION", messages.get(0).getType());
        assertEquals("TOPOLOGY", messages.get(1).getType());
        assertEquals("CLIENT_REGISTRY_UPDATED", messages.get(0).unwrap(ContextualNotification.class).get(0).getType());
        assertEquals("TOPOLOGY", messages.get(1).getType());
      }

    }
  }

}
