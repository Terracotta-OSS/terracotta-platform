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

import org.junit.Test;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.lease.service.LeaseResult;
import org.terracotta.lease.service.LeaseService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ActiveLeaseAcquirerTest {
  @Test
  public void delegatesDisconnectedToService() {
    LeaseService leaseService = mock(LeaseService.class);
    ClientDescriptor clientDescriptor = mock(ClientDescriptor.class);

    ActiveLeaseAcquirer leaseAcquirer = new ActiveLeaseAcquirer(leaseService);
    leaseAcquirer.disconnected(clientDescriptor);

    verify(leaseService).disconnected(clientDescriptor);
  }

  @Test
  public void usesServiceToHandleInvokeSuccess() {
    LeaseService leaseService = mock(LeaseService.class);
    ClientDescriptor clientDescriptor = mock(ClientDescriptor.class);

    LeaseResult leaseResult = mock(LeaseResult.class);
    when(leaseService.acquireLease(clientDescriptor)).thenReturn(leaseResult);
    when(leaseResult.isLeaseGranted()).thenReturn(true);
    when(leaseResult.getLeaseLength()).thenReturn(300L);

    ActiveLeaseAcquirer leaseAcquirer = new ActiveLeaseAcquirer(leaseService);
    LeaseResponse response = leaseAcquirer.invoke(clientDescriptor, new LeaseRequest());

    assertTrue(response.isLeaseGranted());
    assertEquals(300L, response.getLeaseLength());
  }

  @Test
  public void usesServiceToHandleInvokeFail() {
    LeaseService leaseService = mock(LeaseService.class);
    ClientDescriptor clientDescriptor = mock(ClientDescriptor.class);

    LeaseResult leaseResult = mock(LeaseResult.class);
    when(leaseService.acquireLease(clientDescriptor)).thenReturn(leaseResult);
    when(leaseResult.isLeaseGranted()).thenReturn(false);

    ActiveLeaseAcquirer leaseAcquirer = new ActiveLeaseAcquirer(leaseService);
    LeaseResponse response = leaseAcquirer.invoke(clientDescriptor, new LeaseRequest());

    assertFalse(response.isLeaseGranted());
  }
}
