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
package org.terracotta.runnel.metadata;

import org.terracotta.runnel.decoding.ArrayDecoder;
import org.terracotta.runnel.decoding.Decoder;
import org.terracotta.runnel.decoding.StructArrayDecoder;
import org.terracotta.runnel.decoding.StructDecoder;
import org.terracotta.runnel.decoding.fields.ArrayField;
import org.terracotta.runnel.decoding.fields.BoolField;
import org.terracotta.runnel.decoding.fields.ByteBufferField;
import org.terracotta.runnel.decoding.fields.CharField;
import org.terracotta.runnel.decoding.fields.EnumField;
import org.terracotta.runnel.decoding.fields.Field;
import org.terracotta.runnel.decoding.fields.FloatingPoint64Field;
import org.terracotta.runnel.decoding.fields.Int32Field;
import org.terracotta.runnel.decoding.fields.Int64Field;
import org.terracotta.runnel.decoding.fields.StringField;
import org.terracotta.runnel.decoding.fields.StructField;
import org.terracotta.runnel.decoding.fields.ValueField;
import org.terracotta.runnel.utils.CorruptDataException;
import org.terracotta.runnel.utils.ReadBuffer;

import java.nio.ByteBuffer;

/**
 * Decodes fields by walking the encoded stream in field-index order.
 * Note: instances of this class are not thread-safe.
 * @author Ludovic Orban
 */
public class FieldDecoder {

  private final Metadata metadata;
  private ReadBuffer readBuffer;
  private int lastIndex = -1;
  private int readAheadIndex = -1;

  FieldDecoder(Metadata metadata, ReadBuffer readBuffer) {
    this.metadata = metadata;
    this.readBuffer = readBuffer;
  }

  /**
   * Reset this decoder to start decoding a new frame from the given buffer. Used to reuse a
   * single decoder instance (e.g. across the elements of a struct array).
   * @param readBuffer the buffer to decode from
   */
  public void reset(ReadBuffer readBuffer) {
    this.readBuffer = readBuffer;
    this.lastIndex = -1;
    this.readAheadIndex = -1;
  }

  public <P extends Decoder> StructArrayDecoder<P> decodeStructArray(String name, P parent) {
    ArrayField field = nextField(name, ArrayField.class, StructField.class);
    if (field == null) {
      return null;
    }
    return new StructArrayDecoder<>(((StructField) field.subField()), readBuffer, parent);
  }

  public <P extends Decoder> StructArrayDecoder<P> decodeStructArray(ArrayField field, P parent) {
    if (nextField(field, ArrayField.class, StructField.class) == null) {
      return null;
    }
    return new StructArrayDecoder<>(((StructField) field.subField()), readBuffer, parent);
  }

  public <P extends Decoder> StructDecoder<P> decodeStruct(String name, P parent) {
    StructField field = nextField(name, StructField.class, null);
    if (field == null) {
      return null;
    }
    return new StructDecoder<>(field, readBuffer, parent);
  }

  public <P extends Decoder> StructDecoder<P> decodeStruct(StructField field, P parent) {
    if (nextField(field, StructField.class, null) == null) {
      return null;
    }
    return new StructDecoder<>(field, readBuffer, parent);
  }

  @SuppressWarnings("unchecked")
  public <T, P> ArrayDecoder<T, P> decodeValueArray(String name, Class<? extends ValueField<T>> clazz, P parent) {
    ArrayField field = nextField(name, ArrayField.class, clazz);
    if (field == null) {
      return null;
    }
    return new ArrayDecoder<>((ValueField<T>) field.subField(), readBuffer, parent);
  }

  @SuppressWarnings("unchecked")
  public <T, P> ArrayDecoder<T, P> decodeValueArray(ArrayField field, Class<? extends ValueField<T>> clazz, P parent) {
    if (nextField(field, ArrayField.class, clazz) == null) {
      return null;
    }
    return new ArrayDecoder<>((ValueField<T>) field.subField(), readBuffer, parent);
  }

  public <T> T decodeValue(String name, Class<? extends ValueField<T>> clazz) {
    ValueField<T> field = nextField(name, clazz, null);
    if (field == null) {
      return null;
    }
    return field.decode(readBuffer);
  }

