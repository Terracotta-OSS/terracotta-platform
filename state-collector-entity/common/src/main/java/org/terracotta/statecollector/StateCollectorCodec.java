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

import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.MessageCodecException;

class StateCollectorCodec implements MessageCodec<StateCollectorMessage, StateCollectorMessage> {

  @Override
  public byte[] encodeMessage(final StateCollectorMessage stateCollectorMessage) throws MessageCodecException {
    return stateCollectorMessage.encode();
  }

  @Override
  public StateCollectorMessage decodeMessage(final byte[] bytes) throws MessageCodecException {
    return StateCollectorMessage.decode(bytes);
  }

  @Override
  public byte[] encodeResponse(final StateCollectorMessage stateCollectorMessage) throws MessageCodecException {
    return stateCollectorMessage.encode();
  }

  @Override
  public StateCollectorMessage decodeResponse(final byte[] bytes) throws MessageCodecException {
    return StateCollectorMessage.decode(bytes);
  }
}
