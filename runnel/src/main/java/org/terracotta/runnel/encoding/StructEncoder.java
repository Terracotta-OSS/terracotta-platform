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

import org.terracotta.runnel.encoding.dataholders.ArrayDataHolder;
import org.terracotta.runnel.encoding.dataholders.ByteBufferDataHolder;
import org.terracotta.runnel.encoding.dataholders.DataHolder;
import org.terracotta.runnel.encoding.dataholders.Int32DataHolder;
import org.terracotta.runnel.encoding.dataholders.Int64DataHolder;
import org.terracotta.runnel.encoding.dataholders.StringDataHolder;
import org.terracotta.runnel.encoding.dataholders.StructDataHolder;
import org.terracotta.runnel.decoding.fields.ArrayField;
import org.terracotta.runnel.decoding.fields.ByteBufferField;
import org.terracotta.runnel.decoding.fields.Field;
import org.terracotta.runnel.decoding.fields.Int32Field;
import org.terracotta.runnel.decoding.fields.Int64Field;
import org.terracotta.runnel.metadata.Metadata;
import org.terracotta.runnel.decoding.fields.StringField;
import org.terracotta.runnel.decoding.fields.StructField;
import org.terracotta.runnel.utils.VLQ;
import org.terracotta.runnel.utils.WriteBuffer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Ludovic Orban
 */
public class StructEncoder implements PrimitiveEncodingSupport<StructEncoder> {

  private final Metadata metadata;
  private final List<DataHolder> data;
  private final StructEncoder parent;

  public StructEncoder(List<? extends Field> fields) {
    this(fields, new ArrayList<DataHolder>(), null);
  }

  private StructEncoder(List<? extends Field> fields, List<DataHolder> values, StructEncoder structEncoder) {
    this.metadata = new Metadata(fields);
    this.data = values;
    this.parent = structEncoder;
  }

  @Override
  public StructEncoder int32(String name, int value) {
    Field field = metadata.findField(name, Int32Field.class, null);
    data.add(new Int32DataHolder(value, field.index()));
    return this;
  }

  @Override
  public StructEncoder int64(String name, long value) {
    Field field = metadata.findField(name, Int64Field.class, null);
    data.add(new Int64DataHolder(value, field.index()));
    return this;
  }

  @Override
  public StructEncoder string(String name, String value) {
    Field field = metadata.findField(name, StringField.class, null);
    data.add(new StringDataHolder(value, field.index()));
    return this;
  }

  @Override
  public StructEncoder byteBuffer(String name, ByteBuffer value) {
    Field field = metadata.findField(name, ByteBufferField.class, null);
    data.add(new ByteBufferDataHolder(value, field.index()));
    return this;
  }

  public StructEncoder struct(String name) {
    Field field = metadata.findField(name, StructField.class, null);
    List<DataHolder> values = new ArrayList<DataHolder>();
    data.add(new StructDataHolder(values, field.index()));
    return new StructEncoder(field.subFields(), values, this);
  }

  public StructEncoder end() {
    if (parent == null) {
      throw new RuntimeException("Cannot end root encoder");
    }
    return parent;
  }

  public ArrayEncoder<Integer> int32s(String name) {
    final Field field = metadata.findField(name, ArrayField.class, Int32Field.class);
    List<DataHolder> values = new ArrayList<DataHolder>();
    data.add(new ArrayDataHolder(values, field.index()));
    return new ArrayEncoder<Integer>(values, this) {
      @Override
      protected DataHolder buildDataHolder(Integer value) {
        return new Int32DataHolder(value, field.index());
      }
    };
  }

  public ArrayEncoder<Long> int64s(String name) {
    final Field field = metadata.findField(name, ArrayField.class, Int64Field.class);
    List<DataHolder> values = new ArrayList<DataHolder>();
    data.add(new ArrayDataHolder(values, field.index()));
    return new ArrayEncoder<Long>(values, this) {
      @Override
      protected DataHolder buildDataHolder(Long value) {
        return new Int64DataHolder(value, field.index());
      }
    };
  }

  public ArrayEncoder<String> strings(String name) {
    final Field field = metadata.findField(name, ArrayField.class, StringField.class);
    List<DataHolder> values = new ArrayList<DataHolder>();
    data.add(new ArrayDataHolder(values, field.index()));
    return new ArrayEncoder<String>(values, this) {
      @Override
      protected DataHolder buildDataHolder(String value) {
        return new StringDataHolder(value, field.index());
      }
    };
  }

  public StructArrayEncoder structs(String name) {
    final Field field = metadata.findField(name, ArrayField.class, StructField.class);
    List<StructDataHolder> values = new ArrayList<StructDataHolder>();
    data.add(new ArrayDataHolder(values, field.index()));
    return new StructArrayEncoder(values, this, field.subFields().get(0).subFields());
  }

  public ByteBuffer encode(ByteBuffer bb) {
    int size = 0;
    for (DataHolder dataHolder : data) {
      size += dataHolder.size(true);
    }

    WriteBuffer writeBuffer = new WriteBuffer(bb);
    writeBuffer.putVlqInt(size);

    for (DataHolder dataHolder : data) {
      dataHolder.encode(writeBuffer, true);
    }

    return bb;
  }

  public ByteBuffer encode() {
    int size = 0;
    for (DataHolder dataHolder : data) {
      size += dataHolder.size(true);
    }

    ByteBuffer bb = ByteBuffer.allocate(size + VLQ.encodedSize(size));
    WriteBuffer writeBuffer = new WriteBuffer(bb);
    writeBuffer.putVlqInt(size);

    for (DataHolder dataHolder : data) {
      dataHolder.encode(writeBuffer, true);
    }

    return bb;
  }
}
