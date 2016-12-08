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
 * @author Ludovic Orban
 */
public class StructArrayDecoder<P> implements PrimitiveDecodingSupport {
  private final FieldDecoder fieldDecoder;
  private final P parent;
  private final ReadBuffer arrayReadBuffer;
  private final int arrayLength;

  private ReadBuffer structReadBuffer;

  public StructArrayDecoder(StructField field, ReadBuffer readBuffer, P parent) {
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
    this.fieldDecoder = field.getMetadata().fieldDecoder(structReadBuffer);
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

  public StructDecoder<StructArrayDecoder<P>> struct(String name) {
    return fieldDecoder.decodeStruct(name, this);
  }

  public int length() {
    return arrayLength;
  }

  public P end() {
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

    fieldDecoder.reset(structReadBuffer);
  }

}
