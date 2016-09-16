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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.terracotta.entity.BasicServiceConfiguration;
import org.terracotta.monitoring.IStripeMonitoring;
import org.terracotta.monitoring.PlatformClientFetchedEntity;
import org.terracotta.monitoring.PlatformConnectedClient;
import org.terracotta.monitoring.PlatformEntity;
import org.terracotta.monitoring.PlatformServer;
import org.terracotta.monitoring.ServerState;

import java.io.Serializable;
import java.util.HashSet;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.terracotta.management.service.monitoring.DefaultMonitoringConsumer.SERVERS_ROOT_NAME;
import static org.terracotta.management.service.monitoring.Utils.array;
import static org.terracotta.monitoring.PlatformMonitoringConstants.CLIENTS_PATH;
import static org.terracotta.monitoring.PlatformMonitoringConstants.CLIENTS_ROOT_NAME;
import static org.terracotta.monitoring.PlatformMonitoringConstants.ENTITIES_PATH;
import static org.terracotta.monitoring.PlatformMonitoringConstants.ENTITIES_ROOT_NAME;
import static org.terracotta.monitoring.PlatformMonitoringConstants.FETCHED_PATH;
import static org.terracotta.monitoring.PlatformMonitoringConstants.FETCHED_ROOT_NAME;
import static org.terracotta.monitoring.PlatformMonitoringConstants.PLATFORM_PATH;
import static org.terracotta.monitoring.PlatformMonitoringConstants.PLATFORM_ROOT_NAME;
import static org.terracotta.monitoring.PlatformMonitoringConstants.SERVER_STATE_STOPPED;
import static org.terracotta.monitoring.PlatformMonitoringConstants.STATE_NODE_NAME;

/**
 * @author Mathieu Carbou
 */
@RunWith(JUnit4.class)
public class VoltronMonitoringServiceTest {

  MonitoringServiceProvider serviceProvider = new MonitoringServiceProvider();
  IStripeMonitoring producer;
  IMonitoringConsumer consumer;
  ReadOnlyBuffer<PlatformNotification> notifications;

  @Before
  public void setUp() throws Exception {
    producer = serviceProvider.getService(0, new BasicServiceConfiguration<>(IStripeMonitoring.class));
    consumer = serviceProvider.getService(0, new BasicServiceConfiguration<>(IMonitoringConsumer.class));
    notifications = consumer.getOrCreatePlatformNotificationBuffer(1024);
  }

  @Test
  public void test_topology_branches() {
    PlatformServer server1 = new PlatformServer("server-1", "localhost", "127.0.0.1", "0.0.0.0", 1234, 5678, "v1", "b1", System.currentTimeMillis());
    producer.serverDidBecomeActive(server1);

    // See ManagementTopologyEventCollector
    producer.addNode(null, new String[0], PLATFORM_ROOT_NAME, null);
    producer.addNode(null, PLATFORM_PATH, CLIENTS_ROOT_NAME, null);
    producer.addNode(null, PLATFORM_PATH, ENTITIES_ROOT_NAME, null);
    producer.addNode(null, PLATFORM_PATH, FETCHED_ROOT_NAME, null);
    producer.addNode(null, PLATFORM_PATH, STATE_NODE_NAME, SERVER_STATE_STOPPED);

    assertEquals(SERVER_STATE_STOPPED, consumer.getMonitoringTree(0).get().getValueForNode(PLATFORM_PATH, STATE_NODE_NAME, String.class).get());
    assertEquals(new HashSet<>(asList(
        CLIENTS_ROOT_NAME,
        ENTITIES_ROOT_NAME,
        FETCHED_ROOT_NAME,
        STATE_NODE_NAME,
        SERVERS_ROOT_NAME,
        STATE_NODE_NAME
    )), consumer.getMonitoringTree(0).get().getChildNamesForNode(PLATFORM_PATH).get());
  }

  @Test
  public void test_inexisting_parent() {
    assertTrue(producer.addNode(null, null, "A", 1));
    assertFalse(producer.addNode(null, array("A", "AA"), "AAA", 1));
  }

