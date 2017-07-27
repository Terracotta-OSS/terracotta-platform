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
package org.terracotta.management.service.monitoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;
import org.terracotta.entity.BasicServiceConfiguration;
import org.terracotta.entity.ClientCommunicator;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.ClientSourceId;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.ServiceRegistry;
import org.terracotta.management.model.call.ContextualReturn;
import org.terracotta.management.model.capabilities.DefaultCapability;
import org.terracotta.management.model.capabilities.context.CapabilityContext;
import org.terracotta.management.model.capabilities.descriptors.CallDescriptor;
import org.terracotta.management.model.cluster.Client;
import org.terracotta.management.model.cluster.ClientIdentifier;
import org.terracotta.management.model.cluster.Cluster;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.context.ContextContainer;
import org.terracotta.management.model.message.Message;
import org.terracotta.management.model.notification.ContextualNotification;
import org.terracotta.management.model.stats.ContextualStatistics;
import org.terracotta.monitoring.IMonitoringProducer;
import org.terracotta.monitoring.IStripeMonitoring;
import org.terracotta.monitoring.PlatformClientFetchedEntity;
import org.terracotta.monitoring.PlatformConnectedClient;
import org.terracotta.monitoring.PlatformEntity;
import org.terracotta.monitoring.PlatformServer;
import org.terracotta.monitoring.ServerState;

import java.io.File;
import java.io.Serializable;
import java.net.InetAddress;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.terracotta.management.service.monitoring.DefaultDataListener.TOPIC_SERVER_ENTITY_NOTIFICATION;
import static org.terracotta.management.service.monitoring.DefaultDataListener.TOPIC_SERVER_ENTITY_STATISTICS;
import static org.terracotta.monitoring.PlatformMonitoringConstants.CLIENTS_PATH;
import static org.terracotta.monitoring.PlatformMonitoringConstants.CLIENTS_ROOT_NAME;
import static org.terracotta.monitoring.PlatformMonitoringConstants.ENTITIES_PATH;
import static org.terracotta.monitoring.PlatformMonitoringConstants.ENTITIES_ROOT_NAME;
import static org.terracotta.monitoring.PlatformMonitoringConstants.FETCHED_PATH;
import static org.terracotta.monitoring.PlatformMonitoringConstants.FETCHED_ROOT_NAME;
import static org.terracotta.monitoring.PlatformMonitoringConstants.PLATFORM_PATH;
import static org.terracotta.monitoring.PlatformMonitoringConstants.PLATFORM_ROOT_NAME;
import static org.terracotta.monitoring.PlatformMonitoringConstants.STATE_NODE_NAME;

/**
 * @author Mathieu Carbou
 */
@RunWith(JUnit4.class)
public class VoltronMonitoringServiceTest {

  ObjectMapper mapper = new ObjectMapper();
  MonitoringServiceProvider activeServiceProvider = new MonitoringServiceProvider();
  MonitoringServiceProvider passiveServiceProvider = new MonitoringServiceProvider();
  ClientCommunicator clientCommunicator;
  long now = 1476304913984L;

  // service that the platform calls when it received calls on active and passive through IMonitoringProducer
  IStripeMonitoring activePlatformListener;
  IStripeMonitoring activeDataListener;

  // an active and passive
  PlatformServer active;
  PlatformServer passive;

  // a monitoring service (and buffer) retrieved from our entity 1
  ManagementService managementService;
  ClientMonitoringService clientMonitoringService;
  EntityMonitoringService activeEntityMonitoringService;
  EntityMonitoringService passiveEntityMonitoringService;
  BlockingQueue<Message> buffer = new LinkedBlockingQueue<>();
  ManagementExecutor managementExecutor = Mockito.spy(new ManagementExecutorAdapter() {
    @Override
    public void sendMessageToClients(Message message) {
      buffer.add(message);
    }
  });

