/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
package org.terracotta.runnel.encoding;

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
import org.terracotta.runnel.encoding.dataholders.ArrayDataHolder;
import org.terracotta.runnel.encoding.dataholders.BoolArrayDataHolder;
import org.terracotta.runnel.encoding.dataholders.BoolDataHolder;
import org.terracotta.runnel.encoding.dataholders.ByteBufferArrayDataHolder;
import org.terracotta.runnel.encoding.dataholders.ByteBufferDataHolder;
import org.terracotta.runnel.encoding.dataholders.CharArrayDataHolder;
import org.terracotta.runnel.encoding.dataholders.CharDataHolder;
import org.terracotta.runnel.encoding.dataholders.DataHolder;
import org.terracotta.runnel.encoding.dataholders.EnumDataHolder;
import org.terracotta.runnel.encoding.dataholders.FloatingPoint64ArrayDataHolder;
import org.terracotta.runnel.encoding.dataholders.FloatingPoint64DataHolder;
import org.terracotta.runnel.encoding.dataholders.Int32ArrayDataHolder;
import org.terracotta.runnel.encoding.dataholders.Int32DataHolder;
import org.terracotta.runnel.encoding.dataholders.Int64ArrayDataHolder;
import org.terracotta.runnel.encoding.dataholders.Int64DataHolder;
import org.terracotta.runnel.encoding.dataholders.StringArrayDataHolder;
import org.terracotta.runnel.encoding.dataholders.StringDataHolder;
import org.terracotta.runnel.encoding.dataholders.StructDataHolder;
import org.terracotta.runnel.metadata.FieldSearcher;
import org.terracotta.runnel.utils.VLQ;
import org.terracotta.runnel.utils.WriteBuffer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * An encoder allows encoding structured data described by a {@link org.terracotta.runnel.Struct}.
 * Note: Instances of this class are not thread-safe.
 */
public class StructEncoder<P> implements PrimitiveEncodingSupport<StructEncoder<P>> {

  private final FieldSearcher fieldSearcher;
  private final List<DataHolder> data;
  private final P parent;

  public StructEncoder(StructField structField) {
    this(structField, new ArrayList<>(structField.getMetadata().fieldCount()), null);
  }

  StructEncoder(StructField structField, List<DataHolder> values, P parent) {
    this.fieldSearcher = structField.getMetadata().fieldSearcher();
    this.data = values;
    this.parent = parent;
  }

  @Override
  public StructEncoder<P> bool(String name, boolean value) {
    BoolField field = fieldSearcher.findField(name, BoolField.class, null);
    data.add(new BoolDataHolder(value, field.index()));
    return this;
  }

  public StructEncoder<P> bool(BoolField field, boolean value) {
    fieldSearcher.findField(field, BoolField.class, null);
    data.add(new BoolDataHolder(value, field.index()));
    return this;
  }

  @Override
  public StructEncoder<P> chr(String name, char value) {
    CharField field = fieldSearcher.findField(name, CharField.class, null);
    data.add(new CharDataHolder(value, field.index()));
    return this;
  }

