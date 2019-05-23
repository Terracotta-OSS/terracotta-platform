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

import java.io.UTFDataFormatException;
import java.nio.ByteBuffer;

/**
 * @author Ludovic Orban
 */
public class ReadBuffer {
  private static final boolean V1_STRING_DECODING = false;

  private final ByteBuffer byteBuffer;
  private final int limit;

  public ReadBuffer(ByteBuffer byteBuffer) {
    this(byteBuffer, byteBuffer.remaining());
  }

  private ReadBuffer(ByteBuffer byteBuffer, int limit) {
    this.byteBuffer = byteBuffer;
    this.limit = byteBuffer.position() + limit;
    if (this.limit > byteBuffer.capacity()) {
      throw new LimitReachedException();
    }
  }

  public Boolean getBoolean() {
    if (byteBuffer.position() + 1 > limit) {
      throw new LimitReachedException();
    }
    return byteBuffer.get() == (byte) 0 ? Boolean.FALSE : Boolean.TRUE;
  }

  public Double getDouble() {
    if (byteBuffer.position() + 8 > limit) {
      throw new LimitReachedException();
    }
    return byteBuffer.getDouble();
  }

  public Long getLong() {
    if (byteBuffer.position() + 8 > limit) {
      throw new LimitReachedException();
    }
    return byteBuffer.getLong();
  }

  public Character getChar() {
    if (byteBuffer.position() + 2 > limit) {
      throw new LimitReachedException();
    }
    return byteBuffer.getChar();
  }

  public Integer getInt() {
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

    ByteBuffer slice = byteBuffer.slice();
    slice.limit(size);
    String s = readString(slice);

    byteBuffer.position(byteBuffer.position() + size);
    return s;
  }

  private String readString(ByteBuffer binary) {
    if (V1_STRING_DECODING) {
      StringBuilder sb = new StringBuilder(binary.remaining());
      int i = binary.position();
      int end = binary.limit();
      for (; i < end; i++) {
        byte a = binary.get(i);
        if (((a & 0x80) != 0)) {
          break;
        }
        sb.append((char) a);
      }

      for (; i < end; i++) {
        byte a = binary.get(i);
        if ((a & 0x80) == 0) {
          sb.append((char) a);
        } else if ((a & 0xe0) == 0xc0) {
          sb.append((char) (((a & 0x1f) << 6) | ((binary.get(++i) & 0x3f))));
        } else if ((a & 0xf0) == 0xe0) {
          sb.append((char) (((a & 0x0f) << 12) | ((binary.get(++i) & 0x3f) << 6) | (binary.get(++i) & 0x3f)));
        } else {
          //these remaining stanzas are for compatibility with the previous regular UTF-8 codec
          int codepoint;
          if ((a & 0xf8) == 0xf0) {
            codepoint = ((a & 0x7) << 18) | ((binary.get(++i) & 0x3f) << 12) | ((binary.get(++i) & 0x3f) << 6) | ((binary
              .get(++i) & 0x3f));
          } else if ((a & 0xfc) == 0xf8) {
            codepoint = ((a & 0x3) << 24) | ((binary.get(++i) & 0x3f) << 18) | ((binary.get(++i) & 0x3f) << 12) | ((binary
              .get(++i) & 0x3f) << 6) | ((binary.get(++i) & 0x3f));
          } else if ((a & 0xfe) == 0xfc) {
            codepoint = ((a & 0x1) << 30) | ((binary.get(++i) & 0x3f) << 24) | ((binary.get(++i) & 0x3f) << 18) | ((binary
              .get(++i) & 0x3f) << 12) | ((binary.get(++i) & 0x3f) << 6) | ((binary.get(++i) & 0x3f));
          } else {
            throw new CorruptDataException("Unexpected encoding");
          }
          sb.appendCodePoint(codepoint);
        }
      }

      return sb.toString();
    } else {
      try {
        String ret = StringTool.attemptDecodeAsAscii(binary, 256);
        if (ret != null) {
          return ret;
        }
        return StringTool.decodeString(binary, binary.remaining());
      } catch (UTFDataFormatException e) {
        CorruptDataException cde = new CorruptDataException("Unexpected encoding");
        cde.addSuppressed(e);
        throw cde;
      }
    }
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
}
