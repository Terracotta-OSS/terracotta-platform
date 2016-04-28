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
import org.terracotta.management.model.context.ContextContainer;
import org.terracotta.management.registry.AbstractManagementRegistry;
import org.terracotta.management.registry.ManagementRegistry;
import org.terracotta.management.service.monitoring.MonitoringServiceConfiguration;
import org.terracotta.management.service.monitoring.MonitoringServiceProvider;
import org.terracotta.passthrough.IClusterControl;
import org.terracotta.passthrough.PassthroughServer;
import org.terracotta.passthrough.PassthroughTestHelpers;

/**
 * @author Mathieu Carbou
 */
@RunWith(JUnit4.class)
public class ManagementEntityTest {

  @Test
  public void test_expose() throws EntityNotProvidedException, EntityVersionMismatchException, EntityAlreadyExistsException, EntityNotFoundException {
    IClusterControl stripeControl = PassthroughTestHelpers.createActiveOnly(new PassthroughTestHelpers.ServerInitializer() {
      @Override
      public void registerServicesForServer(PassthroughServer server) {
        server.setBindPort(9510);
        server.setGroupPort(9610);
        server.setServerName("server-1");
        server.registerClientEntityService(new ManagementAgentEntityClientService());
        server.registerServerEntityService(new ManagementAgentEntityServerService());
        server.registerServiceProvider(new MonitoringServiceProvider(), new MonitoringServiceConfiguration().setDebug(true));
      }
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

    Connection connection = stripeControl.createConnectionToActive();
    EntityRef<ManagementAgentEntity, ManagementAgentConfig> ref = connection.getEntityRef(ManagementAgentEntity.class, Version.LATEST.version(), "NAME");
    ref.create(new ManagementAgentConfig());
    ManagementAgentEntity entity = ref.fetchEntity();
    entity.expose(registry.getContextContainer(), registry.getCapabilities(), null);
  }

}
