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

class LeaseAcquirerImpl implements LeaseAcquirer {
  private final EntityClientEndpoint<LeaseRequest, LeaseResponse> endpoint;

  LeaseAcquirerImpl(EntityClientEndpoint<LeaseRequest, LeaseResponse> endpoint) {
    this.endpoint = endpoint;
  }

  @Override
  public long acquireLease() throws LeaseException, InterruptedException {
    try {
      InvokeFuture<LeaseResponse> invokeFuture = endpoint.beginInvoke()
              .message(new LeaseRequest())
              .replicate(false)
              .ackCompleted()
              .invoke();

      LeaseResponse leaseResponse = invokeFuture.get();

      if (!leaseResponse.isLeaseGranted()) {
        throw new LeaseException("Unable to obtain lease, the connection is being closed because the lease was not renewed soon enough");
      }

      return leaseResponse.getLeaseLength();
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
}
