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
import org.terracotta.entity.ClientCommunicator;
import org.terracotta.management.model.message.Message;
import org.terracotta.management.model.notification.ContextualNotification;
import org.terracotta.monitoring.IMonitoringProducer;
import org.terracotta.monitoring.IStripeMonitoring;
import org.terracotta.monitoring.PlatformEntity;
import org.terracotta.monitoring.PlatformServer;
import org.terracotta.monitoring.ServerState;

import java.io.Serializable;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.terracotta.monitoring.PlatformMonitoringConstants.ENTITIES_PATH;
import static org.terracotta.monitoring.PlatformMonitoringConstants.PLATFORM_PATH;
import static org.terracotta.monitoring.PlatformMonitoringConstants.STATE_NODE_NAME;

/**
 * @author Mathieu Carbou
 */
@RunWith(JUnit4.class)
public class ManagementRegistryServiceTest {

  MonitoringServiceProvider provider = new MonitoringServiceProvider();
  ManagementService managementService;
  IMonitoringProducer monitoringProducer = mock(IMonitoringProducer.class);
  IStripeMonitoring platformListener;
  IStripeMonitoring dataListener;
  long now = System.currentTimeMillis();
  PlatformServer server = new PlatformServer("server-1", "localhost", "127.0.0.1", "0.0.0.0", 9510, 9520, "version", "build", now);

  @Before
  public void setUp() throws Exception {
    provider.initialize(null, new MyPlatformConfiguration("server-1"));
    platformListener = provider.getService(0, new BasicServiceConfiguration<>(IStripeMonitoring.class));
  }

  @Test
  public void test_management_info_pushed() {

    // dataListener => per entity

    doAnswer(invocation -> {
      dataListener.pushBestEffortsData(
          server,
          (String) invocation.getArguments()[0],
          (Serializable) invocation.getArguments()[1]);
      return null;
    }).when(monitoringProducer).pushBestEffortsData(anyString(), any(Serializable.class));

    doAnswer(invocation -> dataListener.addNode(
        server,
        (String[]) invocation.getArguments()[0],
        (String) invocation.getArguments()[1],
        (Serializable) invocation.getArguments()[2])
    ).when(monitoringProducer).addNode(any(String[].class), anyString(), any(Serializable.class));

    doAnswer(invocation -> dataListener.removeNode(
        server,
        (String[]) invocation.getArguments()[0],
        (String) invocation.getArguments()[1])
    ).when(monitoringProducer).removeNode(any(String[].class), anyString());

    // simulate platform calls

    platformListener.serverDidBecomeActive(server);
    platformListener.addNode(server, PLATFORM_PATH, STATE_NODE_NAME, new ServerState("ACTIVE", now, now));
    platformListener.addNode(server, ENTITIES_PATH, "entity-1", new PlatformEntity("entityType", "entityName", 1, true));

    dataListener = provider.getService(1, new BasicServiceConfiguration<>(IStripeMonitoring.class));
    managementService = provider.getService(1, new ManagementServiceConfiguration(mock(ClientCommunicator.class)));
    ReadOnlyBuffer<Message> buffer = managementService.createMessageBuffer(100);

    // a consumer asks for a service
    ActiveEntityMonitoringService activeEntityMonitoringService = provider.getService(1, new ActiveEntityMonitoringServiceConfiguration());
    ConsumerManagementRegistry registry = provider.getService(1, new ConsumerManagementRegistryConfiguration(activeEntityMonitoringService));
    registry.addManagementProvider(new MyManagementProvider());

    // then register some objects
    registry.register(new MyObject("myCacheManagerName1", "myCacheName1"));
    registry.refresh();

    assertThat(buffer.size(), equalTo(1));
    assertThat(buffer.read().unwrap(ContextualNotification.class).get(0).getType(), equalTo("ENTITY_REGISTRY_AVAILABLE"));
    assertThat(buffer.size(), equalTo(0));

    registry.refresh();
    assertThat(buffer.size(), equalTo(0)); // registry not updated

    registry.register(new MyObject("myCacheManagerName2", "myCacheName2"));
    registry.refresh();

    assertThat(buffer.size(), equalTo(1));
    assertThat(buffer.read().unwrap(ContextualNotification.class).get(0).getType(), equalTo("ENTITY_REGISTRY_UPDATED"));
  }

}
