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
import org.terracotta.runnel.encoding.dataholders.BoolDataHolder;
import org.terracotta.runnel.encoding.dataholders.ByteBufferDataHolder;
import org.terracotta.runnel.encoding.dataholders.CharDataHolder;
import org.terracotta.runnel.encoding.dataholders.DataHolder;
import org.terracotta.runnel.encoding.dataholders.EnumDataHolder;
import org.terracotta.runnel.encoding.dataholders.FloatingPoint64DataHolder;
import org.terracotta.runnel.encoding.dataholders.Int32DataHolder;
import org.terracotta.runnel.encoding.dataholders.Int64DataHolder;
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
    this(structField, new ArrayList<DataHolder>(), null);
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

  @Override
  public StructEncoder<P> chr(String name, char value) {
    CharField field = fieldSearcher.findField(name, CharField.class, null);
    data.add(new CharDataHolder(value, field.index()));
    return this;
  }

  @Override
  public <E> StructEncoder<P> enm(String name, E value) {
    EnumField<E> field = (EnumField<E>) fieldSearcher.findField(name, EnumField.class, null);
    data.add(new EnumDataHolder<E>(value, field.index(), field.getEnumMapping()));
    return this;
  }

  @Override
  public StructEncoder<P> int32(String name, int value) {
    Int32Field field = fieldSearcher.findField(name, Int32Field.class, null);
    data.add(new Int32DataHolder(value, field.index()));
    return this;
  }

  @Override
  public StructEncoder<P> int64(String name, long value) {
    Int64Field field = fieldSearcher.findField(name, Int64Field.class, null);
    data.add(new Int64DataHolder(value, field.index()));
    return this;
  }

  @Override
  public StructEncoder<P> fp64(String name, double value) {
    FloatingPoint64Field field = fieldSearcher.findField(name, FloatingPoint64Field.class, null);
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

  @Override
  public StructEncoder<P> byteBuffer(String name, ByteBuffer value) {
    ByteBufferField field = fieldSearcher.findField(name, ByteBufferField.class, null);
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
    List<DataHolder> values = new ArrayList<DataHolder>();
    data.add(new StructDataHolder(values, field.index()));
    return new StructEncoder<StructEncoder<P>>(field, values, this);
  }

  public P end() {
    if (parent == null) {
      throw new IllegalStateException("Cannot end root encoder");
    }
    return parent;
  }

  public ArrayEncoder<Boolean, StructEncoder<P>> bools(String name) {
    final ArrayField field = fieldSearcher.findField(name, ArrayField.class, BoolField.class);
    List<DataHolder> values = new ArrayList<DataHolder>();
    data.add(new ArrayDataHolder(values, field.index()));
    return new ArrayEncoder<Boolean, StructEncoder<P>>(values, this) {
      @Override
      protected DataHolder buildDataHolder(Boolean value) {
        return new BoolDataHolder(value, field.index());
      }
    };
  }

  public ArrayEncoder<Character, StructEncoder<P>> chrs(String name) {
    final ArrayField field = fieldSearcher.findField(name, ArrayField.class, CharField.class);
    List<DataHolder> values = new ArrayList<DataHolder>();
    data.add(new ArrayDataHolder(values, field.index()));
    return new ArrayEncoder<Character, StructEncoder<P>>(values, this) {
      @Override
      protected DataHolder buildDataHolder(Character value) {
        return new CharDataHolder(value, field.index());
      }
    };
  }

  public ArrayEncoder<Integer, StructEncoder<P>> int32s(String name) {
    final ArrayField field = fieldSearcher.findField(name, ArrayField.class, Int32Field.class);
    List<DataHolder> values = new ArrayList<DataHolder>();
    data.add(new ArrayDataHolder(values, field.index()));
    return new ArrayEncoder<Integer, StructEncoder<P>>(values, this) {
      @Override
      protected DataHolder buildDataHolder(Integer value) {
        return new Int32DataHolder(value, field.index());
      }
    };
  }

  public ArrayEncoder<Long, StructEncoder<P>> int64s(String name) {
    final ArrayField field = fieldSearcher.findField(name, ArrayField.class, Int64Field.class);
    List<DataHolder> values = new ArrayList<DataHolder>();
    data.add(new ArrayDataHolder(values, field.index()));
    return new ArrayEncoder<Long, StructEncoder<P>>(values, this) {
      @Override
      protected DataHolder buildDataHolder(Long value) {
        return new Int64DataHolder(value, field.index());
      }
    };
  }

  public ArrayEncoder<Double, StructEncoder<P>> fp64s(String name) {
    final ArrayField field = fieldSearcher.findField(name, ArrayField.class, FloatingPoint64Field.class);
    List<DataHolder> values = new ArrayList<DataHolder>();
    data.add(new ArrayDataHolder(values, field.index()));
    return new ArrayEncoder<Double, StructEncoder<P>>(values, this) {
      @Override
      protected DataHolder buildDataHolder(Double value) {
        return new FloatingPoint64DataHolder(value, field.index());
      }
    };
  }

  public ArrayEncoder<String, StructEncoder<P>> strings(String name) {
    final ArrayField field = fieldSearcher.findField(name, ArrayField.class, StringField.class);
    List<DataHolder> values = new ArrayList<DataHolder>();
    data.add(new ArrayDataHolder(values, field.index()));
    return new ArrayEncoder<String, StructEncoder<P>>(values, this) {
      @Override
      protected DataHolder buildDataHolder(String value) {
        return new StringDataHolder(value, field.index());
      }
    };
  }

  public StructArrayEncoder<StructEncoder<P>> structs(String name) {
    final ArrayField field = fieldSearcher.findField(name, ArrayField.class, StructField.class);
    List<StructDataHolder> values = new ArrayList<StructDataHolder>();
    data.add(new ArrayDataHolder(values, field.index()));
    return new StructArrayEncoder<StructEncoder<P>>(values, this, ((StructField) field.subField()));
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
   * Encode the structure in the passed byte buffer.
   * @param bb the byte buffer to fill with the encoded structure.
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
    WriteBuffer writeBuffer = new WriteBuffer(bb);
    writeBuffer.putVlqInt(size);

    for (DataHolder dataHolder : data) {
      dataHolder.encode(writeBuffer, true);
    }

    return bb;
  }

}
