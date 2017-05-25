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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.InvocationBuilder;
import org.terracotta.entity.InvokeFuture;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LeaseAcquirerImplTest {
  @Mock
  private EntityClientEndpoint<LeaseRequest, LeaseResponse> endpoint;

  @Mock
  private InvocationBuilder<LeaseRequest, LeaseResponse> invocationBuilder;

  @Mock
  private InvokeFuture<LeaseResponse> invokeFuture;

  @Mock
  private LeaseResponse leaseResponse;

  private LeaseAcquirerImpl leaseAcquirer;

  @Before
  public void before() throws Exception {
    when(endpoint.beginInvoke()).thenReturn(invocationBuilder);
    when(invocationBuilder.message(any(LeaseRequest.class))).thenReturn(invocationBuilder);
    when(invocationBuilder.replicate(any(Boolean.class))).thenReturn(invocationBuilder);
    when(invocationBuilder.ackCompleted()).thenReturn(invocationBuilder);
    when(invocationBuilder.invoke()).thenReturn(invokeFuture);
    when(invokeFuture.get()).thenReturn(leaseResponse);

    leaseAcquirer = new LeaseAcquirerImpl(endpoint);
  }

  @Test(expected = LeaseException.class)
  public void leaseNotGranted() throws Exception {
    when(leaseResponse.isLeaseGranted()).thenReturn(false);
    leaseAcquirer.acquireLease();
  }

  @Test
  public void leaseGranted() throws Exception {
    when(leaseResponse.isLeaseGranted()).thenReturn(true);
    when(leaseResponse.getLeaseLength()).thenReturn(4000L);
    long leaseLength = leaseAcquirer.acquireLease();
    assertEquals(4000L, leaseLength);
  }
}