  @Before
  public void setUp() throws Exception {
    mapper.configure(SerializationFeature.INDENT_OUTPUT, true);

    active = new PlatformServer("server-1", "localhost", "127.0.0.1", "0.0.0.0", 9510, 9610, "v1", "b1", now);
    passive = new PlatformServer("server-2", "localhost", "127.0.0.1", "0.0.0.0", 9511, 9611, "v1", "b1", now);

    // platform does this call to fire events to us
    activeServiceProvider.initialize(null, new MyPlatformConfiguration(active.getServerName()));
    passiveServiceProvider.initialize(null, new MyPlatformConfiguration(passive.getServerName()));
    activePlatformListener = activeServiceProvider.getService(0, new BasicServiceConfiguration<>(IStripeMonitoring.class));

    // simulation of platform calls when active server is up
    activePlatformListener.serverDidBecomeActive(active);
    activePlatformListener.addNode(active, null, PLATFORM_ROOT_NAME, null);
    activePlatformListener.addNode(active, PLATFORM_PATH, STATE_NODE_NAME, new ServerState("ACTIVE", active.getStartTime(), active.getStartTime()));
    activePlatformListener.addNode(active, PLATFORM_PATH, ENTITIES_ROOT_NAME, null);
    activePlatformListener.addNode(active, PLATFORM_PATH, FETCHED_ROOT_NAME, null);
    activePlatformListener.addNode(active, PLATFORM_PATH, CLIENTS_ROOT_NAME, null);

    // simulate a client connection
    activePlatformListener.addNode(active, CLIENTS_PATH, "client-1", new PlatformConnectedClient("uuid-1", "name", InetAddress.getByName("localhost"), 1234, InetAddress.getByName("localhost"), 5678, 111));

    // simulate an entity creation
    activePlatformListener.addNode(active, ENTITIES_PATH, "entity-1", new PlatformEntity("entityType", "entityName-1", 1, true));

    // an entity is requesting the service in its "createActiveEntity" method
    // simulate the IMonitoringProducer's voltron implementation for consumer id 1
    activeDataListener = activeServiceProvider.getService(1, new BasicServiceConfiguration<>(IStripeMonitoring.class));

    clientCommunicator = mock(ClientCommunicator.class);
    managementService = activeServiceProvider.getService(1, new ManagementServiceConfiguration());
    managementService.setManagementExecutor(managementExecutor);

    clientMonitoringService = activeServiceProvider.getService(1, new ClientMonitoringServiceConfiguration(mock(ServiceRegistry.class)) {
      @Override
      public ClientCommunicator getClientCommunicator() {
        return clientCommunicator;
      }
    });

    EntityManagementRegistry activeRegistry = activeServiceProvider.getService(1, new ManagementRegistryConfiguration(mock(ServiceRegistry.class), true));
    activeEntityMonitoringService = activeRegistry.getMonitoringService();
  }

  @Test
  public void test_initial_topology() throws Exception {
    assertTopologyEquals("cluster-1.json");

    List<Message> messages = messages();
    // because buffer is created after server is started of course!
    assertThat(messages.size(), equalTo(0));
  }

  @Test
  public void test_add_new_client() throws Exception {
    activePlatformListener.addNode(active, CLIENTS_PATH, "client-2", new PlatformConnectedClient("uuid-2", "name", InetAddress.getByName("localhost"), 1235, InetAddress.getByName("localhost"), 5679, 222));
    assertTopologyEquals("cluster-2.json");

    List<Message> messages = messages();
    assertThat(messageTypes(messages), equalTo(Arrays.asList("NOTIFICATION")));
    assertThat(notificationTypes(messages), equalTo(Arrays.asList("CLIENT_CONNECTED")));
  }

  @Test
  public void test_remove_client() throws Exception {
    test_add_new_client();
    activePlatformListener.removeNode(active, CLIENTS_PATH, "client-2");
    assertTopologyEquals("cluster-1.json");

    List<Message> messages = messages();
    assertThat(messageTypes(messages), equalTo(Arrays.asList("NOTIFICATION")));
    assertThat(notificationTypes(messages), equalTo(Arrays.asList("CLIENT_DISCONNECTED")));
  }

  @Test
  public void test_add_new_entity() throws Exception {
    activePlatformListener.addNode(active, ENTITIES_PATH, "entity-2", new PlatformEntity("entityType", "entityName-2", 2, true));
    assertTopologyEquals("cluster-3.json");

    List<Message> messages = messages();
    assertThat(messageTypes(messages), equalTo(Arrays.asList("NOTIFICATION")));
    assertThat(notificationTypes(messages), equalTo(Arrays.asList("SERVER_ENTITY_CREATED")));
  }

