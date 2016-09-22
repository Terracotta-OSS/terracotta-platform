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
import org.terracotta.runnel.decoding.fields.EnumField;
import org.terracotta.runnel.decoding.fields.FloatingPoint64Field;
import org.terracotta.runnel.decoding.fields.Int32Field;
import org.terracotta.runnel.decoding.fields.Int64Field;
import org.terracotta.runnel.decoding.fields.StringField;
import org.terracotta.runnel.decoding.fields.StructField;
import org.terracotta.runnel.metadata.FieldDecoder;
import org.terracotta.runnel.utils.ReadBuffer;

import java.nio.ByteBuffer;

/**
 * @author Ludovic Orban
 */
public class StructDecoder implements PrimitiveDecodingSupport {

  private final FieldDecoder fieldDecoder;
  private final ReadBuffer readBuffer;
  private final StructDecoder parent;

  public StructDecoder(StructField structField, ReadBuffer readBuffer) {
    this(structField, readBuffer, null);
  }

  public StructDecoder(StructField structField, ReadBuffer readBuffer, StructDecoder parent) {
    this.parent = parent;
    int size = readBuffer.getVlqInt();
    this.readBuffer = readBuffer.limit(size);
    this.fieldDecoder = structField.getMetadata().fieldDecoder(this.readBuffer);
  }

  @Override
  public Integer int32(String name) {
    return fieldDecoder.decodeValue(name, Int32Field.class);
  }

  @Override
  public <E extends Enum<E>> E enm(String name) {
    return (E) fieldDecoder.decodeValue(name, (Class) EnumField.class);
  }

  @Override
  public Long int64(String name) {
    return fieldDecoder.decodeValue(name, Int64Field.class);
  }

  @Override
  public Double fp64(String name) {
    return fieldDecoder.decodeValue(name, FloatingPoint64Field.class);
  }

  @Override
  public String string(String name) {
    return fieldDecoder.decodeValue(name, StringField.class);
  }

  @Override
  public ByteBuffer byteBuffer(String name) {
    return fieldDecoder.decodeValue(name, ByteBufferField.class);
  }


  public ArrayDecoder<Integer> int32s(String name) {
    return fieldDecoder.decodeValueArray(name, Int32Field.class, this);
  }

  public ArrayDecoder<Long> int64s(String name) {
    return fieldDecoder.decodeValueArray(name, Int64Field.class, this);
  }

  public ArrayDecoder<Double> fp64s(String name) {
    return fieldDecoder.decodeValueArray(name, FloatingPoint64Field.class, this);
  }

  public ArrayDecoder<String> strings(String name) {
    return fieldDecoder.decodeValueArray(name, StringField.class, this);
  }

  public StructDecoder struct(String name) {
    return fieldDecoder.decodeStruct(name, this);
  }

  public StructArrayDecoder structs(String name) {
    return fieldDecoder.decodeStructArray(name, this);
  }

  public StructDecoder end() {
    if (parent == null) {
      throw new IllegalStateException("Cannot end root decoder");
    }

    readBuffer.skipAll();

    return parent;
  }

}