  @Test
  public void test_notifs() {
    PlatformServer server1 = new PlatformServer("server-1", "localhost", "127.0.0.1", "0.0.0.0", 1234, 5678, "v1", "b1", System.currentTimeMillis());
    producer.serverDidBecomeActive(server1);

    producer.addNode(null, new String[0], PLATFORM_ROOT_NAME, null);

    producer.addNode(null, PLATFORM_PATH, ENTITIES_ROOT_NAME, null);
    producer.addNode(null, PLATFORM_PATH, CLIENTS_ROOT_NAME, null);
    producer.addNode(null, PLATFORM_PATH, FETCHED_ROOT_NAME, null);

    PlatformNotification notification = notifications.read();
    assertThat(notification.getType(), equalTo(PlatformNotification.Type.SERVER_JOINED));
    assertThat(notification.getSource(Serializable.class), equalTo(server1));

    ServerState state = new ServerState("ACTIVE", System.currentTimeMillis(), System.currentTimeMillis());
    producer.addNode(server1, new String[]{PLATFORM_ROOT_NAME}, STATE_NODE_NAME, state);
    notification = notifications.read();
    assertThat(notification.getType(), equalTo(PlatformNotification.Type.SERVER_STATE_CHANGED));
    assertThat(notification.getSource(Serializable[].class), equalTo(new Serializable[]{server1, state}));

    PlatformEntity entity1 = new PlatformEntity();
    producer.addNode(server1, ENTITIES_PATH, "entity1", entity1);
    notification = notifications.read();
    assertThat(notification.getType(), equalTo(PlatformNotification.Type.SERVER_ENTITY_CREATED));
    assertThat(notification.getSource(Serializable.class), equalTo(entity1));

    PlatformConnectedClient client1 = new PlatformConnectedClient();
    producer.addNode(server1, CLIENTS_PATH, "client1", client1);
    notification = notifications.read();
    assertThat(notification.getType(), equalTo(PlatformNotification.Type.CLIENT_CONNECTED));
    assertThat(notification.getSource(Serializable.class), equalTo(client1));

    PlatformClientFetchedEntity fetch1 = new PlatformClientFetchedEntity("client1", "entity1", null);
    producer.addNode(server1, FETCHED_PATH, "fetch1", fetch1);
    notification = notifications.read();
    assertThat(notification.getType(), equalTo(PlatformNotification.Type.SERVER_ENTITY_FETCHED));
    assertThat(notification.getSource(Serializable[].class), equalTo(new Serializable[]{client1, entity1}));

    producer.removeNode(server1, FETCHED_PATH, "fetch1");
    notification = notifications.read();
    assertThat(notification.getType(), equalTo(PlatformNotification.Type.SERVER_ENTITY_UNFETCHED));
    assertThat(notification.getSource(Serializable[].class), equalTo(new Serializable[]{client1, entity1}));

    producer.removeNode(server1, CLIENTS_PATH, "client1");
    notification = notifications.read();
    assertThat(notification.getType(), equalTo(PlatformNotification.Type.CLIENT_DISCONNECTED));
    assertThat(notification.getSource(Serializable.class), equalTo(client1));

    producer.removeNode(server1, ENTITIES_PATH, "entity1");
    notification = notifications.read();
    assertThat(notification.getType(), equalTo(PlatformNotification.Type.SERVER_ENTITY_DESTROYED));
    assertThat(notification.getSource(Serializable.class), equalTo(entity1));

    producer.serverDidLeaveStripe(server1);
    notification = notifications.read();
    assertThat(notification.getType(), equalTo(PlatformNotification.Type.SERVER_LEFT));
    assertThat(notification.getSource(Serializable.class), equalTo(server1));
  }

  @Test
  public void test_push_into_inexisting_buffer_do_not_fail() {
    // do not fail
    producer.pushBestEffortsData(null, "test_push_inexisting_buffer", 1);

    ReadOnlyBuffer<Integer> buffer = consumer.getOrCreateBestEffortBuffer("test_push_inexisting_buffer", 2, Integer.class);
    assertThat(buffer.size(), equalTo(0));
    assertThat(buffer.read(), equalTo(null));

    producer.pushBestEffortsData(null, "test_push_inexisting_buffer", 1);
    assertThat(buffer.size(), equalTo(1));
    assertThat(buffer.read(), equalTo(1));

  }

  @Test
  public void test_push_buffer() {
    ReadOnlyBuffer<Integer> buffer = consumer.getOrCreateBestEffortBuffer("test_push_buffer", 2, Integer.class);
    assertThat(buffer.size(), equalTo(0));
    assertThat(buffer.read(), equalTo(null));

    producer.pushBestEffortsData(null, "test_push_buffer", 1);
    producer.pushBestEffortsData(null, "test_push_buffer", 2);
    producer.pushBestEffortsData(null, "test_push_buffer", 3);

    assertThat(buffer.size(), equalTo(2));
    assertThat(buffer.read(), equalTo(2));
    assertThat(buffer.read(), equalTo(3));

    assertThat(buffer.size(), equalTo(0));
    assertThat(buffer.read(), equalTo(null));
  }

}
