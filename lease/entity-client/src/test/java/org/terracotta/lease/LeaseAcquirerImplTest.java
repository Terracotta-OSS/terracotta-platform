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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.InvocationBuilder;
import org.terracotta.entity.InvokeFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class LeaseAcquirerImplTest {
  @Mock
  private EntityClientEndpoint<LeaseMessage, LeaseResponse> endpoint;

  @Mock
  private InvocationBuilder<LeaseMessage, LeaseResponse> invocationBuilder;

  @Mock
  private InvokeFuture<LeaseResponse> invokeFuture;

  @Mock
  private LeaseRequestResult leaseRequestResult;

  @Mock
  private LeaseReconnectListener reconnectListener;

  private LeaseAcquirerImpl leaseAcquirer;

  private ArgumentCaptor<LeaseMessage> leaseMessageCaptor;

  @BeforeEach
  public void before() throws Exception {
    initMocks(this);

    leaseMessageCaptor = ArgumentCaptor.forClass(LeaseMessage.class);
    when(endpoint.beginInvoke()).thenReturn(invocationBuilder);
    when(invocationBuilder.message(leaseMessageCaptor.capture())).thenReturn(invocationBuilder);
    when(invocationBuilder.replicate(any(Boolean.class))).thenReturn(invocationBuilder);
    when(invocationBuilder.ackCompleted()).thenReturn(invocationBuilder);
    when(invocationBuilder.invoke()).thenReturn(invokeFuture);
    when(invokeFuture.get()).thenReturn(leaseRequestResult);

    leaseAcquirer = new LeaseAcquirerImpl(endpoint, reconnectListener);
  }

  @Test
  public void oldConnection() throws Exception {
    assertThrows(LeaseReconnectingException.class, () -> {
      when(leaseRequestResult.isConnectionGood()).thenReturn(false);
      leaseAcquirer.acquireLease();
    });
  }

  @Test
  public void leaseNotGranted() throws Exception {
    assertThrows(LeaseException.class, () -> {
      when(leaseRequestResult.isConnectionGood()).thenReturn(true);
      when(leaseRequestResult.isLeaseGranted()).thenReturn(false);
      leaseAcquirer.acquireLease();
    });
  }

  @Test
  public void leaseGranted() throws Exception {
    when(leaseRequestResult.isConnectionGood()).thenReturn(true);
    when(leaseRequestResult.isLeaseGranted()).thenReturn(true);
    when(leaseRequestResult.getLeaseLength()).thenReturn(4000L);
    long leaseLength = leaseAcquirer.acquireLease();
    assertEquals(4000L, leaseLength);
    LeaseRequest leaseRequest = (LeaseRequest) leaseMessageCaptor.getValue();
    assertEquals(0, leaseRequest.getConnectionSequenceNumber());
  }

  @Test
  public void whenReconnectingDoesNotSendLeaseRequests() throws Exception {
    assertThrows(LeaseReconnectingException.class, () -> {
      leaseAcquirer.reconnecting();
      verify(reconnectListener).reconnecting();
      leaseAcquirer.acquireLease();
    });
  }

  @Test
  public void afterReconnectionSendsLeaseRequestsWithHigherConnectionSequenceNumber() throws Exception {
    when(leaseRequestResult.isConnectionGood()).thenReturn(true);
    when(leaseRequestResult.isLeaseGranted()).thenReturn(true);
    when(leaseRequestResult.getLeaseLength()).thenReturn(4000L);
    leaseAcquirer.reconnecting();
    leaseAcquirer.reconnected();
    verify(reconnectListener).reconnected();
    leaseAcquirer.acquireLease();
    long leaseLength = leaseAcquirer.acquireLease();
    assertEquals(4000L, leaseLength);
    LeaseRequest leaseRequest = (LeaseRequest) leaseMessageCaptor.getValue();
    assertEquals(1, leaseRequest.getConnectionSequenceNumber());
  }

  @Test
  public void reconnectData() {
    LeaseReconnectData reconnectData = leaseAcquirer.getReconnectData();
    assertEquals(0, reconnectData.getConnectionSequenceNumber());
  }

  @Test
  public void reconnectDataAfterReconnectStarted() {
    leaseAcquirer.reconnecting();
    LeaseReconnectData reconnectData = leaseAcquirer.getReconnectData();
    assertEquals(1, reconnectData.getConnectionSequenceNumber());
  }

  @Test
  public void reconnectDataAfterReconnectFinished() {
    leaseAcquirer.reconnecting();
    leaseAcquirer.reconnected();
    LeaseReconnectData reconnectData = leaseAcquirer.getReconnectData();
    assertEquals(1, reconnectData.getConnectionSequenceNumber());
  }

  @Test
  public void reconnectDataAfterSecondReconnectStarted() {
    leaseAcquirer.reconnecting();
    leaseAcquirer.reconnected();
    leaseAcquirer.reconnecting();
    LeaseReconnectData reconnectData = leaseAcquirer.getReconnectData();
    assertEquals(2, reconnectData.getConnectionSequenceNumber());
  }
}
