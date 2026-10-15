/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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

import java.io.UTFDataFormatException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * @author Ludovic Orban
 */
public final class ReadBuffer {

  private final ByteBuffer byteBuffer;
  private int limit;

  public ReadBuffer(ByteBuffer byteBuffer) {
    this(byteBuffer, byteBuffer.remaining());
  }

  private ReadBuffer(ByteBuffer byteBuffer, int limit) {
    this.byteBuffer = byteBuffer;
    setLimit(limit);
  }

  public Boolean getBoolean() {
    return getBooleanValue() ? Boolean.TRUE : Boolean.FALSE;
  }

  public boolean getBooleanValue() {
    if (byteBuffer.position() + 1 > limit) {
      throw new LimitReachedException();
    }
    return byteBuffer.get() != (byte) 0;
  }

  public Double getDouble() {
    return getDoubleValue();
  }

  public double getDoubleValue() {
    if (byteBuffer.position() + 8 > limit) {
      throw new LimitReachedException();
    }
    return byteBuffer.getDouble();
  }

  public Long getLong() {
    return getLongValue();
  }

  public long getLongValue() {
    if (byteBuffer.position() + 8 > limit) {
      throw new LimitReachedException();
    }
    return byteBuffer.getLong();
  }

  public Character getChar() {
    return getCharValue();
  }

  public char getCharValue() {
    if (byteBuffer.position() + 2 > limit) {
      throw new LimitReachedException();
    }
    return byteBuffer.getChar();
  }

  public Integer getInt() {
    return getIntValue();
  }

  public int getIntValue() {
    if (byteBuffer.position() + 4 > limit) {
      throw new LimitReachedException();
    }
    return byteBuffer.getInt();
  }

  public int getVlqInt() {
    return VLQ.decode(this);
  }

  byte getByte() {
    if (byteBuffer.position() + 1 > limit) {
      throw new LimitReachedException();
    }
    return byteBuffer.get();
  }

  public ByteBuffer getByteBuffer(int size) {
    if (byteBuffer.position() + size > limit) {
      throw new LimitReachedException();
    }
    ByteBuffer slice = byteBuffer.slice();
    slice.limit(size);
    byteBuffer.position(byteBuffer.position() + size);
    return slice;
  }

  public String getString(int size) {
    if (byteBuffer.position() + size > limit) {
      throw new LimitReachedException();
    }

    /*
     * Fast path: the modified UTF-8 encoding of a string containing only 7-bit ASCII characters
     * is the character bytes themselves. Scan the raw bytes once; if none have the high bit set,
     * decode the whole range in a single String construction (no intermediate decoder, CharBuffer
     * or slice). All bytes below 0x80 decode identically under US-ASCII and modified UTF-8.
     * The scan is a branchless OR-reduction, which JITs auto-vectorize.
     * Otherwise, at least one byte is >= 0x80 (exactly the inputs on which a US-ASCII decode
     * attempt would fail), so decode straight away as modified UTF-8.
     */
    ByteBuffer encoded;
    if (byteBuffer.hasArray()) {
      byte[] array = byteBuffer.array();
      int start = byteBuffer.arrayOffset() + byteBuffer.position();
      if (isAscii(array, start, size)) {
        byteBuffer.position(byteBuffer.position() + size);
        return new String(array, start, size, StandardCharsets.US_ASCII);
      }
      encoded = byteBuffer.slice();
      encoded.limit(size);
    } else {
      /*
       * Non-array-backed (e.g. direct) buffer: bulk-read into a scratch array without moving
       * this buffer's position, then decode from that array.
       */
      ByteBuffer duplicate = byteBuffer.duplicate();
      byte[] data = new byte[size];
      duplicate.get(data, 0, size);
      if (isAscii(data, 0, size)) {
        byteBuffer.position(byteBuffer.position() + size);
        return new String(data, 0, size, StandardCharsets.US_ASCII);
      }
      encoded = ByteBuffer.wrap(data);
    }

    String s = decodeModifiedUtf8(encoded, size);
    byteBuffer.position(byteBuffer.position() + size);
    return s;
  }

  /**
   * @return true if every one of {@code data[start .. start+length)} is a 7-bit ASCII byte
   */
  private static boolean isAscii(byte[] data, int start, int length) {
    int acc = 0;
    int end = start + length;
    int i = start;
    int end8 = start + (length & ~7);
    for (; i < end8; i += 8) {
      acc |= (data[i] | data[i + 1] | data[i + 2] | data[i + 3] | data[i + 4] | data[i + 5] | data[i + 6] | data[i + 7]);
    }
    for (; i < end; i++) {
      acc |= data[i];
    }
    return acc >= 0;
  }

  private static String decodeModifiedUtf8(ByteBuffer encoded, int size) {
    try {
      return StringTool.decodeString(encoded, size);
    } catch (UTFDataFormatException e) {
      CorruptDataException cde = new CorruptDataException("Unexpected encoding");
      cde.addSuppressed(e);
      throw cde;
    }
  }

  public boolean limitReached() {
    return byteBuffer.position() == limit;
  }

  /**
   * @return the number of bytes between the current position and this buffer's limit
   */
  public int remaining() {
    return limit - byteBuffer.position();
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
      throw new LimitReachedException();
    }
    byteBuffer.position(targetPosition);
  }

  public ReadBuffer limit(int size) {
    if (size < 0) {
      throw new IllegalArgumentException("size cannot be < 0");
    }
    return new ReadBuffer(byteBuffer, size);
  }

  /**
   * Re-set the limit of this buffer to {@code size} bytes ahead of the current position of the
   * underlying {@link ByteBuffer}. This mutates the buffer and allows a single {@code ReadBuffer}
   * instance to be reused across successive same-sized frames on the same underlying buffer.
   *
   * @param size the number of bytes until the new limit
   * @throws LimitReachedException if the new limit exceeds the underlying buffer capacity
   */
  public void setLimit(int size) {
    if (size < 0) {
      throw new IllegalArgumentException("size cannot be < 0");
    }
    this.limit = byteBuffer.position() + size;
    if (this.limit > byteBuffer.capacity()) {
      throw new LimitReachedException();
    }
  }
}
