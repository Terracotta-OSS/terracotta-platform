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
package org.terracotta.runnel.decoding;

import org.terracotta.runnel.decoding.fields.ByteBufferField;
import org.terracotta.runnel.decoding.fields.Int32Field;
import org.terracotta.runnel.decoding.fields.Int64Field;
import org.terracotta.runnel.decoding.fields.StructField;
import org.terracotta.runnel.metadata.FieldSearcher;
import org.terracotta.runnel.decoding.fields.StringField;
import org.terracotta.runnel.utils.ReadBuffer;

import java.nio.ByteBuffer;

/**
 * @author Ludovic Orban
 */
public class StructArrayDecoder implements PrimitiveDecodingSupport {
  private final FieldSearcher fieldSearcher;
  private final StructDecoder parent;
  private final ReadBuffer arrayReadBuffer;
  private final int arrayLength;

  private ReadBuffer structReadBuffer;

  StructArrayDecoder(StructField field, ReadBuffer readBuffer, StructDecoder parent) {
    this.fieldSearcher = field.getMetadata().fieldSearcher();
    this.parent = parent;

    int arraySize = readBuffer.getVlqInt();
    this.arrayReadBuffer = readBuffer.limit(arraySize);
    this.arrayLength = readBuffer.getVlqInt();

    if (this.arrayLength > 0) {
      int structSize = readBuffer.getVlqInt();
      structReadBuffer = arrayReadBuffer.limit(structSize);
    } else {
      structReadBuffer = arrayReadBuffer.limit(0);
    }
  }

  @Override
  public Integer int32(String name) {
    Int32Field field = fieldSearcher.nextField(name, Int32Field.class, null, structReadBuffer);
    if (field == null) {
      return null;
    }
    return (Integer) field.decode(structReadBuffer);
  }

  @Override
  public Long int64(String name) {
    Int64Field field = fieldSearcher.nextField(name, Int64Field.class, null, structReadBuffer);
    if (field == null) {
      return null;
    }
    return (Long) field.decode(structReadBuffer);
  }

  @Override
  public String string(String name) {
    StringField field = fieldSearcher.nextField(name, StringField.class, null, structReadBuffer);
    if (field == null) {
      return null;
    }
    return (String) field.decode(structReadBuffer);
  }

  @Override
  public ByteBuffer byteBuffer(String name) {
    ByteBufferField field = fieldSearcher.nextField(name, ByteBufferField.class, null, structReadBuffer);
    if (field == null) {
      return null;
    }
    return (ByteBuffer) field.decode(structReadBuffer);
  }

  public int length() {
    return arrayLength;
  }

  public StructDecoder end() {
    arrayReadBuffer.skipAll();
    return parent;
  }

  public void next() {
    structReadBuffer.skipAll();

    if (arrayReadBuffer.limitReached()) {
      return;
    }

    int structSize = arrayReadBuffer.getVlqInt();
    structReadBuffer = arrayReadBuffer.limit(structSize);

    fieldSearcher.reset();
  }

}