  @Test
  public void test_remove_entity() throws Exception {
    test_add_new_entity();
    activePlatformListener.removeNode(active, ENTITIES_PATH, "entity-2");
    assertTopologyEquals("cluster-1.json");

    List<Message> messages = messages();
    assertThat(messageTypes(messages), equalTo(Arrays.asList("NOTIFICATION")));
    assertThat(notificationTypes(messages), equalTo(Arrays.asList("SERVER_ENTITY_DESTROYED")));
  }

  @Test
  public void test_fetch_entity() throws Exception {
    activePlatformListener.addNode(active, FETCHED_PATH, "fetch-1-1", new PlatformClientFetchedEntity("client-1", "entity-1", new FakeDesc("1-1")));
    assertTopologyEquals("cluster-4.json");

    List<Message> messages = messages();
    assertThat(messageTypes(messages), equalTo(Arrays.asList("NOTIFICATION")));
    assertThat(notificationTypes(messages), equalTo(Arrays.asList("SERVER_ENTITY_FETCHED")));
  }

  @Test
  public void test_fetch_entity_twice() throws Exception {
    // fetch twice the same from same client
    activePlatformListener.addNode(active, FETCHED_PATH, "fetch-1-1", new PlatformClientFetchedEntity("client-1", "entity-1", new FakeDesc("1-1-1")));
    activePlatformListener.addNode(active, FETCHED_PATH, "fetch-1-1", new PlatformClientFetchedEntity("client-1", "entity-1", new FakeDesc("1-1-2")));
    assertTopologyEquals("cluster-6.json");

    List<Message> messages = messages();
    assertThat(messageTypes(messages), equalTo(Arrays.asList("NOTIFICATION", "NOTIFICATION")));
    assertThat(notificationTypes(messages), equalTo(Arrays.asList("SERVER_ENTITY_FETCHED", "SERVER_ENTITY_FETCHED")));
  }

  @Test
  public void test_unfetch_entity() throws Exception {
    test_fetch_entity();
    activePlatformListener.removeNode(active, FETCHED_PATH, "fetch-1-1");
    assertTopologyEquals("cluster-1.json");

    List<Message> messages = messages();
    assertThat(messageTypes(messages), equalTo(Arrays.asList("NOTIFICATION")));
    assertThat(notificationTypes(messages), equalTo(Arrays.asList("SERVER_ENTITY_UNFETCHED")));
  }

  @Test
  public void test_add_passive_entity() throws Exception {
    // this reflects the calls received on active through IStripeMonitoring
    activePlatformListener.serverDidJoinStripe(passive);
    activePlatformListener.addNode(passive, PLATFORM_PATH, "state", new ServerState("PASSIVE", now, 0));
    activePlatformListener.addNode(passive, ENTITIES_PATH, "entity-1", new PlatformEntity("entityType", "entityName-1", 3, false));
    assertTopologyEquals("cluster-5.json");

    List<Message> messages = messages();
    assertThat(messageTypes(messages), equalTo(Arrays.asList("NOTIFICATION", "NOTIFICATION", "NOTIFICATION")));
    assertThat(notificationTypes(messages), equalTo(Arrays.asList("SERVER_JOINED", "SERVER_STATE_CHANGED", "SERVER_ENTITY_CREATED")));
  }

  @Test
  public void test_remove_passive() throws Exception {
    test_add_passive_entity();
    activePlatformListener.removeNode(passive, ENTITIES_PATH, "entity-1");
    activePlatformListener.removeNode(passive, PLATFORM_PATH, "state");
    activePlatformListener.serverDidLeaveStripe(passive);
    assertTopologyEquals("cluster-1.json");

    List<Message> messages = messages();
    assertThat(messageTypes(messages), equalTo(Arrays.asList("NOTIFICATION", "NOTIFICATION")));
    assertThat(notificationTypes(messages), equalTo(Arrays.asList("SERVER_ENTITY_DESTROYED", "SERVER_LEFT")));
  }

  @Test
  public void test_expose_client_tags() throws Exception {
    test_fetch_entity();
    clientMonitoringService.exposeTags(new FakeDesc("1-1"), "tags1");
    assertTopologyEquals("cluster-10.json");

    List<Message> messages = messages();
    assertThat(messageTypes(messages), equalTo(Arrays.asList("NOTIFICATION")));
    assertThat(notificationTypes(messages), equalTo(Arrays.asList("CLIENT_TAGS_UPDATED")));
  }

