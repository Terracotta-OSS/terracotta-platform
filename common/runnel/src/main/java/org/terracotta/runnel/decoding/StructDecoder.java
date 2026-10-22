/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2026
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

import org.terracotta.runnel.decoding.fields.ArrayField;
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
public class StructDecoder<P extends Decoder> implements PrimitiveDecodingSupport, Decoder {

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

  /**
   * Advance this decoder to the next element of the enclosing array. Internal use by
   * {@link StructArrayDecoder#forEachRemaining}: skips any unread remainder of the current
   * element, then re-initializes the shared {@link ReadBuffer} for the next one. Returns
   * {@code true} if there is a next element, {@code false} when the array is exhausted.
   * Callers must not retain this instance across iterations.
   */
  boolean next() {
    ReadBuffer parentBuffer = end().buffer();
    if (parentBuffer.limitReached()) {
      return false;
    } else {
      readBuffer.setLimit(parent.buffer().getVlqInt());
      fieldDecoder.reset(readBuffer);
      return true;
    }
  }

  @Override
  public ReadBuffer buffer() {
    return readBuffer;
  }

  @Override
  public Boolean bool(String name) {
    return fieldDecoder.decodeValue(name, BoolField.class);
  }

  public Boolean bool(BoolField field) {
    return fieldDecoder.decodeValue(field, BoolField.class);
  }

  /**
   * Decode a boolean, without boxing.
   * @param name the field name
   * @param defaultValue the value to return if the field is absent from the data
   * @return the decoded value, or {@code defaultValue}
   */
  public boolean bool(String name, boolean defaultValue) {
    return fieldDecoder.decodeBoolean(name, defaultValue);
  }

  public boolean bool(BoolField field, boolean defaultValue) {
    return fieldDecoder.decodeBoolean(field, defaultValue);
  }

  @Override
  public Character chr(String name) {
    return fieldDecoder.decodeValue(name, CharField.class);
  }

  public Character chr(CharField field) {
    return fieldDecoder.decodeValue(field, CharField.class);
  }

  /**
   * Decode a character, without boxing.
   * @param name the field name
   * @param defaultValue the value to return if the field is absent from the data
   * @return the decoded value, or {@code defaultValue}
   */
  public char chr(String name, char defaultValue) {
    return fieldDecoder.decodeChar(name, defaultValue);
  }

  public char chr(CharField field, char defaultValue) {
    return fieldDecoder.decodeChar(field, defaultValue);
  }

  @Override
  public Integer int32(String name) {
    return fieldDecoder.decodeValue(name, Int32Field.class);
  }

  public Integer int32(Int32Field field) {
    return fieldDecoder.decodeValue(field, Int32Field.class);
  }

  /**
   * Decode a 32-bit integer, without boxing.
   * @param name the field name
   * @param defaultValue the value to return if the field is absent from the data
   * @return the decoded value, or {@code defaultValue}
   */
  public int int32(String name, int defaultValue) {
    return fieldDecoder.decodeInt32(name, defaultValue);
  }

