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

import org.junit.Test;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.lease.service.monitor.LeaseState;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LeaseServiceImplTest {
  @Test
  public void acquireLease() {
    LeaseState leaseState = mock(LeaseState.class);
    ClientDescriptor clientDescriptor = mock(ClientDescriptor.class);

    when(leaseState.acquireLease(clientDescriptor, 1000L)).thenReturn(true);

    LeaseServiceImpl leaseService = new LeaseServiceImpl(1000L, leaseState);
    LeaseResult leaseResult = leaseService.acquireLease(clientDescriptor);

    assertTrue(leaseResult.isLeaseGranted());
    assertEquals(1000L, leaseResult.getLeaseLength());
  }

  @Test
  public void failToAcquireLease() {
    LeaseState leaseState = mock(LeaseState.class);
    ClientDescriptor clientDescriptor = mock(ClientDescriptor.class);

    when(leaseState.acquireLease(clientDescriptor, 1000L)).thenReturn(false);

    LeaseServiceImpl leaseService = new LeaseServiceImpl(1000L, leaseState);
    LeaseResult leaseResult = leaseService.acquireLease(clientDescriptor);

    assertFalse(leaseResult.isLeaseGranted());
  }

  @Test
  public void delegatesDisconnected() {
    LeaseState leaseState = mock(LeaseState.class);
    ClientDescriptor clientDescriptor = mock(ClientDescriptor.class);

    LeaseServiceImpl leaseService = new LeaseServiceImpl(1000L, leaseState);
    leaseService.disconnected(clientDescriptor);

    verify(leaseState).disconnected(clientDescriptor);
  }

  @Test
  public void delegatesReconnecting() {
    LeaseState leaseState = mock(LeaseState.class);
    ClientDescriptor clientDescriptor = mock(ClientDescriptor.class);

    LeaseServiceImpl leaseService = new LeaseServiceImpl(1000L, leaseState);
    leaseService.reconnecting(clientDescriptor);

    verify(leaseState).reconnecting(clientDescriptor);
  }

  @Test
  public void delegatesReconnected() {
    LeaseState leaseState = mock(LeaseState.class);
    ClientDescriptor clientDescriptor = mock(ClientDescriptor.class);

    LeaseServiceImpl leaseService = new LeaseServiceImpl(1000L, leaseState);
    leaseService.reconnected(clientDescriptor);

    verify(leaseState).reconnected(clientDescriptor, 1000L);
  }
}
