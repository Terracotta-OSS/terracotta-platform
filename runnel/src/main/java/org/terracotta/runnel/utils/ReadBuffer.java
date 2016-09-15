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
public class ReadBuffer {

  private final ByteBuffer byteBuffer;
  private final int origin;
  private final int limit;

  public ReadBuffer(ByteBuffer byteBuffer) {
    this(byteBuffer, byteBuffer.position() + byteBuffer.capacity());
  }

  private ReadBuffer(ByteBuffer byteBuffer, int limit) {
    this.byteBuffer = byteBuffer;
    this.origin = byteBuffer.position();
    this.limit = origin + limit;
    if (this.limit > byteBuffer.capacity()) {
      throw new BufferLimitReachedException();
    }
  }

  public long getLong() {
    if (byteBuffer.position() + 8 > limit) {
      throw new BufferLimitReachedException();
    }
    return byteBuffer.getLong();
  }

  public int getInt() {
    if (byteBuffer.position() + 4 > limit) {
      throw new BufferLimitReachedException();
    }
    return byteBuffer.getInt();
  }

  public int getVlqInt() {
    return VLQ.decode(this);
  }

  public byte getByte() {
    if (byteBuffer.position() + 1 > limit) {
      throw new BufferLimitReachedException();
    }
    return byteBuffer.get();
  }

  public ByteBuffer getByteBuffer(int size) {
    if (byteBuffer.position() + size > limit) {
      throw new BufferLimitReachedException();
    }
    ByteBuffer slice = byteBuffer.slice();
    slice.limit(size);
    byteBuffer.position(byteBuffer.position() + size);
    return slice;
  }

  public String getString(int size) {
    if (byteBuffer.position() + size > limit) {
      throw new BufferLimitReachedException();
    }
    char[] chars = new char[size / 2];
    for (int i = 0; i < chars.length; i++) {
      chars[i] = byteBuffer.getChar();
    }
    return new String(chars);
  }

  public boolean limitReached() {
    return byteBuffer.position() == limit;
  }

  public void skipAll() {
    byteBuffer.position(limit);
  }

  public void skip(int size) {
    if (size < 0) {
      throw new IllegalArgumentException("size cannot be < 0");
    }
    int targetPosition = byteBuffer.position() + size;
    if (targetPosition > limit) {
      throw new BufferLimitReachedException();
    }
    byteBuffer.position(targetPosition);
  }

  public void rewind(int size) {
    if (size < 0) {
      throw new IllegalArgumentException("size cannot be < 0");
    }
    int targetPosition = byteBuffer.position() - size;
    if (targetPosition < origin) {
      throw new BufferLimitReachedException();
    }
    byteBuffer.position(targetPosition);
  }

  public ReadBuffer limit(int size) {
    if (size < 0) {
      throw new IllegalArgumentException("size cannot be < 0");
    }
    return new ReadBuffer(byteBuffer, size);
  }
}
