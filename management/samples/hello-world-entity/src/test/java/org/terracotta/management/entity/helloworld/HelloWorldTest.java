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
package org.terracotta.management.entity.helloworld;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionFactory;
import org.terracotta.management.entity.helloworld.client.HelloWorldEntity;
import org.terracotta.management.entity.helloworld.client.HelloWorldEntityClientService;
import org.terracotta.management.entity.helloworld.client.HelloWorldEntityFactory;
import org.terracotta.management.entity.helloworld.client.management.HelloWorldManagementRegistry;
import org.terracotta.management.entity.helloworld.server.HelloWorldEntityServerService;
import org.terracotta.management.entity.management.ManagementAgentConfig;
import org.terracotta.management.entity.management.client.ManagementAgentEntityClientService;
import org.terracotta.management.entity.management.client.ManagementAgentEntityFactory;
import org.terracotta.management.entity.management.client.ManagementAgentService;
import org.terracotta.management.entity.management.server.ManagementAgentEntityServerService;
import org.terracotta.management.entity.monitoring.client.MonitoringServiceEntityClientService;
import org.terracotta.management.entity.monitoring.client.MonitoringServiceEntityFactory;
import org.terracotta.management.entity.monitoring.client.MonitoringServiceProxyEntity;
import org.terracotta.management.entity.monitoring.server.MonitoringServiceEntityServerService;
import org.terracotta.management.model.call.Parameter;
import org.terracotta.management.model.cluster.Client;
import org.terracotta.management.model.cluster.ManagementRegistry;
import org.terracotta.management.model.context.Context;
import org.terracotta.passthrough.PassthroughClusterControl;
import org.terracotta.passthrough.PassthroughServer;

import java.net.URI;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * @author Mathieu Carbou
 */
@RunWith(JUnit4.class)
public class HelloWorldTest {

  PassthroughClusterControl stripeControl;

  @Before
  public void setUp() throws Exception {
    PassthroughServer activeServer = new PassthroughServer();

    activeServer.registerServerEntityService(new HelloWorldEntityServerService());
    activeServer.registerClientEntityService(new HelloWorldEntityClientService());

    activeServer.registerServerEntityService(new ManagementAgentEntityServerService());
    activeServer.registerClientEntityService(new ManagementAgentEntityClientService());

    activeServer.registerServerEntityService(new MonitoringServiceEntityServerService());
    activeServer.registerClientEntityService(new MonitoringServiceEntityClientService());

    stripeControl = new PassthroughClusterControl("stripe-1", activeServer);
  }

  @After
  public void tearDown() throws Exception {
    stripeControl.tearDown();
  }

  @Test
  public void test_hello_world_management() throws Exception {
    try (Connection connection = ConnectionFactory.connect(URI.create("passthrough://stripe-1:9510/cluster-1"), new Properties())) {

      // create, fetch and use the custom entity

      HelloWorldEntityFactory helloWorldEntityFactory = new HelloWorldEntityFactory(connection);
      HelloWorldEntity entity = helloWorldEntityFactory.retrieveOrCreate(getClass().getSimpleName());

      assertEquals("Hello world!", entity.sayHello("world"));

      // create a custom management registry

      HelloWorldManagementRegistry managementRegistry = new HelloWorldManagementRegistry("my-hello-world-entity-name");
      managementRegistry.register(entity);

      // try a management call that will hit the entity underhood

      String returned = managementRegistry.withCapability("HelloWorldActionProvider")
          .call("sayHello", String.class, new Parameter("you", String.class.getName()))
          .on(Context.create("entityName", "my-hello-world-entity-name"))
          .build()
          .execute()
          .getSingleResult()
          .getValue();
      assertEquals("Hello you!", returned);

      // expose a management registry in the server

      ManagementAgentService managementAgentService = new ManagementAgentService(new ManagementAgentEntityFactory(connection).retrieveOrCreate(new ManagementAgentConfig()));
      managementAgentService.setManagementRegistry(managementRegistry);
      managementAgentService.init();

      // check it has been exposed properly

      MonitoringServiceEntityFactory entityFactory = new MonitoringServiceEntityFactory(connection);
      MonitoringServiceProxyEntity consumerEntity = entityFactory.retrieveOrCreate(getClass().getSimpleName());

      ManagementRegistry registry = consumerEntity.readTopology()
          .clientStream()
          .filter(Client::isManageable)
          .map(client -> client.getManagementRegistry().get())
          .findFirst()
          .get();

      assertEquals(managementRegistry.getContextContainer(), registry.getContextContainer());
      assertEquals(managementRegistry.getCapabilities(), registry.getCapabilities());
    }
  }

}
