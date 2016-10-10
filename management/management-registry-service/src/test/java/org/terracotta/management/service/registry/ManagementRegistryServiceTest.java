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
package org.terracotta.management.service.registry;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.terracotta.entity.BasicServiceConfiguration;
import org.terracotta.entity.ServiceRegistry;
import org.terracotta.management.model.message.Message;
import org.terracotta.management.model.notification.ContextualNotification;
import org.terracotta.management.service.monitoring.MonitoringService;
import org.terracotta.management.service.monitoring.MonitoringServiceConfiguration;
import org.terracotta.management.service.monitoring.MonitoringServiceProvider;
import org.terracotta.management.service.monitoring.buffer.ReadOnlyBuffer;
import org.terracotta.management.service.monitoring.platform.IStripeMonitoring;
import org.terracotta.monitoring.PlatformEntity;
import org.terracotta.monitoring.PlatformServer;
import org.terracotta.monitoring.ServerState;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Mathieu Carbou
 */
@RunWith(JUnit4.class)
public class ManagementRegistryServiceTest {

  @Test
  public void test_management_info_pushed() {
    ServiceRegistry serviceRegistry = mock(ServiceRegistry.class);
    MonitoringServiceProvider serviceProvider = new MonitoringServiceProvider();
    IStripeMonitoring stripeMonitoring = serviceProvider.getService(0, new BasicServiceConfiguration<>(IStripeMonitoring.class));

    // simulate platform calls
    long now = System.currentTimeMillis();
    PlatformServer server = new PlatformServer("server-1", "localhost", "127.0.0.1", "0.0.0.0", 9510, 9520, "version", "build", now);
    stripeMonitoring.serverDidBecomeActive(server);
    stripeMonitoring.serverStateChanged(server, new ServerState("ACTIVE", now, now));
    stripeMonitoring.serverEntityCreated(server, new PlatformEntity("entityType", "entityName", 1, true));

    MonitoringService monitoringService = serviceProvider.getService(1, new MonitoringServiceConfiguration(serviceRegistry));
    when(serviceRegistry.getService(any(MonitoringServiceConfiguration.class))).thenReturn(monitoringService);
    ReadOnlyBuffer<Message> buffer = monitoringService.createMessageBuffer(100);

    // a consumer asks for a service
    ConsumerManagementRegistryProvider provider = new ConsumerManagementRegistryProvider();
    ConsumerManagementRegistry registry = provider.getService(0, new ConsumerManagementRegistryConfiguration(serviceRegistry)
        .addProvider(new MyManagementProvider()));

    // then register some objects
    registry.register(new MyObject("myCacheManagerName1", "myCacheName1"));
    registry.refresh();

    assertThat(buffer.size(), equalTo(1));
    assertThat(buffer.read().unwrap(ContextualNotification.class).get(0).getType(), equalTo("ENTITY_REGISTRY_UPDATED"));
    assertThat(buffer.size(), equalTo(0));

    // no modification => not dirty
    registry.refresh();
    assertThat(buffer.size(), equalTo(0));

    registry.register(new MyObject("myCacheManagerName2", "myCacheName2"));
    registry.refresh();
    assertThat(buffer.size(), equalTo(1));
  }

}
