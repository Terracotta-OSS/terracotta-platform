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

import org.terracotta.runnel.decoding.fields.BoolField;
import org.terracotta.runnel.decoding.fields.ByteBufferField;
import org.terracotta.runnel.decoding.fields.CharField;
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
 * A decoder allows decoding structured data described by a {@link org.terracotta.runnel.Struct}.
 * Note: Instances of this class are not thread-safe.
 */
public class StructDecoder<P> implements PrimitiveDecodingSupport {

  private final FieldDecoder fieldDecoder;
  private final ReadBuffer readBuffer;
  private final P parent;

  public StructDecoder(StructField structField, ReadBuffer readBuffer) {
    this(structField, readBuffer, null);
  }

  public StructDecoder(StructField structField, ReadBuffer readBuffer, P parent) {
    this.parent = parent;
    int size = readBuffer.getVlqInt();
    this.readBuffer = readBuffer.limit(size);
    this.fieldDecoder = structField.getMetadata().fieldDecoder(this.readBuffer);
  }

  @Override
  public Boolean bool(String name) {
    return fieldDecoder.decodeValue(name, BoolField.class);
  }

  @Override
  public Character chr(String name) {
    return fieldDecoder.decodeValue(name, CharField.class);
  }

  @Override
  public Integer int32(String name) {
    return fieldDecoder.decodeValue(name, Int32Field.class);
  }

  @Override
  public <E> Enm<E> enm(String name) {
    Enm<E> enm = (Enm<E>) fieldDecoder.decodeValue(name, (Class) EnumField.class);
    if (enm == null) {
      return new Enm<E>(name);
    }
    return enm;
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


  public ArrayDecoder<Integer, StructDecoder<P>> int32s(String name) {
    return fieldDecoder.decodeValueArray(name, Int32Field.class, this);
  }

  public ArrayDecoder<Boolean, StructDecoder<P>> bools(String name) {
    return fieldDecoder.decodeValueArray(name, BoolField.class, this);
  }

  public ArrayDecoder<Character, StructDecoder<P>> chrs(String name) {
    return fieldDecoder.decodeValueArray(name, CharField.class, this);
  }

  public ArrayDecoder<Long, StructDecoder<P>> int64s(String name) {
    return fieldDecoder.decodeValueArray(name, Int64Field.class, this);
  }

  public ArrayDecoder<Double, StructDecoder<P>> fp64s(String name) {
    return fieldDecoder.decodeValueArray(name, FloatingPoint64Field.class, this);
  }

  public ArrayDecoder<String, StructDecoder<P>> strings(String name) {
    return fieldDecoder.decodeValueArray(name, StringField.class, this);
  }

  public StructDecoder<StructDecoder<P>> struct(String name) {
    return fieldDecoder.decodeStruct(name, this);
  }

  public StructArrayDecoder<StructDecoder<P>> structs(String name) {
    return fieldDecoder.decodeStructArray(name, this);
  }

  public P end() {
    if (parent == null) {
      throw new IllegalStateException("Cannot end root decoder");
    }

    readBuffer.skipAll();

    return parent;
  }

}
