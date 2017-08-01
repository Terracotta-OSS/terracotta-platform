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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.terracotta.entity.ActiveInvokeContext;
import org.terracotta.entity.ClientCommunicator;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.IEntityMessenger;
import org.terracotta.lease.service.LeaseResult;
import org.terracotta.lease.service.LeaseService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ActiveLeaseAcquirerTest {
  @Mock
  private LeaseService leaseService;

  @Mock
  private ClientCommunicator clientCommunicator;

  @Mock
  private IEntityMessenger entityMessenger;

  @Mock
  private ClientDescriptor clientDescriptor;

  @Mock
  private ActiveInvokeContext context;

  @Mock
  private ActiveInvokeContext serverContext;

  @Mock
  private LeaseResult leaseResult;

  private ArgumentCaptor<LeaseMessage> selfMessageCaptor;

  @Before
  public void before() throws Exception {
    selfMessageCaptor = ArgumentCaptor.forClass(LeaseMessage.class);
    doNothing().when(entityMessenger).messageSelf(selfMessageCaptor.capture());
    when(context.getClientDescriptor()).thenReturn(clientDescriptor);
  }

  @Test
  public void delegatesDisconnectedToService() {
    ActiveLeaseAcquirer leaseAcquirer = new ActiveLeaseAcquirer(leaseService, clientCommunicator, entityMessenger);
    leaseAcquirer.disconnected(clientDescriptor);

    verify(leaseService).disconnected(clientDescriptor);
  }

  @Test
  public void usesServiceToHandleInvokeSuccess() throws Exception {
    when(leaseService.acquireLease(clientDescriptor)).thenReturn(leaseResult);
    when(leaseResult.isLeaseGranted()).thenReturn(true);
    when(leaseResult.getLeaseLength()).thenReturn(300L);

    ActiveLeaseAcquirer leaseAcquirer = new ActiveLeaseAcquirer(leaseService, clientCommunicator, entityMessenger);
    LeaseRequestResult response = (LeaseRequestResult) leaseAcquirer.invokeActive(context, new LeaseRequest(0));

    assertTrue(response.isConnectionGood());
    assertTrue(response.isLeaseGranted());
    assertEquals(300L, response.getLeaseLength());
  }

  @Test
  public void usesServiceToHandleInvokeFail() throws Exception {
    when(leaseService.acquireLease(clientDescriptor)).thenReturn(leaseResult);
    when(leaseResult.isLeaseGranted()).thenReturn(false);

    ActiveLeaseAcquirer leaseAcquirer = new ActiveLeaseAcquirer(leaseService, clientCommunicator, entityMessenger);
    LeaseRequestResult response = (LeaseRequestResult) leaseAcquirer.invokeActive(context, new LeaseRequest(0));

    assertTrue(response.isConnectionGood());
    assertFalse(response.isLeaseGranted());
  }

  @Test
  public void tellsServiceAboutReconnectionStarting() {
    ActiveLeaseAcquirer leaseAcquirer = new ActiveLeaseAcquirer(leaseService, clientCommunicator, entityMessenger);
    leaseAcquirer.connected(clientDescriptor);
    leaseAcquirer.handleReconnect(clientDescriptor, new LeaseReconnectData(1).encode());

    verify(leaseService).reconnecting(clientDescriptor);
    verifyNoMoreInteractions(leaseService);

    assertEquals(LeaseReconnectFinished.class, selfMessageCaptor.getValue().getClass());
  }

  @Test
  public void tellsServiceAboutReconnectionFinishing() throws Exception {
    ActiveLeaseAcquirer leaseAcquirer = new ActiveLeaseAcquirer(leaseService, clientCommunicator, entityMessenger);
    leaseAcquirer.connected(clientDescriptor);
    leaseAcquirer.handleReconnect(clientDescriptor, new LeaseReconnectData(1).encode());
    verify(leaseService).reconnecting(clientDescriptor);

    LeaseMessage selfMessage = selfMessageCaptor.getValue();
    leaseAcquirer.invokeActive(serverContext, selfMessage);

    verify(leaseService).reconnected(clientDescriptor);
    verifyNoMoreInteractions(leaseService);

    verify(clientCommunicator).sendNoResponse(eq(clientDescriptor), any(LeaseAcquirerAvailable.class));
  }

  @Test
  public void rejectsLeaseRequestsSentOnOldConnections() throws Exception {
    ActiveLeaseAcquirer leaseAcquirer = new ActiveLeaseAcquirer(leaseService, clientCommunicator, entityMessenger);
    leaseAcquirer.handleReconnect(clientDescriptor, new LeaseReconnectData(1).encode());
    LeaseRequestResult response = (LeaseRequestResult) leaseAcquirer.invokeActive(context, new LeaseRequest(0));

    assertFalse(response.isConnectionGood());
  }
}