  /**
   * Decodes the given pre-resolved field handle. The expected class is validated against the
   * field's runtime type (catching unchecked-cast misuse with the same
   * {@code IllegalArgumentException} the name-based API gives, instead of failing later with a
   * {@code ClassCastException} or decoding as the wrong type).
   */
  public <T, F extends ValueField<T>> T decodeValue(F field, Class<F> fieldClazz) {
    if (nextField(field, fieldClazz, null) == null) {
      return null;
    }
    return field.decode(readBuffer);
  }

  // ------------------------------------------------------------------ primitive decodes

  public boolean decodeBoolean(String name, boolean defaultValue) {
    BoolField field = nextField(name, BoolField.class, null);
    return field == null ? defaultValue : field.decodeBoolean(readBuffer);
  }

  public boolean decodeBoolean(BoolField field, boolean defaultValue) {
    return nextField(field, BoolField.class, null) == null ? defaultValue : field.decodeBoolean(readBuffer);
  }

  public char decodeChar(String name, char defaultValue) {
    CharField field = nextField(name, CharField.class, null);
    return field == null ? defaultValue : field.decodeChar(readBuffer);
  }

  public char decodeChar(CharField field, char defaultValue) {
    return nextField(field, CharField.class, null) == null ? defaultValue : field.decodeChar(readBuffer);
  }

  public int decodeInt32(String name, int defaultValue) {
    Int32Field field = nextField(name, Int32Field.class, null);
    return field == null ? defaultValue : field.decodeInt(readBuffer);
  }

  public int decodeInt32(Int32Field field, int defaultValue) {
    return nextField(field, Int32Field.class, null) == null ? defaultValue : field.decodeInt(readBuffer);
  }

  public long decodeInt64(String name, long defaultValue) {
    Int64Field field = nextField(name, Int64Field.class, null);
    return field == null ? defaultValue : field.decodeLong(readBuffer);
  }

  public long decodeInt64(Int64Field field, long defaultValue) {
    return nextField(field, Int64Field.class, null) == null ? defaultValue : field.decodeLong(readBuffer);
  }

  public double decodeFp64(String name, double defaultValue) {
    FloatingPoint64Field field = nextField(name, FloatingPoint64Field.class, null);
    return field == null ? defaultValue : field.decodeDouble(readBuffer);
  }

  public double decodeFp64(FloatingPoint64Field field, double defaultValue) {
    return nextField(field, FloatingPoint64Field.class, null) == null ? defaultValue : field.decodeDouble(readBuffer);
  }

  @SuppressWarnings("unchecked")
  public <E> E decodeEnum(String name, E defaultValue) {
    EnumField<E> field = nextField(name, (Class<EnumField<E>>) (Class<?>) EnumField.class, null);
    if (field == null) {
      return defaultValue;
    }
    E decoded = field.decodeEnum(readBuffer);
    return decoded == null ? defaultValue : decoded;
  }

  public <E> E decodeEnum(EnumField<E> field, E defaultValue) {
    if (nextField(field, enumFieldClass(), null) == null) {
      return defaultValue;
    }
    E decoded = field.decodeEnum(readBuffer);
    return decoded == null ? defaultValue : decoded;
  }

  // ------------------------------------------------------------------ bulk array decodes

  public int[] decodeInt32Array(String name) {
    ArrayField field = nextField(name, ArrayField.class, Int32Field.class);
    return field == null ? null : decodeInt32Array0(field);
  }

  public int[] decodeInt32Array(ArrayField field) {
    if (nextField(field, ArrayField.class, Int32Field.class) == null) {
      return null;
    }
    return decodeInt32Array0(field);
  }

  private int[] decodeInt32Array0(ArrayField field) {
    int size = readBuffer.getVlqInt();
    ReadBuffer sub = readBuffer.limit(size);
    int[] values = new int[arrayLength(sub)];
    Int32Field subField = (Int32Field) field.subField();
    for (int i = 0; i < values.length; i++) {
      values[i] = subField.decodeInt(sub);
    }
    sub.skipAll();
    return values;
  }

  public long[] decodeInt64Array(String name) {
    ArrayField field = nextField(name, ArrayField.class, Int64Field.class);
    return field == null ? null : decodeInt64Array0(field);
  }

  public long[] decodeInt64Array(ArrayField field) {
    if (nextField(field, ArrayField.class, Int64Field.class) == null) {
      return null;
    }
    return decodeInt64Array0(field);
  }

  private long[] decodeInt64Array0(ArrayField field) {
    int size = readBuffer.getVlqInt();
    ReadBuffer sub = readBuffer.limit(size);
    long[] values = new long[arrayLength(sub)];
    Int64Field subField = (Int64Field) field.subField();
    for (int i = 0; i < values.length; i++) {
      values[i] = subField.decodeLong(sub);
    }
    sub.skipAll();
    return values;
  }

