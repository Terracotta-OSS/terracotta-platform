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

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * @author Ludovic Orban
 */
public class StringDataHolder extends AbstractDataHolder {

  private static final Charset UTF8 = Charset.forName("UTF-8");

  private final ByteBuffer encodedString;

  public StringDataHolder(String value, int index) {
    super(index);
    this.encodedString = UTF8.encode(value);
  }

  @Override
  protected int valueSize() {
    return encodedString.remaining();
  }

  @Override
  protected void encodeValue(WriteBuffer writeBuffer) {
    writeBuffer.putByteBuffer(encodedString);
  }
}
