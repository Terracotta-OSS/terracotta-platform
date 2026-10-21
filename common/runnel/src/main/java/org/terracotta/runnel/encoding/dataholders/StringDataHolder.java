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
package org.terracotta.runnel.encoding.dataholders;

import org.terracotta.runnel.utils.StringTool;
import org.terracotta.runnel.utils.WriteBuffer;

import java.nio.charset.StandardCharsets;

/**
 * @author Ludovic Orban
 */
public final class StringDataHolder extends AbstractDataHolder {

  private final byte[] encodedString;

  public StringDataHolder(String value, int index) {
    super(index);
    this.encodedString = encodeString(value);
  }

  @Override
  protected int valueSize() {
    return encodedString.length;
  }

  @Override
  protected void encodeValue(WriteBuffer writeBuffer) {
    writeBuffer.putBytes(encodedString, 0, encodedString.length);
  }

  /**
   * Encodes a string to its modified UTF-8 representation (NUL as {@code C0 80}, 2- and 3-byte
   * forms, supplementary characters as surrogate pairs), in an exactly-sized byte array.
   */
  static byte[] encodeString(String object) {
    int strLength = object.length();
    long encodedLen = StringTool.getEncodedLen(object);
    if (encodedLen != (int) encodedLen) {
      throw new IllegalStateException("Encoded length greater than Integer.MAX_VALUE: " + encodedLen);
    }

    if (encodedLen == strLength) {
      /*
       * Every character weighed exactly one byte, which can only happen when all characters
       * are non-NUL 7-bit ASCII (NUL weighs two bytes). Those encode to themselves under
       * ISO-8859-1, which on compact-string JVMs is a plain copy of the string's internal bytes.
       */
      return object.getBytes(StandardCharsets.ISO_8859_1);
    }

    byte[] out = new byte[(int) encodedLen];
    int pos = 0;
    for (int i = 0; i < strLength; i++) {
      char c = object.charAt(i);
      if (c <= 0x7F && c != 0) {
        out[pos++] = (byte) c;
      } else if (c <= 0x7FF) {
        out[pos++] = (byte) (0xC0 | c >>> 6);
        out[pos++] = (byte) (0x80 | (c & 0x3F));
      } else {
        out[pos++] = (byte) (0xE0 | c >>> 12);
        out[pos++] = (byte) (0x80 | ((c >>> 6) & 0x3F));
        out[pos++] = (byte) (0x80 | (c & 0x3F));
      }
    }
    return out;
  }

}
