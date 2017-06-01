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

import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;

import java.nio.ByteBuffer;

class StateCollectorMessage implements EntityMessage, EntityResponse {
  private final StateCollectorMessageType type;

  public StateCollectorMessage(final StateCollectorMessageType type) {this.type = type;}
  
  public static StateCollectorMessage decode(byte[] bytes) {
    ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
    int ordinal = byteBuffer.getInt();
    return new StateCollectorMessage(StateCollectorMessageType.values()[ordinal]);
  }
  
  public byte[] encode() {
    ByteBuffer byteBuffer = ByteBuffer.allocate(Integer.SIZE / Byte.SIZE);
    byteBuffer.putInt(type.ordinal());
    return byteBuffer.array();
  }

  public StateCollectorMessageType getType() {
    return type;
  }
}