  @Test
  public void test_expose_registry_on_client() throws Exception {
    test_fetch_entity();
    clientMonitoringService.exposeManagementRegistry(
        new FakeDesc("1-1"),
        new ContextContainer("ctName", "ctValue"),
        new DefaultCapability("capabilityName", new CapabilityContext(), new CallDescriptor("myMethod", "java.lang.String")));
    assertTopologyEquals("cluster-7.json");

    List<Message> messages = messages();
    assertThat(messageTypes(messages), equalTo(Arrays.asList("NOTIFICATION")));
    assertThat(notificationTypes(messages), equalTo(Arrays.asList("CLIENT_REGISTRY_AVAILABLE")));
  }

  @Test
  public void test_expose_registry_on_active_entity() throws Exception {
    activeEntityMonitoringService.exposeManagementRegistry(
        new ContextContainer("ctName", "ctValue"),
        new DefaultCapability("capabilityName", new CapabilityContext(), new CallDescriptor("myMethod", "java.lang.String")));
    assertTopologyEquals("cluster-8.json");

    List<Message> messages = messages();
    assertThat(messageTypes(messages), equalTo(Arrays.asList("NOTIFICATION")));
    assertThat(notificationTypes(messages), equalTo(Arrays.asList("ENTITY_REGISTRY_AVAILABLE")));
  }

  @Test
  public void test_expose_registry_on_passive_entity() throws Exception {
    test_add_passive_entity();

    // simulate the IMonitoringProducer's voltron implementation for consumer id 3
    activeDataListener = activeServiceProvider.getService(3, new BasicServiceConfiguration<>(IStripeMonitoring.class));
    IMonitoringProducer monitoringProducer = new IMonitoringProducer() {
      @Override
      public boolean addNode(String[] parents, String name, Serializable value) {
        return activeDataListener.addNode(passive, parents, name, value);
      }

      @Override
      public boolean removeNode(String[] parents, String name) {
        return activeDataListener.removeNode(passive, parents, name);
      }

      @Override
      public void pushBestEffortsData(String name, Serializable data) {
        activeDataListener.pushBestEffortsData(passive, name, data);
      }
    };
    EntityManagementRegistry passiveRegistry = passiveServiceProvider.getService(3, new ManagementRegistryConfiguration(mock(ServiceRegistry.class), false) {
      @Override
      public  IMonitoringProducer getMonitoringProducer() {
        return monitoringProducer;
      }
    });
    passiveEntityMonitoringService = passiveRegistry.getMonitoringService();

    passiveEntityMonitoringService.exposeManagementRegistry(
        new ContextContainer("k", "v"),
        new DefaultCapability("capabilityName", new CapabilityContext(), new CallDescriptor("myMethod", "java.lang.String")));
    assertTopologyEquals("cluster-9.json");

    List<Message> messages = messages();
    assertThat(messageTypes(messages), equalTo(Arrays.asList("NOTIFICATION")));
    assertThat(notificationTypes(messages), equalTo(Arrays.asList("ENTITY_REGISTRY_AVAILABLE")));

    // no update
    passiveEntityMonitoringService.exposeManagementRegistry(
        new ContextContainer("k", "v"),
        new DefaultCapability("capabilityName", new CapabilityContext(), new CallDescriptor("myMethod", "java.lang.String")));

    messages = messages();
    assertThat(messages.size(), equalTo(0));

    // update
    passiveEntityMonitoringService.exposeManagementRegistry(
        new ContextContainer("w", "w"),
        new DefaultCapability("capabilityName", new CapabilityContext(), new CallDescriptor("myMethod", "java.lang.String")));

    messages = messages();
    assertThat(messages.size(), equalTo(0));
  }

  @Test
  public void test_entity_identifier() throws Exception {
    assertThat(activeEntityMonitoringService.getConsumerId(), equalTo(1L));
  }

