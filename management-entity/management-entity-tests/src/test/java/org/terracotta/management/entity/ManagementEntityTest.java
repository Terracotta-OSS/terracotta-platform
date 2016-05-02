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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.terracotta.connection.Connection;
import org.terracotta.connection.entity.EntityRef;
import org.terracotta.exception.EntityAlreadyExistsException;
import org.terracotta.exception.EntityNotFoundException;
import org.terracotta.exception.EntityNotProvidedException;
import org.terracotta.exception.EntityVersionMismatchException;
import org.terracotta.management.entity.client.ManagementAgentEntity;
import org.terracotta.management.entity.client.ManagementAgentEntityClientService;
import org.terracotta.management.entity.server.ManagementAgentEntityServerService;
import org.terracotta.management.model.cluster.Client;
import org.terracotta.management.model.cluster.ClientIdentifier;
import org.terracotta.management.model.cluster.Manageable;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.context.ContextContainer;
import org.terracotta.management.registry.AbstractManagementRegistry;
import org.terracotta.management.registry.ManagementRegistry;
import org.terracotta.management.service.monitoring.MonitoringServiceConfiguration;
import org.terracotta.management.service.monitoring.MonitoringServiceProvider;
import org.terracotta.passthrough.IClusterControl;
import org.terracotta.passthrough.PassthroughTestHelpers;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Mathieu Carbou
 */
@RunWith(JUnit4.class)
public class ManagementEntityTest {

  @Test
  public void test_expose() throws EntityNotProvidedException, EntityVersionMismatchException, EntityAlreadyExistsException, EntityNotFoundException, IOException, ExecutionException, InterruptedException {
    IClusterControl stripeControl = PassthroughTestHelpers.createActiveOnly(server -> {
      server.setBindPort(9510);
      server.setGroupPort(9610);
      server.setServerName("server-1");
      server.registerClientEntityService(new ManagementAgentEntityClientService());
      server.registerServerEntityService(new ManagementAgentEntityServerService());
      server.registerServiceProvider(new MonitoringServiceProvider(), new MonitoringServiceConfiguration().setDebug(true));
    });

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
      EntityRef<ManagementAgentEntity, ManagementAgentConfig> ref = connection.getEntityRef(ManagementAgentEntity.class, Version.LATEST.version(), "SUPER_NAME");
      ref.create(new ManagementAgentConfig());
      ManagementAgentEntity entity = ref.fetchEntity();

      ClientIdentifier clientIdentifier = entity.getClientIdentifier(null).get();
      System.out.println(clientIdentifier);
      assertEquals(Long.parseLong(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]), clientIdentifier.getPid());
      assertEquals("product-name", clientIdentifier.getName());
      assertEquals("logical-conn-uuid", clientIdentifier.getConnectionUid());

      Collection<Context> contexts = entity.getEntityContexts(null).get();
      System.out.println(contexts);
      assertEquals(1, contexts.size());
      Context context = contexts.iterator().next();
      assertEquals(4, context.size());
      assertTrue(context.contains(Client.KEY));
      assertTrue(context.contains(Manageable.KEY));
      assertTrue(context.contains(Manageable.TYPE_KEY));
      assertTrue(context.contains(Manageable.NAME_KEY));
      assertEquals(clientIdentifier.getClientId(), context.get(Client.KEY));
      assertEquals("SUPER_NAME", context.get(Manageable.NAME_KEY));
      assertEquals(ManagementAgentConfig.ENTITY_NAME, context.get(Manageable.TYPE_KEY));

      entity.expose(
          Context.empty()
              .with(Manageable.TYPE_KEY, ManagementAgentConfig.ENTITY_NAME)
              .with(Manageable.NAME_KEY, "SUPER_NAME"),
          registry.getContextContainer(),
          registry.getCapabilities(),
          null).get();

      contexts = entity.getEntityContexts(null).get();
      System.out.println(contexts);
      assertEquals(1, contexts.size());
      context = contexts.iterator().next();
      assertEquals(5, context.size());
      assertEquals("my-cm-name", context.get("cacheManagerName"));
    }
  }

}
