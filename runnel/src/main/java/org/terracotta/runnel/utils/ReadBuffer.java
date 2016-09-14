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

  public ReadBuffer(ByteBuffer byteBuffer) {
    this.byteBuffer = byteBuffer;
  }

  public long getLong() {
    return byteBuffer.getLong();
  }

  public int getInt() {
    return byteBuffer.getInt();
  }

  public int getVlqInt() {
    return VLQ.decode(byteBuffer);
  }

  public ByteBuffer getByteBuffer(int len) {
    ByteBuffer slice = byteBuffer.slice();
    slice.limit(len);
    return slice;
  }

  public String getString(int len) {
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
}
