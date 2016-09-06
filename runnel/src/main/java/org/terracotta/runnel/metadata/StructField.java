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
import java.util.List;

/**
 * @author Ludovic Orban
 */
public class StructField extends AbstractField {

  private final List<? extends Field> subFields;

  public StructField(String name, int index, List<? extends Field> subFields) {
    super(name, index);
    this.subFields = subFields;
  }

  @Override
  public List<? extends Field> subFields() {
    return subFields;
  }

  @Override
  public Object decode(ByteBuffer byteBuffer) {
    throw new UnsupportedOperationException("todo");
  }

  @Override
  public int skip(ByteBuffer byteBuffer) {
    int size = byteBuffer.getInt() - 4;
    byteBuffer.position(byteBuffer.position() + size);
    return size;
  }
}
