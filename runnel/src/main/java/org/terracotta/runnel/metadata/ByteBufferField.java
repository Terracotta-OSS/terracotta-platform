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
package org.terracotta.runnel.metadata;

import java.nio.ByteBuffer;

/**
 * @author Ludovic Orban
 */
public class ByteBufferField extends AbstractField {
  public ByteBufferField(String name, int index) {
    super(name, index);
  }

  @Override
  public Object decode(ByteBuffer byteBuffer) {
    int len = byteBuffer.getInt();
    ByteBuffer slice = byteBuffer.slice();
    slice.limit(len);
    return slice;
  }

  @Override
  public int skip(ByteBuffer byteBuffer) {
    int len = byteBuffer.getInt();
    byteBuffer.position(byteBuffer.position() + len);
    return len + 4;
  }
}
