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

import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.InvokeFuture;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.exception.EntityException;

import java.util.concurrent.atomic.AtomicLong;

class LeaseAcquirerImpl implements LeaseAcquirer, LeaseReconnectListener, LeaseReconnectDataSupplier {
  private final EntityClientEndpoint<LeaseMessage, LeaseResponse> endpoint;
  private final LeaseReconnectListener reconnectListener;
  private final AtomicLong connectionSequenceNumber = new AtomicLong();
  private volatile boolean reconnecting;

  LeaseAcquirerImpl(EntityClientEndpoint<LeaseMessage, LeaseResponse> endpoint, LeaseReconnectListener reconnectListener) {
    this.endpoint = endpoint;
    this.reconnectListener = reconnectListener;
    endpoint.setDelegate(new LeaseEndpointDelegate(this, this));
  }

  @Override
  public long acquireLease() throws LeaseException, InterruptedException {
    long currentConnectionSequenceNumber = connectionSequenceNumber.get();

    if (reconnecting) {
      throw new LeaseReconnectingException("Will not attempt to acquire a lease as a reconnection is taking place");
    }

    try {
      InvokeFuture<LeaseResponse> invokeFuture = endpoint.beginInvoke()
              .message(new LeaseRequest(currentConnectionSequenceNumber))
              .replicate(false)
              .ackCompleted()
              .invoke();

      LeaseRequestResult leaseRequestResult = (LeaseRequestResult) invokeFuture.get();

      if (!leaseRequestResult.isConnectionGood()) {
        throw new LeaseReconnectingException("Attempted to acquire a lease but fail-over occurred");
      }

      if (!leaseRequestResult.isLeaseGranted()) {
        throw new LeaseException("Unable to obtain lease, the connection is being closed because the lease was not renewed soon enough");
      }

      return leaseRequestResult.getLeaseLength();
    } catch (MessageCodecException e) {
      throw new LeaseException(e);
    } catch (EntityException e) {
      throw new LeaseException(e);
    }
  }

  @Override
  public void close() {
    endpoint.close();
  }

  @Override
  public void reconnecting() {
    reconnecting = true;
    connectionSequenceNumber.incrementAndGet();
    reconnectListener.reconnecting();
  }

  @Override
  public void reconnected() {
    reconnecting = false;
    reconnectListener.reconnected();
  }

  @Override
  public LeaseReconnectData getReconnectData() {
    return new LeaseReconnectData(connectionSequenceNumber.get());
  }
}
