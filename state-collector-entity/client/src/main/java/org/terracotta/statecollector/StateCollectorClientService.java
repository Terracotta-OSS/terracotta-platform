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
package org.terracotta.statecollector;

import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.EntityClientService;
import org.terracotta.entity.MessageCodec;

public class StateCollectorClientService implements EntityClientService<StateCollector, Void, StateCollectorMessage, StateCollectorMessage, Object>{
  @Override
  public boolean handlesEntityType(final Class<StateCollector> requestedType) {
    return StateCollector.class.equals(requestedType);
  }

  @Override
  public byte[] serializeConfiguration(final Void voidType) {
    return new byte[0];
  }

  @Override
  public Void deserializeConfiguration(final byte[] bytes) {
    return null;
  }

  @Override
  public StateCollectorImpl create(final EntityClientEndpoint<StateCollectorMessage, StateCollectorMessage> entityClientEndpoint, final Object o) {
    return new StateCollectorImpl(entityClientEndpoint);
  }

  @Override
  public MessageCodec<StateCollectorMessage, StateCollectorMessage> getMessageCodec() {
    return new StateCollectorCodec();
  }
}
