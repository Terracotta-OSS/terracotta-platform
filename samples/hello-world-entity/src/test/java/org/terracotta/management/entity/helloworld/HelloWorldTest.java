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
package org.terracotta.management.entity.helloworld;

import org.terracotta.management.entity.helloworld.client.HelloWorldEntity;
import org.terracotta.management.entity.helloworld.client.HelloWorldEntityClientService;
import org.terracotta.management.entity.helloworld.client.HelloWorldEntityFactory;
import org.terracotta.management.entity.helloworld.client.management.HelloWorldManagementRegistry;
import org.terracotta.management.entity.helloworld.server.HelloWorldEntityServerService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.terracotta.connection.Connection;
import org.terracotta.management.entity.client.ManagementAgentEntityClientService;
import org.terracotta.management.entity.client.ManagementAgentService;
import org.terracotta.management.entity.server.ManagementAgentEntityServerService;
import org.terracotta.management.model.call.Parameter;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.service.monitoring.MonitoringServiceConfiguration;
import org.terracotta.management.service.monitoring.MonitoringServiceProvider;
import org.terracotta.passthrough.IClusterControl;
import org.terracotta.passthrough.PassthroughClusterControl;
import org.terracotta.passthrough.PassthroughServer;

import static org.junit.Assert.assertEquals;

/**
 * @author Mathieu Carbou
 */
@RunWith(JUnit4.class)
public class HelloWorldTest {

  IClusterControl stripeControl;

  @Before
  public void setUp() throws Exception {
    PassthroughServer activeServer = new PassthroughServer(true);

    activeServer.registerServerEntityService(new HelloWorldEntityServerService());
    activeServer.registerClientEntityService(new HelloWorldEntityClientService());

    activeServer.registerServerEntityService(new ManagementAgentEntityServerService());
    activeServer.registerClientEntityService(new ManagementAgentEntityClientService());

    activeServer.registerServiceProvider(new MonitoringServiceProvider(), new MonitoringServiceConfiguration().setDebug(true));

    activeServer.start();
    stripeControl = new PassthroughClusterControl("server-1", activeServer, null);
  }

  @After
  public void tearDown() throws Exception {
    stripeControl.tearDown();
  }

  @Test
  public void test_basic_tms_entity() throws Exception {
    try (Connection connection = stripeControl.createConnectionToActive()) {

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

      ManagementAgentService managementAgentService = new ManagementAgentService(connection);
      managementAgentService.setCapabilities(managementRegistry.getContextContainer(), managementRegistry.getCapabilities());

      //
    }
  }

}