  public StructEncoder<P> chr(CharField field, char value) {
    fieldSearcher.findField(field, CharField.class, null);
    data.add(new CharDataHolder(value, field.index()));
    return this;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <E> StructEncoder<P> enm(String name, E value) {
    EnumField<E> field = (EnumField<E>) fieldSearcher.findField(name, EnumField.class, null);
    data.add(new EnumDataHolder<>(value, field.index(), field.getEnumMapping()));
    return this;
  }

  public <E> StructEncoder<P> enm(EnumField<E> field, E value) {
    fieldSearcher.findField(field, enumFieldClass(), null);
    data.add(new EnumDataHolder<>(value, field.index(), field.getEnumMapping()));
    return this;
  }

  @Override
  public StructEncoder<P> int32(String name, int value) {
    Int32Field field = fieldSearcher.findField(name, Int32Field.class, null);
    data.add(new Int32DataHolder(value, field.index()));
    return this;
  }

  public StructEncoder<P> int32(Int32Field field, int value) {
    fieldSearcher.findField(field, Int32Field.class, null);
    data.add(new Int32DataHolder(value, field.index()));
    return this;
  }

  @Override
  public StructEncoder<P> int64(String name, long value) {
    Int64Field field = fieldSearcher.findField(name, Int64Field.class, null);
    data.add(new Int64DataHolder(value, field.index()));
    return this;
  }

  public StructEncoder<P> int64(Int64Field field, long value) {
    fieldSearcher.findField(field, Int64Field.class, null);
    data.add(new Int64DataHolder(value, field.index()));
    return this;
  }

  @Override
  public StructEncoder<P> fp64(String name, double value) {
    FloatingPoint64Field field = fieldSearcher.findField(name, FloatingPoint64Field.class, null);
    data.add(new FloatingPoint64DataHolder(value, field.index()));
    return this;
  }

  public StructEncoder<P> fp64(FloatingPoint64Field field, double value) {
    fieldSearcher.findField(field, FloatingPoint64Field.class, null);
    data.add(new FloatingPoint64DataHolder(value, field.index()));
    return this;
  }

  @Override
  public StructEncoder<P> string(String name, String value) {
    StringField field = fieldSearcher.findField(name, StringField.class, null);
    if (value != null) {
      data.add(new StringDataHolder(value, field.index()));
    }
    return this;
  }

  public StructEncoder<P> string(StringField field, String value) {
    fieldSearcher.findField(field, StringField.class, null);
    if (value != null) {
      data.add(new StringDataHolder(value, field.index()));
    }
    return this;
  }

  @Override
  public StructEncoder<P> byteBuffer(String name, ByteBuffer value) {
    ByteBufferField field = fieldSearcher.findField(name, ByteBufferField.class, null);
    data.add(new ByteBufferDataHolder(value, field.index()));
    return this;
  }

  public StructEncoder<P> byteBuffer(ByteBufferField field, ByteBuffer value) {
    fieldSearcher.findField(field, ByteBufferField.class, null);
    data.add(new ByteBufferDataHolder(value, field.index()));
    return this;
  }

  public <T> StructEncoder<P> struct(String name, T value, StructEncoderFunction<T> function) {
    StructEncoder<StructEncoder<P>> subStructEncoder = struct(name);
    function.encode(subStructEncoder, value);
    subStructEncoder.end();
    return this;
  }

  public StructEncoder<StructEncoder<P>> struct(String name) {
    StructField field = fieldSearcher.findField(name, StructField.class, null);
    List<DataHolder> values = new ArrayList<>(field.getMetadata().fieldCount());
    data.add(new StructDataHolder(values, field.index()));
    return new StructEncoder<>(field, values, this);
  }

  public StructEncoder<StructEncoder<P>> struct(StructField field) {
    fieldSearcher.findField(field, StructField.class, null);
    List<DataHolder> values = new ArrayList<>(field.getMetadata().fieldCount());
    data.add(new StructDataHolder(values, field.index()));
    return new StructEncoder<>(field, values, this);
  }

  public P end() {
    if (parent == null) {
      throw new IllegalStateException("Cannot end root encoder");
    }
    return parent;
  }

  public ArrayEncoder<Boolean, StructEncoder<P>> bools(String name) {
    return boolsEncoder(fieldSearcher.findField(name, ArrayField.class, BoolField.class));
  }

  public ArrayEncoder<Boolean, StructEncoder<P>> bools(ArrayField field) {
    fieldSearcher.findField(field, ArrayField.class, BoolField.class);
    return boolsEncoder(field);
  }

  private ArrayEncoder<Boolean, StructEncoder<P>> boolsEncoder(final ArrayField field) {
    List<DataHolder> values = new ArrayList<>();
    data.add(new ArrayDataHolder(values, field.index()));
    return new ArrayEncoder<Boolean, StructEncoder<P>>(values, this) {
      @Override
      protected DataHolder buildDataHolder(Boolean value) {
        return new BoolDataHolder(value, field.index());
      }
    };
  }

  /**
   * Bulk-encode a boolean array without per-element holder objects or boxing.
   * Produces byte-identical output to {@link #bools(String)} fed with the same values.
   */
  public StructEncoder<P> bools(String name, boolean[] values) {
    ArrayField field = fieldSearcher.findField(name, ArrayField.class, BoolField.class);
    data.add(new BoolArrayDataHolder(values, field.index()));
    return this;
  }

  public StructEncoder<P> bools(ArrayField field, boolean[] values) {
    fieldSearcher.findField(field, ArrayField.class, BoolField.class);
    data.add(new BoolArrayDataHolder(values, field.index()));
    return this;
  }

  public ArrayEncoder<Character, StructEncoder<P>> chrs(String name) {
    return chrsEncoder(fieldSearcher.findField(name, ArrayField.class, CharField.class));
  }

  public ArrayEncoder<Character, StructEncoder<P>> chrs(ArrayField field) {
    fieldSearcher.findField(field, ArrayField.class, CharField.class);
    return chrsEncoder(field);
  }

  private ArrayEncoder<Character, StructEncoder<P>> chrsEncoder(final ArrayField field) {
    List<DataHolder> values = new ArrayList<>();
    data.add(new ArrayDataHolder(values, field.index()));
    return new ArrayEncoder<Character, StructEncoder<P>>(values, this) {
      @Override
      protected DataHolder buildDataHolder(Character value) {
        return new CharDataHolder(value, field.index());
      }
    };
  }

  /**
   * Bulk-encode a char array without per-element holder objects or boxing.
   * Produces byte-identical output to {@link #chrs(String)} fed with the same values.
   */
  public StructEncoder<P> chrs(String name, char[] values) {
    ArrayField field = fieldSearcher.findField(name, ArrayField.class, CharField.class);
    data.add(new CharArrayDataHolder(values, field.index()));
    return this;
  }

  public StructEncoder<P> chrs(ArrayField field, char[] values) {
    fieldSearcher.findField(field, ArrayField.class, CharField.class);
    data.add(new CharArrayDataHolder(values, field.index()));
    return this;
  }

  public ArrayEncoder<Integer, StructEncoder<P>> int32s(String name) {
    return int32sEncoder(fieldSearcher.findField(name, ArrayField.class, Int32Field.class));
  }

  public ArrayEncoder<Integer, StructEncoder<P>> int32s(ArrayField field) {
    fieldSearcher.findField(field, ArrayField.class, Int32Field.class);
    return int32sEncoder(field);
  }

  private ArrayEncoder<Integer, StructEncoder<P>> int32sEncoder(final ArrayField field) {
    List<DataHolder> values = new ArrayList<>();
    data.add(new ArrayDataHolder(values, field.index()));
    return new ArrayEncoder<Integer, StructEncoder<P>>(values, this) {
      @Override
      protected DataHolder buildDataHolder(Integer value) {
        return new Int32DataHolder(value, field.index());
      }
    };
  }

  /**
   * Bulk-encode an int array without per-element holder objects or boxing.
   * Produces byte-identical output to {@link #int32s(String)} fed with the same values.
   */
  public StructEncoder<P> int32s(String name, int[] values) {
    ArrayField field = fieldSearcher.findField(name, ArrayField.class, Int32Field.class);
    data.add(new Int32ArrayDataHolder(values, field.index()));
    return this;
  }

  public StructEncoder<P> int32s(ArrayField field, int[] values) {
    fieldSearcher.findField(field, ArrayField.class, Int32Field.class);
    data.add(new Int32ArrayDataHolder(values, field.index()));
    return this;
  }

  public ArrayEncoder<Long, StructEncoder<P>> int64s(String name) {
    return int64sEncoder(fieldSearcher.findField(name, ArrayField.class, Int64Field.class));
  }

  public ArrayEncoder<Long, StructEncoder<P>> int64s(ArrayField field) {
    fieldSearcher.findField(field, ArrayField.class, Int64Field.class);
    return int64sEncoder(field);
  }

  private ArrayEncoder<Long, StructEncoder<P>> int64sEncoder(final ArrayField field) {
    List<DataHolder> values = new ArrayList<>();
    data.add(new ArrayDataHolder(values, field.index()));
    return new ArrayEncoder<Long, StructEncoder<P>>(values, this) {
      @Override
      protected DataHolder buildDataHolder(Long value) {
        return new Int64DataHolder(value, field.index());
      }
    };
  }

  /**
   * Bulk-encode a long array without per-element holder objects or boxing.
   * Produces byte-identical output to {@link #int64s(String)} fed with the same values.
   */
  public StructEncoder<P> int64s(String name, long[] values) {
    ArrayField field = fieldSearcher.findField(name, ArrayField.class, Int64Field.class);
    data.add(new Int64ArrayDataHolder(values, field.index()));
    return this;
  }

  public StructEncoder<P> int64s(ArrayField field, long[] values) {
    fieldSearcher.findField(field, ArrayField.class, Int64Field.class);
    data.add(new Int64ArrayDataHolder(values, field.index()));
    return this;
  }

  public ArrayEncoder<Double, StructEncoder<P>> fp64s(String name) {
    return fp64sEncoder(fieldSearcher.findField(name, ArrayField.class, FloatingPoint64Field.class));
  }

  public ArrayEncoder<Double, StructEncoder<P>> fp64s(ArrayField field) {
    fieldSearcher.findField(field, ArrayField.class, FloatingPoint64Field.class);
    return fp64sEncoder(field);
  }

  private ArrayEncoder<Double, StructEncoder<P>> fp64sEncoder(final ArrayField field) {
    List<DataHolder> values = new ArrayList<>();
    data.add(new ArrayDataHolder(values, field.index()));
    return new ArrayEncoder<Double, StructEncoder<P>>(values, this) {
      @Override
      protected DataHolder buildDataHolder(Double value) {
        return new FloatingPoint64DataHolder(value, field.index());
      }
    };
  }

  /**
   * Bulk-encode a double array without per-element holder objects or boxing.
   * Produces byte-identical output to {@link #fp64s(String)} fed with the same values.
   */
  public StructEncoder<P> fp64s(String name, double[] values) {
    ArrayField field = fieldSearcher.findField(name, ArrayField.class, FloatingPoint64Field.class);
    data.add(new FloatingPoint64ArrayDataHolder(values, field.index()));
    return this;
  }

  public StructEncoder<P> fp64s(ArrayField field, double[] values) {
    fieldSearcher.findField(field, ArrayField.class, FloatingPoint64Field.class);
    data.add(new FloatingPoint64ArrayDataHolder(values, field.index()));
    return this;
  }

  public ArrayEncoder<String, StructEncoder<P>> strings(String name) {
    return stringsEncoder(fieldSearcher.findField(name, ArrayField.class, StringField.class));
  }

  public ArrayEncoder<String, StructEncoder<P>> strings(ArrayField field) {
    fieldSearcher.findField(field, ArrayField.class, StringField.class);
    return stringsEncoder(field);
  }

  private ArrayEncoder<String, StructEncoder<P>> stringsEncoder(final ArrayField field) {
    List<DataHolder> values = new ArrayList<>();
    data.add(new ArrayDataHolder(values, field.index()));
    return new ArrayEncoder<String, StructEncoder<P>>(values, this) {
      @Override
      protected DataHolder buildDataHolder(String value) {
        return new StringDataHolder(value, field.index());
      }
    };
  }

  /**
   * Bulk-encode a string array without per-element holder objects.
   * Produces byte-identical output to {@link #strings(String)} fed with the same values.
   */
  public StructEncoder<P> strings(String name, String[] values) {
    ArrayField field = fieldSearcher.findField(name, ArrayField.class, StringField.class);
    data.add(new StringArrayDataHolder(values, field.index()));
    return this;
  }

  public StructEncoder<P> strings(ArrayField field, String[] values) {
    fieldSearcher.findField(field, ArrayField.class, StringField.class);
    data.add(new StringArrayDataHolder(values, field.index()));
    return this;
  }

  public ArrayEncoder<ByteBuffer, StructEncoder<P>> byteBuffers(String name) {
    return byteBuffersEncoder(fieldSearcher.findField(name, ArrayField.class, ByteBufferField.class));
  }

  public ArrayEncoder<ByteBuffer, StructEncoder<P>> byteBuffers(ArrayField field) {
    fieldSearcher.findField(field, ArrayField.class, ByteBufferField.class);
    return byteBuffersEncoder(field);
  }

  private ArrayEncoder<ByteBuffer, StructEncoder<P>> byteBuffersEncoder(final ArrayField field) {
    List<DataHolder> values = new ArrayList<>();
    data.add(new ArrayDataHolder(values, field.index()));
    return new ArrayEncoder<ByteBuffer, StructEncoder<P>>(values, this) {
      @Override
      protected DataHolder buildDataHolder(ByteBuffer value) {
        return new ByteBufferDataHolder(value, field.index());
      }
    };
  }

  /**
   * Bulk-encode a byte buffer array without per-element holder objects.
   * Produces byte-identical output to {@link #byteBuffers(String)} fed with the same values.
   */
  public StructEncoder<P> byteBuffers(String name, ByteBuffer[] values) {
    ArrayField field = fieldSearcher.findField(name, ArrayField.class, ByteBufferField.class);
    data.add(new ByteBufferArrayDataHolder(values, field.index()));
    return this;
  }

  public StructEncoder<P> byteBuffers(ArrayField field, ByteBuffer[] values) {
    fieldSearcher.findField(field, ArrayField.class, ByteBufferField.class);
    data.add(new ByteBufferArrayDataHolder(values, field.index()));
    return this;
  }

  public StructArrayEncoder<StructEncoder<P>> structs(String name) {
    ArrayField field = fieldSearcher.findField(name, ArrayField.class, StructField.class);
    List<StructDataHolder> values = new ArrayList<>();
    data.add(new ArrayDataHolder(values, field.index()));
    return new StructArrayEncoder<>(values, this, ((StructField) field.subField()));
  }

  public StructArrayEncoder<StructEncoder<P>> structs(ArrayField field) {
    fieldSearcher.findField(field, ArrayField.class, StructField.class);
    List<StructDataHolder> values = new ArrayList<>();
    data.add(new ArrayDataHolder(values, field.index()));
    return new StructArrayEncoder<>(values, this, ((StructField) field.subField()));
  }

  public <T> StructEncoder<P> structs(String name, T[] array, StructEncoderFunction<T> function) {
    return structs(name, Arrays.asList(array), function);
  }

  public <T> StructEncoder<P> structs(String name, Iterable<T> iterable, StructEncoderFunction<T> function) {
    StructArrayEncoder<StructEncoder<P>> subStructArrayEncoder = structs(name);
    for (T t : iterable) {
      function.encode(subStructArrayEncoder.add(), t);
    }
    subStructArrayEncoder.end();
    return this;
  }


  /**
   * Computes the total number of bytes a call to {@link #encode()} or {@link #encode(ByteBuffer)}
   * will produce for this structure as it stands at the time of this call, allowing callers to
   * pre-size or pool a target buffer. Must only be called on the root encoder.
   * <p>
   * The returned value goes stale if fields are added afterwards — {@link #encode()} recalculates
   * and encodes such a modified structure correctly. Encoding fails with an
   * {@link IllegalStateException} only when a nested array or struct was extended after this
   * call, or a {@link java.nio.ByteBuffer} value was consumed or shared during encoding —
   * cases that would otherwise silently emit a corrupt frame. Passing a buffer sized from a
   * stale result to {@link #encode(ByteBuffer)} naturally fails with a
   * {@link java.nio.BufferOverflowException}.
   * @return the total encoded size in bytes, including the size prefix
   */
  public int encodedSize() {
    if (parent != null) {
      throw new IllegalStateException("Cannot size non-root encoder");
    }
    int size = calculateSize();
    return size + VLQ.encodedSize(size);
  }

  /**
   * Encode the structure in the given byte buffer.
   * @param bb the byte buffer to fill with the encoded structure. Its remaining capacity must be
   * at least {@link #encodedSize()} bytes.
   * @return the passed-in byte buffer.
   */
  public ByteBuffer encode(ByteBuffer bb) {
    if (parent != null) {
      throw new IllegalStateException("Cannot encode non-root encoder");
    }
    int size = calculateSize();
    return performEncoding(bb, size);
  }

  /**
   * Size the structure and return a newly allocated byte buffer containing the encoded structure of the exact size.
   * The returned byte buffer is heap-allocated, so it supports {@link ByteBuffer#array()}.
   * @return the encoded structure in a new byte buffer.
   */
  public ByteBuffer encode() {
    if (parent != null) {
      throw new IllegalStateException("Cannot encode non-root encoder");
    }
    int size = calculateSize();
    ByteBuffer bb = ByteBuffer.allocate(size + VLQ.encodedSize(size));
    return performEncoding(bb, size);
  }

  private int calculateSize() {
    int size = 0;
    for (DataHolder dataHolder : data) {
      size += dataHolder.size(true);
    }
    return size;
  }

  private ByteBuffer performEncoding(ByteBuffer bb, int size) {
    int start = bb.position();
    WriteBuffer writeBuffer = new WriteBuffer(bb);
    writeBuffer.putVlqInt(size);

    for (DataHolder dataHolder : data) {
      dataHolder.encode(writeBuffer, true);
    }

    /*
     * Nested holders memoize their sizes, so a structure modified after it was sized (e.g. fields
     * added after encodedSize()), or a ByteBuffer value consumed or shared within the structure,
     * yields length prefixes that disagree with the written bytes. Fail loudly rather than emit a
     * corrupt frame.
     */
    int written = bb.position() - start;
    int expected = size + VLQ.encodedSize(size);
    if (written != expected) {
      throw new IllegalStateException("Encoded " + written + " bytes but the computed size was " + expected
          + " bytes: the structure was modified after being sized, or a ByteBuffer value was modified or shared during encoding");
    }
    return bb;
  }

  @SuppressWarnings("unchecked")
  private static <E> Class<EnumField<E>> enumFieldClass() {
    return (Class<EnumField<E>>) (Class<?>) EnumField.class;
  }

}