  public int int32(Int32Field field, int defaultValue) {
    return fieldDecoder.decodeInt32(field, defaultValue);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <E> Enm<E> enm(String name) {
    Enm<E> enm = (Enm<E>) fieldDecoder.decodeValue(name, (Class) EnumField.class);
    if (enm == null) {
      return new Enm<>(name);
    }
    return enm;
  }

  public <E> Enm<E> enm(EnumField<E> field) {
    Enm<E> enm = fieldDecoder.decodeValue(field, enumFieldClass());
    if (enm == null) {
      return new Enm<>(field.name());
    }
    return enm;
  }

  /**
   * Decode an enum without allocating an {@link Enm} holder.
   * @param name the field name
   * @param defaultValue the value to return if the field is absent from the data, or if the
   * encoded value is unknown to the enum mapping (forward compatibility)
   * @param <E> the enumeration's actual type
   * @return the decoded enum, or {@code defaultValue}
   */
  public <E> E enm(String name, E defaultValue) {
    return fieldDecoder.decodeEnum(name, defaultValue);
  }

  public <E> E enm(EnumField<E> field, E defaultValue) {
    return fieldDecoder.decodeEnum(field, defaultValue);
  }

  @Override
  public Long int64(String name) {
    return fieldDecoder.decodeValue(name, Int64Field.class);
  }

  public Long int64(Int64Field field) {
    return fieldDecoder.decodeValue(field, Int64Field.class);
  }

  /**
   * Decode a 64-bit integer, without boxing.
   * @param name the field name
   * @param defaultValue the value to return if the field is absent from the data
   * @return the decoded value, or {@code defaultValue}
   */
  public long int64(String name, long defaultValue) {
    return fieldDecoder.decodeInt64(name, defaultValue);
  }

  public long int64(Int64Field field, long defaultValue) {
    return fieldDecoder.decodeInt64(field, defaultValue);
  }

  @Override
  public Double fp64(String name) {
    return fieldDecoder.decodeValue(name, FloatingPoint64Field.class);
  }

  public Double fp64(FloatingPoint64Field field) {
    return fieldDecoder.decodeValue(field, FloatingPoint64Field.class);
  }

  /**
   * Decode a 64-bit floating point number, without boxing.
   * @param name the field name
   * @param defaultValue the value to return if the field is absent from the data
   * @return the decoded value, or {@code defaultValue}
   */
  public double fp64(String name, double defaultValue) {
    return fieldDecoder.decodeFp64(name, defaultValue);
  }

  public double fp64(FloatingPoint64Field field, double defaultValue) {
    return fieldDecoder.decodeFp64(field, defaultValue);
  }

  @Override
  public String string(String name) {
    return fieldDecoder.decodeValue(name, StringField.class);
  }

  public String string(StringField field) {
    return fieldDecoder.decodeValue(field, StringField.class);
  }

  @Override
  public ByteBuffer byteBuffer(String name) {
    return fieldDecoder.decodeValue(name, ByteBufferField.class);
  }

  public ByteBuffer byteBuffer(ByteBufferField field) {
    return fieldDecoder.decodeValue(field, ByteBufferField.class);
  }


  public ArrayDecoder<Integer, StructDecoder<P>> int32s(String name) {
    return fieldDecoder.decodeValueArray(name, Int32Field.class, this);
  }

  public ArrayDecoder<Integer, StructDecoder<P>> int32s(ArrayField field) {
    return fieldDecoder.decodeValueArray(field, Int32Field.class, this);
  }

  /**
   * Decode an int32 array directly into a primitive array, without boxed elements.
   * @param name the field name
   * @return the decoded values, or {@code null} if the field is absent from the data
   */
  public int[] int32sArray(String name) {
    return fieldDecoder.decodeInt32Array(name);
  }

  public int[] int32sArray(ArrayField field) {
    return fieldDecoder.decodeInt32Array(field);
  }

  public ArrayDecoder<Boolean, StructDecoder<P>> bools(String name) {
    return fieldDecoder.decodeValueArray(name, BoolField.class, this);
  }

  public ArrayDecoder<Boolean, StructDecoder<P>> bools(ArrayField field) {
    return fieldDecoder.decodeValueArray(field, BoolField.class, this);
  }

  /**
   * Decode a boolean array directly into a primitive array, without boxed elements.
   * @param name the field name
   * @return the decoded values, or {@code null} if the field is absent from the data
   */
  public boolean[] boolsArray(String name) {
    return fieldDecoder.decodeBoolArray(name);
  }

  public boolean[] boolsArray(ArrayField field) {
    return fieldDecoder.decodeBoolArray(field);
  }

  public ArrayDecoder<Character, StructDecoder<P>> chrs(String name) {
    return fieldDecoder.decodeValueArray(name, CharField.class, this);
  }

  public ArrayDecoder<Character, StructDecoder<P>> chrs(ArrayField field) {
    return fieldDecoder.decodeValueArray(field, CharField.class, this);
  }

  /**
   * Decode a char array directly into a primitive array, without boxed elements.
   * @param name the field name
   * @return the decoded values, or {@code null} if the field is absent from the data
   */
  public char[] chrsArray(String name) {
    return fieldDecoder.decodeCharArray(name);
  }

  public char[] chrsArray(ArrayField field) {
    return fieldDecoder.decodeCharArray(field);
  }

  public ArrayDecoder<Long, StructDecoder<P>> int64s(String name) {
    return fieldDecoder.decodeValueArray(name, Int64Field.class, this);
  }

  public ArrayDecoder<Long, StructDecoder<P>> int64s(ArrayField field) {
    return fieldDecoder.decodeValueArray(field, Int64Field.class, this);
  }

  /**
   * Decode an int64 array directly into a primitive array, without boxed elements.
   * @param name the field name
   * @return the decoded values, or {@code null} if the field is absent from the data
   */
  public long[] int64sArray(String name) {
    return fieldDecoder.decodeInt64Array(name);
  }

  public long[] int64sArray(ArrayField field) {
    return fieldDecoder.decodeInt64Array(field);
  }

  public ArrayDecoder<Double, StructDecoder<P>> fp64s(String name) {
    return fieldDecoder.decodeValueArray(name, FloatingPoint64Field.class, this);
  }

  public ArrayDecoder<Double, StructDecoder<P>> fp64s(ArrayField field) {
    return fieldDecoder.decodeValueArray(field, FloatingPoint64Field.class, this);
  }

  /**
   * Decode an fp64 array directly into a primitive array, without boxed elements.
   * @param name the field name
   * @return the decoded values, or {@code null} if the field is absent from the data
   */
  public double[] fp64sArray(String name) {
    return fieldDecoder.decodeFp64Array(name);
  }

  public double[] fp64sArray(ArrayField field) {
    return fieldDecoder.decodeFp64Array(field);
  }

  public ArrayDecoder<String, StructDecoder<P>> strings(String name) {
    return fieldDecoder.decodeValueArray(name, StringField.class, this);
  }

  public ArrayDecoder<String, StructDecoder<P>> strings(ArrayField field) {
    return fieldDecoder.decodeValueArray(field, StringField.class, this);
  }

  /**
   * Decode a string array directly into an array.
   * @param name the field name
   * @return the decoded values, or {@code null} if the field is absent from the data
   */
  public String[] stringsArray(String name) {
    return fieldDecoder.decodeStringArray(name);
  }

  public String[] stringsArray(ArrayField field) {
    return fieldDecoder.decodeStringArray(field);
  }

  public ArrayDecoder<ByteBuffer, StructDecoder<P>> byteBuffers(String name) {
    return fieldDecoder.decodeValueArray(name, ByteBufferField.class, this);
  }

  public ArrayDecoder<ByteBuffer, StructDecoder<P>> byteBuffers(ArrayField field) {
    return fieldDecoder.decodeValueArray(field, ByteBufferField.class, this);
  }

  /**
   * Decode a byte buffer array directly into an array.
   * @param name the field name
   * @return the decoded values, or {@code null} if the field is absent from the data
   */
  public ByteBuffer[] byteBuffersArray(String name) {
    return fieldDecoder.decodeByteBufferArray(name);
  }

  public ByteBuffer[] byteBuffersArray(ArrayField field) {
    return fieldDecoder.decodeByteBufferArray(field);
  }

  public StructDecoder<StructDecoder<P>> struct(String name) {
    return fieldDecoder.decodeStruct(name, this);
  }

  public StructDecoder<StructDecoder<P>> struct(StructField field) {
    return fieldDecoder.decodeStruct(field, this);
  }

  public StructArrayDecoder<StructDecoder<P>> structs(String name) {
    return fieldDecoder.decodeStructArray(name, this);
  }

  public StructArrayDecoder<StructDecoder<P>> structs(ArrayField field) {
    return fieldDecoder.decodeStructArray(field, this);
  }

  public P end() {
    if (parent == null) {
      throw new IllegalStateException("Cannot end root decoder");
    }

    readBuffer.skipAll();

    return parent;
  }

  @SuppressWarnings("unchecked")
  private static <E> Class<EnumField<E>> enumFieldClass() {
    return (Class<EnumField<E>>) (Class<?>) EnumField.class;
  }

}
