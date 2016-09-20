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

/**
 * @author Ludovic Orban
 */
public class ByteBufferDataHolder extends AbstractDataHolder {

  private final ByteBuffer value;

  public ByteBufferDataHolder(ByteBuffer value, int index) {
    super(index);
    this.value = value;
  }

  @Override
  protected int valueSize() {
    return value.remaining();
  }

  @Override
  protected void encodeValue(WriteBuffer writeBuffer) {
    writeBuffer.putByteBuffer(value);
  }
}
