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
import org.terracotta.entity.EntityClientService;
import org.terracotta.entity.MessageCodec;

public class LeaseAcquirerClientService implements EntityClientService<LeaseAcquirer, Void, LeaseMessage, LeaseResponse, LeaseReconnectListener> {
  @Override
  public boolean handlesEntityType(Class<LeaseAcquirer> c) {
    return LeaseAcquirer.class.equals(c);
  }

  @Override
  public byte[] serializeConfiguration(Void aVoid) {
    return new byte[0];
  }

  @Override
  public Void deserializeConfiguration(byte[] bytes) {
    return null;
  }

  @Override
  public LeaseAcquirer create(EntityClientEndpoint<LeaseMessage, LeaseResponse> endpoint, LeaseReconnectListener reconnectListener) {
    return new LeaseAcquirerImpl(endpoint, reconnectListener);
  }

  @Override
  public MessageCodec<LeaseMessage, LeaseResponse> getMessageCodec() {
    return new LeaseAcquirerCodec();
  }
}
