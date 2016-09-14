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
package org.terracotta.runnel.utils;

import java.nio.ByteBuffer;

/**
 * @author Ludovic Orban
 */
public class WriteBuffer {

  private final ByteBuffer byteBuffer;

  public WriteBuffer(ByteBuffer byteBuffer) {
    this.byteBuffer = byteBuffer;
  }

  public void putLong(long value) {
    byteBuffer.putLong(value);
  }

  public void putInt(int value) {
    byteBuffer.putInt(value);
  }

  public void putVlqInt(int value) {
    VLQ.encode(value, byteBuffer);
  }

  public void putByteBuffer(ByteBuffer buffer) {
    byteBuffer.put(buffer);
  }

  public void putString(String value) {
    for (int i = 0; i < value.length(); i++) {
      byteBuffer.putChar(value.charAt(i));
    }
  }
}
