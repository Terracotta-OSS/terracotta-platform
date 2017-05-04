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
package org.terracotta.runnel.encoding.dataholders;

import org.terracotta.runnel.utils.WriteBuffer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author Ludovic Orban
 */
public class StringDataHolder extends AbstractDataHolder {

  private final ByteBuffer encodedString;

  public StringDataHolder(String value, int index) {
    super(index);
    this.encodedString = encodeString(value);
  }

  @Override
  protected int valueSize() {
    return encodedString.remaining();
  }

  @Override
  protected void encodeValue(WriteBuffer writeBuffer) {
    writeBuffer.putByteBuffer(encodedString);
  }


  private ByteBuffer encodeString(String object) {
    ByteArrayOutputStream bout = new ByteArrayOutputStream(2 * object.length() + 1);
    try {
      int length = object.length();
      int i = 0;

      for (; i < length; i++) {
        char c = object.charAt(i);
        if ((c == 0x0000) || (c > 0x007f)) break;
        bout.write(c);
      }

      for (; i < length; i++) {
        char c = object.charAt(i);
        if (c == 0x0000) {
          bout.write(0xc0);
          bout.write(0x80);
        } else if (c < 0x0080) {
          bout.write(c);
        } else if (c < 0x800) {
          bout.write(0xc0 | ((c >>> 6) & 0x1f));
          bout.write(0x80 | (c & 0x3f));
        } else {
          bout.write(0xe0 | ((c >>> 12) & 0x1f));
          bout.write(0x80 | ((c >>> 6) & 0x3f));
          bout.write(0x80 | (c & 0x3f));
        }
      }
    } finally {
      try {
        bout.close();
      } catch (IOException ex) {
        throw new AssertionError(ex);
      }
    }
    return ByteBuffer.wrap(bout.toByteArray());
  }

}
