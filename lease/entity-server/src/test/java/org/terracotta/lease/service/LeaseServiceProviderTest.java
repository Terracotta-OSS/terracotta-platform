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
package org.terracotta.lease.service;

import com.tc.classloader.BuiltinService;
import org.junit.Test;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.PlatformConfiguration;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProviderConfiguration;
import org.terracotta.lease.MockStateDumpCollector;
import org.terracotta.lease.TestTimeSource;
import org.terracotta.lease.TimeSourceProvider;
import org.terracotta.lease.service.closer.ClientConnectionCloser;
import org.terracotta.lease.service.config.LeaseConfiguration;

import java.util.Collection;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

public class LeaseServiceProviderTest {
  @Test
  public void serviceTypes() {
    LeaseServiceProvider serviceProvider = new LeaseServiceProvider();
    Collection<Class<?>> serviceTypes = serviceProvider.getProvidedServiceTypes();
    assertEquals(1, serviceTypes.size());
    assertEquals(LeaseService.class, serviceTypes.iterator().next());
  }

  @Test
  public void singleLeaseConfigured() throws Exception {
    testLeaseLengths(1500L, new LeaseConfiguration(1500L));
  }

  @Test
  public void noLeaseConfigured() throws Exception {
    testLeaseLengths(150_000L, () -> LeaseServiceProvider.class);
  }

  private void testLeaseLengths(long expectedLeaseLength, ServiceProviderConfiguration configuredLease) throws Exception {
    TestTimeSource timeSource = spy(new TestTimeSource());
    TimeSourceProvider.setTimeSource(timeSource);
    PlatformConfiguration platformConfiguration = mock(PlatformConfiguration.class);
    ClientConnectionCloser closer = mock(ClientConnectionCloser.class);
    ServiceConfiguration<LeaseService> serviceConfiguration = new LeaseServiceConfiguration(closer);
    ClientDescriptor clientDescriptor = mock(ClientDescriptor.class);

    LeaseServiceProvider serviceProvider = new LeaseServiceProvider();
    serviceProvider.initialize(configuredLease, platformConfiguration);
    LeaseService service = serviceProvider.getService(1L, serviceConfiguration);

    LeaseResult leaseResult = service.acquireLease(clientDescriptor);
    assertTrue(leaseResult.isLeaseGranted());
    assertEquals(expectedLeaseLength, leaseResult.getLeaseLength());

    verify(timeSource, timeout(1000L)).sleep(200L);

    timeSource.tickMillis(expectedLeaseLength);
    timeSource.tickMillis(100L);
    verify(closer, timeout(1000L)).closeClientConnection(clientDescriptor);
  }

  @Test
  public void isBuiltinService() {
    assertNotNull(LeaseServiceProvider.class.getAnnotation(BuiltinService.class));
  }

  @Test
  public void testStateDump() throws Exception {
    TestTimeSource timeSource = spy(new TestTimeSource());
    TimeSourceProvider.setTimeSource(timeSource);
    PlatformConfiguration platformConfiguration = mock(PlatformConfiguration.class);

    LeaseConfiguration providerConfig = new LeaseConfiguration(1500L);
    LeaseServiceProvider serviceProvider = new LeaseServiceProvider();
    serviceProvider.initialize(providerConfig, platformConfiguration);

    MockStateDumpCollector dumper = new MockStateDumpCollector();
    serviceProvider.addStateTo(dumper);
    assertThat(dumper.getMapping("LeaseLength"), is("1500"));
    assertThat(dumper.getMapping("LeaseState"), notNullValue());
  }

}
