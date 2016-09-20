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
import org.terracotta.runnel.decoding.fields.EnmField;
import org.terracotta.runnel.decoding.fields.FloatingPoint64Field;
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

  public StructArrayDecoder(StructField field, ReadBuffer readBuffer, StructDecoder parent) {
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
    return fieldSearcher.decodeValue(name, Int32Field.class, structReadBuffer);
  }

  @Override
  public <E extends Enum<E>> E enm(String name) {
    return (E) fieldSearcher.decodeValue(name, (Class) EnmField.class, structReadBuffer);
  }

  @Override
  public Long int64(String name) {
    return fieldSearcher.decodeValue(name, Int64Field.class, structReadBuffer);
  }

  @Override
  public Double fp64(String name) {
    return fieldSearcher.decodeValue(name, FloatingPoint64Field.class, structReadBuffer);
  }

  @Override
  public String string(String name) {
    return fieldSearcher.decodeValue(name, StringField.class, structReadBuffer);
  }

  @Override
  public ByteBuffer byteBuffer(String name) {
    return fieldSearcher.decodeValue(name, ByteBufferField.class, structReadBuffer);
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
