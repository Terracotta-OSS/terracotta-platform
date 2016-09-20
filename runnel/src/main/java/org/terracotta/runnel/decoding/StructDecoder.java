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
import org.terracotta.runnel.decoding.fields.StringField;
import org.terracotta.runnel.decoding.fields.StructField;
import org.terracotta.runnel.metadata.FieldSearcher;
import org.terracotta.runnel.utils.ReadBuffer;

import java.nio.ByteBuffer;

/**
 * @author Ludovic Orban
 */
public class StructDecoder implements PrimitiveDecodingSupport {

  private final FieldSearcher fieldSearcher;
  private final ReadBuffer readBuffer;
  private final StructDecoder parent;

  public StructDecoder(StructField structField, ReadBuffer readBuffer) {
    this(structField, readBuffer, null);
  }

  public StructDecoder(StructField structField, ReadBuffer readBuffer, StructDecoder parent) {
    this.fieldSearcher = structField.getMetadata().fieldSearcher();
    this.parent = parent;
    int size = readBuffer.getVlqInt();
    this.readBuffer = readBuffer.limit(size);
  }

  @Override
  public Integer int32(String name) {
    return fieldSearcher.decodeValue(name, Int32Field.class, readBuffer);
  }

  @Override
  public <E extends Enum<E>> E enm(String name) {
    return (E) fieldSearcher.decodeValue(name, (Class) EnmField.class, readBuffer);
  }

  @Override
  public Long int64(String name) {
    return fieldSearcher.decodeValue(name, Int64Field.class, readBuffer);
  }

  @Override
  public Double fp64(String name) {
    return fieldSearcher.decodeValue(name, FloatingPoint64Field.class, readBuffer);
  }

  @Override
  public String string(String name) {
    return fieldSearcher.decodeValue(name, StringField.class, readBuffer);
  }

  @Override
  public ByteBuffer byteBuffer(String name) {
    return fieldSearcher.decodeValue(name, ByteBufferField.class, readBuffer);
  }


  public ArrayDecoder<Integer> int32s(String name) {
    return fieldSearcher.decodeValueArray(name, Int32Field.class, readBuffer, this);
  }

  public ArrayDecoder<Long> int64s(String name) {
    return fieldSearcher.decodeValueArray(name, Int64Field.class, readBuffer, this);
  }

  public ArrayDecoder<Double> fp64s(String name) {
    return fieldSearcher.decodeValueArray(name, FloatingPoint64Field.class, readBuffer, this);
  }

  public ArrayDecoder<String> strings(String name) {
    return fieldSearcher.decodeValueArray(name, StringField.class, readBuffer, this);
  }

  public StructDecoder struct(String name) {
    return fieldSearcher.decodeStruct(name, readBuffer, this);
  }

  public StructArrayDecoder structs(String name) {
    return fieldSearcher.decodeStructArray(name, readBuffer, this);
  }

  public StructDecoder end() {
    if (parent == null) {
      throw new IllegalStateException("Cannot end root decoder");
    }

    readBuffer.skipAll();

    return parent;
  }

}
