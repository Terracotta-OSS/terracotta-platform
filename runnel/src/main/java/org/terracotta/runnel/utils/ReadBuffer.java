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
  private final int limit;

  public ReadBuffer(ByteBuffer byteBuffer) {
    this(byteBuffer, Integer.MAX_VALUE);
  }

  private ReadBuffer(ByteBuffer byteBuffer, int limit) {
    this.byteBuffer = byteBuffer;
    this.limit = byteBuffer.position() + limit;
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

  public ByteBuffer getByteBuffer(int len) {
    if (byteBuffer.position() + len > limit) {
      throw new BufferLimitReachedException();
    }
    ByteBuffer slice = byteBuffer.slice();
    slice.limit(len);
    return slice;
  }

  public String getString(int len) {
    if (byteBuffer.position() + len > limit) {
      throw new BufferLimitReachedException();
    }
    char[] chars = new char[len / 2];
    for (int i = 0; i < chars.length; i++) {
      chars[i] = byteBuffer.getChar();
    }
    return new String(chars);
  }

  public void skip(int len) {
    if (len < 0) {
      throw new IllegalArgumentException("len cannot be < 0");
    }
    if (byteBuffer.position() + len > limit) {
      throw new BufferLimitReachedException();
    }
    byteBuffer.position(byteBuffer.position() + len);
  }

  public void rewind(int len) {
    if (len < 0) {
      throw new IllegalArgumentException("len cannot be < 0");
    }
    byteBuffer.position(byteBuffer.position() - len);
  }

  public int position() {
    return byteBuffer.position();
  }

  public ReadBuffer limit(int maxSize) {
    return new ReadBuffer(byteBuffer, maxSize);
  }
}
