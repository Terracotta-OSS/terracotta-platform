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
import org.terracotta.management.model.capabilities.Capability;
import org.terracotta.management.model.context.ContextContainer;
import org.terracotta.management.model.message.Message;
import org.terracotta.monitoring.IMonitoringProducer;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * @author Mathieu Carbou
 */
@RunWith(JUnit4.class)
public class ManagementRegistryServiceTest {

  @Test
  public void test_management_info_pushed() {
    ManagementRegistryServiceProvider provider = new ManagementRegistryServiceProvider();
    IMonitoringProducer monitoringProducer = mock(IMonitoringProducer.class);

    // a consumer asks for a service
    ConsumerManagementRegistry registry = provider.getService(0, new ConsumerManagementRegistryConfiguration(monitoringProducer)
        .addProvider(new MyManagementProvider()));

    // then register some objects
    registry.register(new MyObject("myCacheManagerName", "myCacheName1"));

    verifyZeroInteractions(monitoringProducer);

    registry.refresh();

    verify(monitoringProducer).addNode(new String[0], "registry", null);
    verify(monitoringProducer).addNode(new String[]{"registry"}, "contextContainer", new ContextContainer("entityConsumerId", "0"));
    verify(monitoringProducer).addNode(eq(new String[]{"registry"}), eq("capabilities"), any(Capability[].class));

    verify(monitoringProducer).pushBestEffortsData(eq("entity-notifications"), any(Message.class));
    verifyNoMoreInteractions(monitoringProducer);
  }

}
