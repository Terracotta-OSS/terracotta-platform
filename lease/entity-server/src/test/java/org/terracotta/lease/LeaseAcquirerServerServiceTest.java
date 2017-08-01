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
package org.terracotta.lease;

import com.tc.classloader.PermanentEntity;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.terracotta.entity.ClientCommunicator;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.ConcurrencyStrategy;
import org.terracotta.entity.IEntityMessenger;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceRegistry;
import org.terracotta.lease.service.LeaseService;
import org.terracotta.lease.service.LeaseServiceConfiguration;
import org.terracotta.lease.service.closer.ClientConnectionCloser;

import java.util.Collections;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LeaseAcquirerServerServiceTest {
  @Test
  public void handlesEntityType() {
    LeaseAcquirerServerService serverService = new LeaseAcquirerServerService();
    assertTrue(serverService.handlesEntityType(LeaseAcquirer.class.getCanonicalName()));
  }

  @Test
  public void isPermanentEntity() {
    PermanentEntity permanentEntity = LeaseAcquirerServerService.class.getAnnotation(PermanentEntity.class);
    assertEquals(LeaseAcquirer.class.getCanonicalName(), permanentEntity.type());
    assertEquals(1, permanentEntity.names().length);
    assertEquals("SystemLeaseAcquirer", permanentEntity.names()[0]);
    assertEquals(1L, permanentEntity.version());
  }

  @Test
  public void concurrencyStrategy() {
    LeaseAcquirerServerService serverService = new LeaseAcquirerServerService();
    ConcurrencyStrategy<LeaseMessage> concurrencyStrategy = serverService.getConcurrencyStrategy(new byte[0]);
    assertEquals(ConcurrencyStrategy.UNIVERSAL_KEY, concurrencyStrategy.concurrencyKey(new LeaseRequest(0)));
    assertEquals(ConcurrencyStrategy.MANAGEMENT_KEY, concurrencyStrategy.concurrencyKey(new LeaseReconnectFinished(UUID.randomUUID())));
  }

  @Test
  public void codec() {
    LeaseAcquirerServerService serverService = new LeaseAcquirerServerService();
    assertEquals(LeaseAcquirerCodec.class, serverService.getMessageCodec().getClass());
  }

  @Test
  public void activeEntity() throws Exception {
    ServiceRegistry serviceRegistry = mock(ServiceRegistry.class);
    ClientCommunicator clientCommunicator = mock(ClientCommunicator.class);
    IEntityMessenger entityMessenger = mock(IEntityMessenger.class);
    LeaseService leaseService = mock(LeaseService.class);
    ClientDescriptor clientDescriptor = mock(ClientDescriptor.class);

    ArgumentCaptor<LeaseServiceConfiguration> configurationCaptor = ArgumentCaptor.forClass(LeaseServiceConfiguration.class);
    when(serviceRegistry.getService(configurationCaptor.capture())).thenReturn(leaseService);
    when(serviceRegistry.getService(argThat(serviceType(ClientCommunicator.class)))).thenReturn(clientCommunicator);
    when(serviceRegistry.getService(argThat(serviceType(IEntityMessenger.class)))).thenReturn(entityMessenger);

    LeaseAcquirerServerService serverService = new LeaseAcquirerServerService();
    ActiveLeaseAcquirer activeEntity = (ActiveLeaseAcquirer) serverService.createActiveEntity(serviceRegistry, new byte[0]);

    LeaseServiceConfiguration configuration = configurationCaptor.getValue();
    ClientConnectionCloser clientConnectionCloser = configuration.getClientConnectionCloser();
    clientConnectionCloser.closeClientConnection(clientDescriptor);
    verify(clientCommunicator).closeClientConnection(clientDescriptor);

    activeEntity.disconnected(clientDescriptor);
    verify(leaseService).disconnected(clientDescriptor);
  }

  private static <T> Matcher<ServiceConfiguration<T>> serviceType(Class<T> serviceType) {
    return new ServiceTypeMatcher<T>(serviceType);
  }

  private static class ServiceTypeMatcher<T> extends FeatureMatcher<ServiceConfiguration<T>, Class> {
    ServiceTypeMatcher(Class serviceType) {
      super(equalTo(serviceType), "serviceType", "serviceType");
    }

    @Override
    protected Class featureValueOf(ServiceConfiguration serviceConfiguration) {
      return serviceConfiguration.getServiceType();
    }
  }
}
