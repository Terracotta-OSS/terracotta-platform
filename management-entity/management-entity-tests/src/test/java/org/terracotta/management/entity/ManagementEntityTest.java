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
import org.terracotta.entity.ServiceProviderConfiguration;
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
import org.terracotta.management.service.monitoring.IMonitoringConsumer;
import org.terracotta.management.service.monitoring.MonitoringConsumerConfiguration;
import org.terracotta.management.service.monitoring.MonitoringServiceConfiguration;
import org.terracotta.management.service.monitoring.MonitoringServiceProvider;
import org.terracotta.passthrough.IClusterControl;
import org.terracotta.passthrough.PassthroughTestHelpers;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.terracotta.monitoring.PlatformMonitoringConstants.FETCHED_PATH;

/**
 * @author Mathieu Carbou
 */
@RunWith(JUnit4.class)
public class ManagementEntityTest {

  private static IMonitoringConsumer consumer;

  @Test
  public void test_expose() throws EntityNotProvidedException, EntityVersionMismatchException, EntityAlreadyExistsException, EntityNotFoundException, IOException, ExecutionException, InterruptedException {
    IClusterControl stripeControl = PassthroughTestHelpers.createActiveOnly(server -> {
      server.setBindPort(9510);
      server.setGroupPort(9610);
      server.setServerName("server-1");
      server.registerClientEntityService(new ManagementAgentEntityClientService());
      server.registerServerEntityService(new ManagementAgentEntityServerService());
      server.registerServiceProvider(new HackedMonitoringServiceProvider(), new MonitoringServiceConfiguration().setDebug(true));
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
      assertEquals("UNKNOWN", clientIdentifier.getName());
      assertNotNull(clientIdentifier.getConnectionUid());

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
          Arrays.asList("EhcachePounder", "webapp-1", "app-server-node-1"),
          null).get();

      contexts = entity.getEntityContexts(null).get();
      System.out.println(contexts);
      assertEquals(1, contexts.size());
      context = contexts.iterator().next();
      assertEquals(5, context.size());
      assertEquals("my-cm-name", context.get("cacheManagerName"));

      String fetchId = consumer.getChildNamesForNode(FETCHED_PATH).get().iterator().next();
      Map<String, Object> children = consumer.getChildValuesForNode(FETCHED_PATH, fetchId).get();
      assertEquals(3, children.size());
      assertEquals(new TreeSet<>(Arrays.asList("contextContainer", "capabilities", "tags")), new TreeSet<>(children.keySet()));
      assertEquals(registry.getCapabilities(), children.get("capabilities"));
      assertEquals(Arrays.asList("EhcachePounder", "webapp-1", "app-server-node-1"), children.get("tags"));
    }
  }

  // to be able to access the IMonitoringConsumer interface outside Voltron
  public static class HackedMonitoringServiceProvider extends MonitoringServiceProvider {
    @Override
    public boolean initialize(ServiceProviderConfiguration configuration) {
      super.initialize(configuration);
      consumer = getService(0, new MonitoringConsumerConfiguration());
      return true;
    }
  }

}