  public double[] decodeFp64Array(String name) {
    ArrayField field = nextField(name, ArrayField.class, FloatingPoint64Field.class);
    return field == null ? null : decodeFp64Array0(field);
  }

  public double[] decodeFp64Array(ArrayField field) {
    if (nextField(field, ArrayField.class, FloatingPoint64Field.class) == null) {
      return null;
    }
    return decodeFp64Array0(field);
  }

  private double[] decodeFp64Array0(ArrayField field) {
    int size = readBuffer.getVlqInt();
    ReadBuffer sub = readBuffer.limit(size);
    double[] values = new double[arrayLength(sub)];
    FloatingPoint64Field subField = (FloatingPoint64Field) field.subField();
    for (int i = 0; i < values.length; i++) {
      values[i] = subField.decodeDouble(sub);
    }
    sub.skipAll();
    return values;
  }

  public boolean[] decodeBoolArray(String name) {
    ArrayField field = nextField(name, ArrayField.class, BoolField.class);
    return field == null ? null : decodeBoolArray0(field);
  }

  public boolean[] decodeBoolArray(ArrayField field) {
    if (nextField(field, ArrayField.class, BoolField.class) == null) {
      return null;
    }
    return decodeBoolArray0(field);
  }

  private boolean[] decodeBoolArray0(ArrayField field) {
    int size = readBuffer.getVlqInt();
    ReadBuffer sub = readBuffer.limit(size);
    boolean[] values = new boolean[arrayLength(sub)];
    BoolField subField = (BoolField) field.subField();
    for (int i = 0; i < values.length; i++) {
      values[i] = subField.decodeBoolean(sub);
    }
    sub.skipAll();
    return values;
  }

  public char[] decodeCharArray(String name) {
    ArrayField field = nextField(name, ArrayField.class, CharField.class);
    return field == null ? null : decodeCharArray0(field);
  }

  public char[] decodeCharArray(ArrayField field) {
    if (nextField(field, ArrayField.class, CharField.class) == null) {
      return null;
    }
    return decodeCharArray0(field);
  }

  private char[] decodeCharArray0(ArrayField field) {
    int size = readBuffer.getVlqInt();
    ReadBuffer sub = readBuffer.limit(size);
    char[] values = new char[arrayLength(sub)];
    CharField subField = (CharField) field.subField();
    for (int i = 0; i < values.length; i++) {
      values[i] = subField.decodeChar(sub);
    }
    sub.skipAll();
    return values;
  }

  public String[] decodeStringArray(String name) {
    ArrayField field = nextField(name, ArrayField.class, StringField.class);
    return field == null ? null : decodeStringArray0(field);
  }

  public String[] decodeStringArray(ArrayField field) {
    if (nextField(field, ArrayField.class, StringField.class) == null) {
      return null;
    }
    return decodeStringArray0(field);
  }

  private String[] decodeStringArray0(ArrayField field) {
    int size = readBuffer.getVlqInt();
    ReadBuffer sub = readBuffer.limit(size);
    String[] values = new String[arrayLength(sub)];
    StringField subField = (StringField) field.subField();
    for (int i = 0; i < values.length; i++) {
      values[i] = subField.decode(sub);
    }
    sub.skipAll();
    return values;
  }

  public ByteBuffer[] decodeByteBufferArray(String name) {
    ArrayField field = nextField(name, ArrayField.class, ByteBufferField.class);
    return field == null ? null : decodeByteBufferArray0(field);
  }

  public ByteBuffer[] decodeByteBufferArray(ArrayField field) {
    if (nextField(field, ArrayField.class, ByteBufferField.class) == null) {
      return null;
    }
    return decodeByteBufferArray0(field);
  }

  private ByteBuffer[] decodeByteBufferArray0(ArrayField field) {
    int size = readBuffer.getVlqInt();
    ReadBuffer sub = readBuffer.limit(size);
    ByteBuffer[] values = new ByteBuffer[arrayLength(sub)];
    ByteBufferField subField = (ByteBufferField) field.subField();
    for (int i = 0; i < values.length; i++) {
      values[i] = subField.decode(sub);
    }
    sub.skipAll();
    return values;
  }

