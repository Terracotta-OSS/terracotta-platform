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

import org.terracotta.runnel.decoding.fields.BoolField;
import org.terracotta.runnel.decoding.fields.EnumField;
import org.terracotta.runnel.decoding.fields.FloatingPoint64Field;
import org.terracotta.runnel.decoding.fields.StructField;
import org.terracotta.runnel.encoding.dataholders.BoolDataHolder;
import org.terracotta.runnel.encoding.dataholders.ByteBufferDataHolder;
import org.terracotta.runnel.encoding.dataholders.DataHolder;
import org.terracotta.runnel.encoding.dataholders.EnumDataHolder;
import org.terracotta.runnel.encoding.dataholders.FloatingPoint64DataHolder;
import org.terracotta.runnel.encoding.dataholders.Int32DataHolder;
import org.terracotta.runnel.encoding.dataholders.Int64DataHolder;
import org.terracotta.runnel.encoding.dataholders.StringDataHolder;
import org.terracotta.runnel.encoding.dataholders.StructDataHolder;
import org.terracotta.runnel.decoding.fields.ByteBufferField;
import org.terracotta.runnel.decoding.fields.Int32Field;
import org.terracotta.runnel.decoding.fields.Int64Field;
import org.terracotta.runnel.metadata.FieldSearcher;
import org.terracotta.runnel.decoding.fields.StringField;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Ludovic Orban
 */
public class StructArrayEncoder implements PrimitiveEncodingSupport<StructArrayEncoder> {
  private static final int ARRAY_INITIAL_SIZE = 16;

  private final List<StructDataHolder> values;
  private final StructEncoder parent;
  private final FieldSearcher fieldSearcher;
  private List<DataHolder> currentData;

  StructArrayEncoder(List<StructDataHolder> values, StructEncoder parent, StructField structField) {
    this.values = values;
    this.parent = parent;
    this.fieldSearcher = structField.getMetadata().fieldSearcher();
    this.currentData = new ArrayList<DataHolder>(ARRAY_INITIAL_SIZE);
  }

  @Override
  public StructArrayEncoder bool(String name, boolean value) {
    BoolField field = fieldSearcher.findField(name, BoolField.class, null);
    currentData.add(new BoolDataHolder(value, field.index()));
    return this;
  }

  @Override
  public <E> StructArrayEncoder enm(String name, E value) {
    EnumField<E> field = (EnumField<E>) fieldSearcher.findField(name, EnumField.class, null);
    currentData.add(new EnumDataHolder<E>(value, field.index(), field.getEnumMapping()));
    return this;
  }

  @Override
  public StructArrayEncoder int32(String name, int value) {
    Int32Field field = fieldSearcher.findField(name, Int32Field.class, null);
    currentData.add(new Int32DataHolder(value, field.index()));
    return this;
  }

  @Override
  public StructArrayEncoder int64(String name, long value) {
    Int64Field field = fieldSearcher.findField(name, Int64Field.class, null);
    currentData.add(new Int64DataHolder(value, field.index()));
    return this;
  }

  @Override
  public StructArrayEncoder fp64(String name, double value) {
    FloatingPoint64Field field = fieldSearcher.findField(name, FloatingPoint64Field.class, null);
    currentData.add(new FloatingPoint64DataHolder(value, field.index()));
    return this;
  }

  @Override
  public StructArrayEncoder string(String name, String value) {
    StringField field = fieldSearcher.findField(name, StringField.class, null);
    currentData.add(new StringDataHolder(value, field.index()));
    return this;
  }

  @Override
  public StructArrayEncoder byteBuffer(String name, ByteBuffer value) {
    ByteBufferField field = fieldSearcher.findField(name, ByteBufferField.class, null);
    currentData.add(new ByteBufferDataHolder(value, field.index()));
    return this;
  }

  public StructArrayEncoder next() {
    fieldSearcher.reset();
    values.add(new StructDataHolder(currentData, -1));
    currentData = new ArrayList<DataHolder>(ARRAY_INITIAL_SIZE);
    return this;
  }

  public StructEncoder end() {
    if (!currentData.isEmpty()) {
      values.add(new StructDataHolder(currentData, -1));
    }
    return parent;
  }

}
