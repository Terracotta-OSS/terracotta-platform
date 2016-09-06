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

import org.terracotta.runnel.dataholders.ByteBufferDataHolder;
import org.terracotta.runnel.dataholders.DataHolder;
import org.terracotta.runnel.dataholders.Int32DataHolder;
import org.terracotta.runnel.dataholders.Int64DataHolder;
import org.terracotta.runnel.dataholders.StringDataHolder;
import org.terracotta.runnel.dataholders.StructDataHolder;
import org.terracotta.runnel.metadata.ByteBufferField;
import org.terracotta.runnel.metadata.Field;
import org.terracotta.runnel.metadata.Int32Field;
import org.terracotta.runnel.metadata.Int64Field;
import org.terracotta.runnel.metadata.StringField;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Ludovic Orban
 */
public class StructArrayEncoder {
  public static final int ARRAY_INITIAL_SIZE = 16;
  private final List<StructDataHolder> values;
  private final StructEncoder parent;
  private final Field structField;
  private final List<? extends Field> metadata;
  private int i = 0;
  private List<DataHolder> currentData;

  StructArrayEncoder(List<StructDataHolder> values, StructEncoder parent, Field structField) {
    this.values = values;
    this.parent = parent;
    this.structField = structField;
    this.metadata = structField.subFields();
    this.currentData = new ArrayList<DataHolder>(ARRAY_INITIAL_SIZE);
  }

  public StructArrayEncoder int32(String name, int value) {
    Field field = findMetadataFor(name, Int32Field.class);
    currentData.add(new Int32DataHolder(value, field.index()));
    return this;
  }

  public StructArrayEncoder int64(String name, long value) {
    Field field = findMetadataFor(name, Int64Field.class);
    currentData.add(new Int64DataHolder(value, field.index()));
    return this;
  }

  public StructArrayEncoder string(String name, String value) {
    Field field = findMetadataFor(name, StringField.class);
    currentData.add(new StringDataHolder(value, field.index()));
    return this;
  }

  public StructArrayEncoder byteBuffer(String name, ByteBuffer value) {
    Field field = findMetadataFor(name, ByteBufferField.class);
    currentData.add(new ByteBufferDataHolder(value, field.index()));
    return this;
  }

  public StructArrayEncoder next() {
    i = 0;
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

}