  /**
   * Reads the array element count, defending against corrupt lengths that would otherwise
   * trigger huge pre-allocations: every element occupies at least one byte (its size prefix,
   * which is all an empty-valued element is), so a length greater than the remaining byte
   * count can never be valid.
   */
  private int arrayLength(ReadBuffer sub) {
    int length = sub.getVlqInt();
    if (length < 0 || length > sub.remaining()) {
      throw new CorruptDataException("Invalid array length : " + length);
    }
    return length;
  }

  // ------------------------------------------------------------------ field resolution

  @SuppressWarnings("unchecked")
  private <T extends Field, S extends Field> T nextField(String name, Class<T> fieldClazz, Class<S> subFieldClazz) {
    Field field = findFieldWithIndex(name, fieldClazz, subFieldClazz);
    return (T) resolveField(field);
  }

  @SuppressWarnings("unchecked")
  private <T extends Field, S extends Field> T nextField(T field, Class<T> fieldClazz, Class<S> subFieldClazz) {
    validateHandle(field, fieldClazz, subFieldClazz);
    return (T) resolveField(field);
  }

  /**
   * Advances the stream up to the given field, skipping over any lower-indexed fields.
   * @return the field if it is next in the stream, or null if it is absent (the stream position
   * is left at the following field, remembered for the next call).
   */
  private Field resolveField(Field field) {
    if (readBuffer.limitReached()) {
      return null;
    }

    // readAheadIndex is -1 (nothing read ahead) or the index of the next field in the stream,
    // whose index VLQ has already been consumed: use it instead of reading a new one.
    int index = readAheadIndex != -1 ? readAheadIndex : readBuffer.getVlqInt();
    readAheadIndex = -1;
    // skip all fields with a lower index than the requested field's
    while (index < field.index()) {
      int fieldSize = readBuffer.getVlqInt();
      readBuffer.skip(fieldSize);
      if (readBuffer.limitReached()) {
        return null;
      }
      index = readBuffer.getVlqInt();
    }

    if (index > field.index()) {
      readAheadIndex = index;
      return null;
    } else if (index != field.index()) {
      return null;
    } else {
      return field;
    }
  }

  @SuppressWarnings("unchecked")
  private static <E> Class<EnumField<E>> enumFieldClass() {
    return (Class<EnumField<E>>) (Class<?>) EnumField.class;
  }

  private <T extends Field, S extends Field> void validateHandle(T field, Class<T> fieldClazz, Class<S> subFieldClazz) {
    if (!metadata.hasField(field)) {
      throw new IllegalArgumentException("Field '" + field.name() + "' does not belong to this struct");
    }
    if (field.index() <= lastIndex) {
      throw new IllegalArgumentException("No such field left : '" + field.name() + "'");
    }
    lastIndex = field.index();

    if (field.getClass() != fieldClazz) {
      throw new IllegalArgumentException("Invalid type for field '" + field.name() + "', expected : '" + fieldClazz.getSimpleName() + "' but was '" + field.getClass().getSimpleName() + "'");
    }
    if (subFieldClazz != null) {
      ArrayField arrayField = (ArrayField) field;
      Field nextSubField = arrayField.subField();
      if (!nextSubField.getClass().equals(subFieldClazz)) {
        throw new IllegalArgumentException("Invalid subtype for field '" + field.name() + "', expected : '" + subFieldClazz.getSimpleName() + "' but was '" + nextSubField.getClass().getSimpleName() + "'");
      }
    }
  }

  private <T extends Field, S extends Field> Field findFieldWithIndex(String name, Class<T> fieldClazz, Class<S> subFieldClazz) {
    Field field = metadata.getFieldByName(name);
    if (field == null) {
      throw new IllegalArgumentException("No such field : " + name);
    }
    if (field.index() <= lastIndex) {
      throw new IllegalArgumentException("No such field left : '" + name + "'");
    }
    lastIndex = field.index();

    if (field.getClass() != fieldClazz) {
      throw new IllegalArgumentException("Invalid type for field '" + name + "', expected : '" + fieldClazz.getSimpleName() + "' but was '" + field.getClass().getSimpleName() + "'");
    }
    if (subFieldClazz != null) {
      ArrayField arrayField = (ArrayField) field;
      Field nextSubField = arrayField.subField();
      if (!nextSubField.getClass().equals(subFieldClazz)) {
        throw new IllegalArgumentException("Invalid subtype for field '" + name + "', expected : '" + subFieldClazz.getSimpleName() + "' but was '" + nextSubField.getClass().getSimpleName() + "'");
      }
    }
    return field;
  }

}
