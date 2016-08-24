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
import org.terracotta.connection.entity.EntityRef;
import org.terracotta.management.entity.tms.TmsAgentConfig;
import org.terracotta.management.entity.tms.TmsAgentVersion;
import org.terracotta.management.model.cluster.Client;
import org.terracotta.management.model.cluster.ClientIdentifier;
import org.terracotta.management.model.cluster.Cluster;
import org.terracotta.management.model.cluster.Connection;
import org.terracotta.management.model.cluster.Endpoint;
import org.terracotta.management.model.cluster.Server;
import org.terracotta.management.model.cluster.ServerEntity;
import org.terracotta.management.model.cluster.ServerState;
import org.terracotta.management.model.cluster.Stripe;
import org.terracotta.management.model.message.Message;
import org.terracotta.management.model.notification.ContextualNotification;
import org.terracotta.management.entity.tms.client.TmsAgentEntity;
import org.terracotta.management.entity.tms.client.TmsAgentEntityClientService;
import org.terracotta.management.entity.tms.server.TmsAgentEntityServerService;
import org.terracotta.passthrough.PassthroughClusterControl;
import org.terracotta.passthrough.PassthroughServer;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.URI;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

/**
 * @author Mathieu Carbou
 */
@RunWith(JUnit4.class)
public class TmsAgentTest {

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
    stripeControl = new PassthroughClusterControl("server-1", activeServer);

    clientIdentifier = ClientIdentifier.create(
        Long.parseLong(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]),
        InetAddress.getLocalHost().getHostAddress(),
        "UNKNOWN",
        "uuid");

    expectedCluster = Cluster.create()
        .addStripe(Stripe.create("stripe-1")
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
                .setState(ServerState.ACTIVE)
                .addServerEntity(ServerEntity.create(getClass().getSimpleName(), TmsAgentEntity.class.getName()))))
        .addClient(Client.create(clientIdentifier)
            .setHostName(InetAddress.getLocalHost().getHostName()));

    client = expectedCluster.getClients().values().iterator().next();
    connection = Connection.create(
        "uuid",
        expectedCluster.getStripe("stripe-1").get().getServerByName("server-1").get(),
        Endpoint.create(InetAddress.getLocalHost().getHostAddress(), -1) // values set by passthrough system
    );
    client.addConnection(connection);

    connection.fetchServerEntity(expectedCluster.getStripe("stripe-1").get()
        .getServerByName("server-1").get()
        .getServerEntity("TmsAgentTest:" + TmsAgentConfig.ENTITY_TYPE).get());
  }

  @After
  public void tearDown() throws Exception {
    stripeControl.tearDown();
  }

  @Test
  public void test_basic_tms_entity() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(SerializationFeature.INDENT_OUTPUT, true);

    try (org.terracotta.connection.Connection connection = ConnectionFactory.connect(URI.create("passthrough://server-1:9510/cluster-1"), new Properties())) {
      EntityRef<TmsAgentEntity, TmsAgentConfig> ref = connection.getEntityRef(TmsAgentEntity.class, TmsAgentVersion.LATEST.version(), getClass().getSimpleName());
      ref.create(new TmsAgentConfig());

      TmsAgentEntity entity = ref.fetchEntity();

      Cluster cluster = entity.readTopology().get();

      // reset runtime data
      expectedCluster.serverStream().forEach(expectedServer -> {
        Server server = cluster.getStripe("stripe-1").get().getServerByName(expectedServer.getServerName()).get();
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

      System.out.println("EXPECTED:");
      System.out.println(expected);

      String actual = mapper.writeValueAsString(cluster.toMap());
      System.out.println("ACTUAL");
      System.out.println(actual);

      assertEquals(expected, actual);

      List<Message> messages = entity.readMessages().get();
      assertEquals(3, messages.size());

      // ensure a second read without any topology modifications leads to 0 messages
      assertEquals(0, entity.readMessages().get().size());

      System.out.println(messages.stream().map(Message::toString).collect(Collectors.joining("\n")));

      assertEquals("TOPOLOGY", messages.get(0).getType());
      assertEquals(cluster, messages.get(0).unwrap(Cluster.class));

      assertEquals("NOTIFICATION", messages.get(1).getType());
      ContextualNotification firstNotif = messages.get(1).unwrap(ContextualNotification.class);
      assertEquals("SERVER_ENTITY_CREATED", firstNotif.getType());
      assertEquals(expectedCluster.serverEntityStream().findFirst().get().getContext(), firstNotif.getContext());

      assertEquals("NOTIFICATION", messages.get(2).getType());
      ContextualNotification secondNotif = messages.get(2).unwrap(ContextualNotification.class);
      assertEquals("SERVER_ENTITY_FETCHED", secondNotif.getType());
      assertEquals(expectedCluster.serverEntityStream().findFirst().get().getContext(), firstNotif.getContext());
      assertEquals(
          expectedCluster.clientStream().findFirst().get().getClientId().replace("uuid", uuid),
          secondNotif.getAttributes().get(Client.KEY));
    }
  }

}
