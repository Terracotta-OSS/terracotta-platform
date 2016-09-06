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

import org.terracotta.runnel.dataholders.ArrayDataHolder;
import org.terracotta.runnel.dataholders.ByteBufferDataHolder;
import org.terracotta.runnel.dataholders.DataHolder;
import org.terracotta.runnel.dataholders.Int32DataHolder;
import org.terracotta.runnel.dataholders.Int64DataHolder;
import org.terracotta.runnel.dataholders.StringDataHolder;
import org.terracotta.runnel.dataholders.StructDataHolder;
import org.terracotta.runnel.metadata.ArrayField;
import org.terracotta.runnel.metadata.ByteBufferField;
import org.terracotta.runnel.metadata.Field;
import org.terracotta.runnel.metadata.Int32Field;
import org.terracotta.runnel.metadata.Int64Field;
import org.terracotta.runnel.metadata.StringField;
import org.terracotta.runnel.metadata.StructField;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Ludovic Orban
 */
public class StructEncoder {

  private final List<? extends Field> metadata;
  private final List<DataHolder> data;
  private final StructEncoder parent;
  private int i = 0;

  public StructEncoder(List<? extends Field> metadata) {
    this(metadata, new ArrayList<DataHolder>(), null);
  }

  private StructEncoder(List<? extends Field> metadata, List<DataHolder> values, StructEncoder structEncoder) {
    this.metadata = metadata;
    this.data = values;
    this.parent = structEncoder;
  }

  public StructEncoder int32(String name, int value) {
    Field field = findMetadataFor(name, Int32Field.class);
    data.add(new Int32DataHolder(value, field.index()));
    return this;
  }

  public StructEncoder int64(String name, long value) {
    Field field = findMetadataFor(name, Int64Field.class);
    data.add(new Int64DataHolder(value, field.index()));
    return this;
  }

  public StructEncoder string(String name, String value) {
    Field field = findMetadataFor(name, StringField.class);
    data.add(new StringDataHolder(value, field.index()));
    return this;
  }

  public StructEncoder byteBuffer(String name, ByteBuffer value) {
    Field field = findMetadataFor(name, ByteBufferField.class);
    data.add(new ByteBufferDataHolder(value, field.index()));
    return this;
  }

  public StructEncoder struct(String name) {
    Field field = findMetadataFor(name, StructField.class);
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
    final Field field = findMetadataFor(name, ArrayField.class);
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
    final Field field = findMetadataFor(name, ArrayField.class);
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
    final Field field = findMetadataFor(name, ArrayField.class);
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
    final Field field = findMetadataFor(name, ArrayField.class);
    List<StructDataHolder> values = new ArrayList<StructDataHolder>();
    data.add(new ArrayDataHolder(values, field.index()));
    return new StructArrayEncoder(values, this, field.subFields().get(0));
  }

  private Field findMetadataFor(String name, Class<? extends Field> clazz) {
    for (; i < metadata.size(); i++) {
      Field field = metadata.get(i);
      if (field.name().equals(name)) {
        if (field.getClass() != clazz) {
          throw new RuntimeException("Invalid type for field '" + name + "', expected : '" + clazz.getSimpleName() + "' but was '" + field.getClass().getSimpleName() + "'");
        }
        return field;
      }
    }
    throw new RuntimeException("No such field left : '" + name + "'");
  }

  public ByteBuffer encode(ByteBuffer bb) {
    int size = 0;
    for (DataHolder dataHolder : data) {
      size += dataHolder.size(true);
    }
    bb.putInt(size);

    for (DataHolder dataHolder : data) {
      dataHolder.encode(bb, true);
    }

    return bb;
  }

  public ByteBuffer encode() {
    int size = 0;
    for (DataHolder dataHolder : data) {
      size += dataHolder.size(true);
    }

    ByteBuffer bb = ByteBuffer.allocate(size + 4);

    bb.putInt(size);

    for (DataHolder dataHolder : data) {
      dataHolder.encode(bb, true);
    }

    return bb;
  }
}
