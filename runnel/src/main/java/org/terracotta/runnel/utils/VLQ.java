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
public class VLQ {

  public static void encode(int value, ByteBuffer out) {
    if (value < 0) {
      throw new IllegalArgumentException("Cannot encode negative values");
    }
    while ((value & 0xFFFFFF80) != 0L) {
      out.put((byte) ((value & 0x7F) | 0x80));
      value >>>= 7;
    }
    out.put((byte) (value & 0x7F));
  }

  public static int decode(ByteBuffer in) {
    int value = 0;
    int i = 0;
    int b;
    while (((b = in.get()) & 0x80) != 0) {
      value |= (b & 0x7F) << i;
      i += 7;
      if (i > 35) {
        throw new IllegalArgumentException("Encoded value is greater than Integer");
      }
    }
    return value | (b << i);
  }

  public static int encodedSize(int value) {
    if (value < 0) {
      throw new IllegalArgumentException("Cannot encode negative values");
    }
    int i = 1;
    while ((value & 0xFFFFFF80) != 0L) {
      value >>>= 7;
      i++;
    }
    return i;
  }

}
