/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package org.terracotta.management.integration.tests;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionException;
import org.terracotta.connection.ConnectionFactory;
import org.terracotta.connection.ConnectionPropertyNames;
import org.terracotta.management.entity.nms.NmsConfig;
import org.terracotta.management.entity.nms.agent.client.DefaultNmsAgentService;
import org.terracotta.management.entity.nms.agent.client.NmsAgentEntity;
import org.terracotta.management.entity.nms.agent.client.NmsAgentEntityFactory;
import org.terracotta.management.entity.nms.client.DefaultNmsService;
import org.terracotta.management.entity.nms.client.NmsEntityFactory;
import org.terracotta.management.entity.nms.client.NmsService;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.context.ContextContainer;
import org.terracotta.management.model.message.Message;
import org.terracotta.management.model.notification.ContextualNotification;
import org.terracotta.management.registry.DefaultManagementRegistry;
import org.terracotta.management.registry.ManagementRegistry;
import org.terracotta.testing.config.ConfigRepoStartupBuilder;
import org.terracotta.testing.rules.Cluster;

import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.terracotta.testing.rules.BasicExternalClusterBuilder.newCluster;

/**
 * @author Mathieu Carbou
 */
public class NmsAgentServiceIT {

  private final String offheapResource = "primary-server-resource";
  private final String resourceConfig =
      "<config xmlns:ohr='http://www.terracotta.org/config/offheap-resource'>"
          + "<ohr:offheap-resources>"
          + "<ohr:resource name=\"" + offheapResource + "\" unit=\"MB\">64</ohr:resource>"
          + "</ohr:offheap-resources>" +
          "</config>\n";

  @Rule
  public Timeout timeout = Timeout.seconds(120);

  @Rule
  public Cluster voltron = newCluster()
      .in(Paths.get("build", "galvan", "tests"))
      .withSystemProperty("terracotta.management.assert", "true")
      .withTcProperty("terracotta.management.assert", "true")
      .withServiceFragment(resourceConfig)
      .startupBuilder(ConfigRepoStartupBuilder::new)
      .build();

  Connection managementConnection;
  NmsService nmsService;
  DefaultNmsAgentService nmsAgentService;
  AtomicInteger opErrors = new AtomicInteger();
  ManagementRegistry registry = new DefaultManagementRegistry(new ContextContainer("foo", "bar"));

  volatile Connection clientConnection;

  @Before
  public void setUp() throws Exception {
    voltron.getClusterControl().waitForActive();

    managementConnection = createConnection();

    NmsEntityFactory nmsEntityFactory = new NmsEntityFactory(managementConnection, getClass().getSimpleName());
    nmsService = new DefaultNmsService(nmsEntityFactory.retrieveOrCreate(new NmsConfig()));
    nmsService.setOperationTimeout(30, TimeUnit.SECONDS);

    clientConnection = createConnection();

    // note the supplier below, which is used to recycle the entity with a potentially new connection
    nmsAgentService = new DefaultNmsAgentService(Context.empty(), this::createAgentEntity);
    nmsAgentService.setOperationTimeout(30, TimeUnit.SECONDS);
    nmsAgentService.setOnOperationError((operation, throwable) -> opErrors.incrementAndGet());
    nmsAgentService.setManagementRegistry(registry);
  }

  @After
  public void tearDown() {
    try {
      managementConnection.close();
    } catch (Exception ignored) {
    }
    try {
      clientConnection.close();
    } catch (Exception ignored) {
    }
  }

  @Test
  public void can_set_tag_and_close_client() throws Exception {
    nmsAgentService.setTags("tag");
    assertThat(nmsService.readTopology().clientStream().anyMatch(client -> client.getTags().contains("tag")), is(true));

    clientConnection.close();
    assertThat(opErrors.get(), equalTo(0));
    while (!Thread.currentThread().isInterrupted()) {
      if (nmsService.readTopology().clientStream().noneMatch(client -> client.getTags().contains("tag"))) {
        return;
      }
    }
    fail();
  }

  @Test
  public void nmsAgentService_can_recycle_entity_and_recover_manually() throws Exception {
    can_set_tag_and_close_client();

    nmsAgentService.sendStates();
    assertThat(opErrors.get(), equalTo(2));

    clientConnection = createConnection();
    nmsAgentService.flushEntity();
    nmsAgentService.sendStates();
    assertThat(opErrors.get(), equalTo(2));

    while (!Thread.currentThread().isInterrupted()) {
      if (nmsService.readTopology().clientStream().anyMatch(c -> c.getTags().contains("tag") && c.isManageable())) {
        return;
      }
    }
    fail();
  }

  @Test
  public void nmsAgentService_can_recycle_entity_and_recover_states_automatically() throws Exception {
    can_set_tag_and_close_client();

    nmsAgentService.pushNotification(new ContextualNotification(Context.empty(), "MY_NOTIF_TYPE"));
    assertThat(opErrors.get(), equalTo(1));

    clientConnection = createConnection();
    nmsAgentService.pushNotification(new ContextualNotification(Context.empty(), "MY_NOTIF_TYPE"));
    assertThat(opErrors.get(), equalTo(1));

    while (!Thread.currentThread().isInterrupted()) {
      if (nmsService.readTopology().clientStream().anyMatch(c -> c.getTags().contains("tag") && c.isManageable())) {
        return;
      }
    }
    fail();
  }

  @Test
  public void nmsAgentService_can_retry_operation() throws Exception {
    nmsAgentService.setOnOperationError((operation, throwable) -> {
      opErrors.incrementAndGet();

      // recycle the connection
      try {
        clientConnection = createConnection();
      } catch (ConnectionException e) {
        throw new RuntimeException(e);
      }

      operation.retry();
    });

    can_set_tag_and_close_client();

    nmsAgentService.pushNotification(new ContextualNotification(Context.empty(), "MY_NOTIF_TYPE"));
    assertThat(opErrors.get(), equalTo(1));

    while (!Thread.currentThread().isInterrupted()) {
      if (nmsService.readTopology().clientStream().anyMatch(c -> c.getTags().contains("tag") && c.isManageable())) {
        return;
      }
    }
    fail();

    while (!Thread.currentThread().isInterrupted()) {
      Message message = nmsService.waitForMessage();
      if (message.getType().equals("NOTIFICATION") && message.unwrap(ContextualNotification.class).get(0).getType().equals("MY_NOTIF_TYPE")) {
        return;
      }
    }
    fail();
  }

  private NmsAgentEntity createAgentEntity() {
    // uses the global client connection object to create an entity
    // this connection ref will always be there, but might be "broken"
    assertNotNull(clientConnection);
    return new NmsAgentEntityFactory(clientConnection).retrieve();
  }

  private Connection createConnection() throws ConnectionException {
    Properties properties = new Properties();
    properties.setProperty(ConnectionPropertyNames.CONNECTION_NAME, "NmsAgentServiceIT");
    return ConnectionFactory.connect(voltron.getConnectionURI(), properties);
  }
}