  @Test
  public void test_notifs_and_stats() throws Exception {
    test_fetch_entity();

    clientMonitoringService.pushNotification(new FakeDesc("1-1"), new ContextualNotification(Context.empty(), "TYPE-1"));
    clientMonitoringService.pushStatistics(new FakeDesc("1-1"), new ContextualStatistics("capability", Context.empty(), Collections.emptyMap()));

    activeDataListener.pushBestEffortsData(active, TOPIC_SERVER_ENTITY_NOTIFICATION, new ContextualNotification(Context.empty(), "TYPE-2"));
    activeDataListener.pushBestEffortsData(active, TOPIC_SERVER_ENTITY_STATISTICS, new ContextualStatistics[]{new ContextualStatistics("capability", Context.empty(), Collections.emptyMap())});

    List<Message> messages = messages();
    assertThat(messageTypes(messages), equalTo(Arrays.asList("NOTIFICATION", "STATISTICS", "NOTIFICATION", "STATISTICS")));
    assertThat(notificationTypes(messages), equalTo(Arrays.asList("TYPE-1", "TYPE-2")));
    assertThat(
        notificationContexts(messages),
        equalTo(Arrays.asList(
            Context.create(Client.KEY, "111@127.0.0.1:name:uuid-1"),
            managementService.readTopology().getSingleStripe().getActiveServerEntity("entityName-1", "entityType").get().getContext())));
  }

  @Test
  public void test_management_call() throws Exception {
    activePlatformListener.addNode(active, FETCHED_PATH, "fetch-1-1", new PlatformClientFetchedEntity("client-1", "entity-1", new FakeDesc("1-1")));

    activePlatformListener.addNode(active, CLIENTS_PATH, "client-2", new PlatformConnectedClient("uuid-2", "name", InetAddress.getByName("localhost"), 1235, InetAddress.getByName("localhost"), 5679, 222));
    activePlatformListener.addNode(active, FETCHED_PATH, "fetch-2-1", new PlatformClientFetchedEntity("client-2", "entity-1", new FakeDesc("2-1")));

    clientMonitoringService.exposeManagementRegistry(
        new FakeDesc("2-1"),
        new ContextContainer("ctName", "ctValue"),
        new DefaultCapability("capabilityName", new CapabilityContext(), new CallDescriptor("myMethod", "java.lang.String")));

    String id = managementService.sendManagementCallRequest(
        new FakeDesc("1-1"),
        Context.create(Client.KEY, ClientIdentifier.create(222L, InetAddress.getByName("localhost").getHostAddress(), "name", "uuid-2").toString()),
        "capabilityName",
        "myMethod",
        Void.TYPE);

    clientMonitoringService.answerManagementCall(
        new FakeDesc("2-1"),
        id,
        ContextualReturn.notExecuted("capabilityName", Context.empty(), "methodName"));

    verify(clientCommunicator, times(1)).sendNoResponse(eq(new FakeDesc("2-1")), any(EntityResponse.class));
    verifyNoMoreInteractions(clientCommunicator);

    verify(managementExecutor, times(1)).sendMessageToClient(any(Message.class), eq(new FakeDesc("1-1")));
    verify(managementExecutor, times(4)).sendMessageToClients(any(Message.class)); // notifs and stats
    verifyNoMoreInteractions(managementExecutor);
  }

  private void assertTopologyEquals(String file) throws Exception {
    Cluster cluster = managementService.readTopology();
    cluster.serverStream().forEach(server -> {
      server.setUpTimeSec(0);
    });
    assertEquals(new String(Files.readAllBytes(new File("src/test/resources/" + file).toPath()), "UTF-8"), mapper.writeValueAsString(cluster.toMap()));
  }

  private List<Message> messages() {
    List<Message> messages = new ArrayList<>();
    buffer.drainTo(messages);
    return messages;
  }

  private static List<String> messageTypes(List<Message> messages) {
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

  private static List<Context> notificationContexts(List<Message> messages) {
    return messages.stream()
        .filter(message -> message.getType().equals("NOTIFICATION"))
        .flatMap(message -> message.unwrap(ContextualNotification.class).stream())
        .map(ContextualNotification::getContext)
        .collect(Collectors.toList());
  }

  private static final class FakeDesc implements ClientDescriptor {
    private final String id;

    FakeDesc(String id) {
      this.id = id;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      FakeDesc that = (FakeDesc) o;
      return id.equals(that.id);
    }

    @Override
    public int hashCode() {
      return id.hashCode();
    }

    @Override
    public String toString() {
      return id;
    }

    @Override
    public ClientSourceId getSourceId() {
      return null;
    }
  }

}
