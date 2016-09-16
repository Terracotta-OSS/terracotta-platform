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
package org.terracotta.runnel;

import org.terracotta.runnel.decoding.fields.ArrayField;
import org.terracotta.runnel.decoding.fields.ByteBufferField;
import org.terracotta.runnel.decoding.fields.Field;
import org.terracotta.runnel.decoding.fields.FloatingPoint64Field;
import org.terracotta.runnel.decoding.fields.Int32Field;
import org.terracotta.runnel.decoding.fields.Int64Field;
import org.terracotta.runnel.decoding.fields.StringField;
import org.terracotta.runnel.decoding.fields.StructField;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Ludovic Orban
 */
public class StructBuilder {

  private final List<Field> fields = new ArrayList<Field>();
  private final Set<String> names = new HashSet<String>();
  private int lastIndex = -1;

  public Struct build() {
    return new Struct(new StructField("root", -1, fields));
  }

  public static StructBuilder newStructBuilder() {
    return new StructBuilder();
  }


  public StructBuilder int32(String name, int index) {
    checkIndex(index);
    checkName(name);
    fields.add(new Int32Field(name, index));
    return this;
  }

  public StructBuilder int64(String name, int index) {
    checkIndex(index);
    fields.add(new Int64Field(name, index));
    return this;
  }

  public StructBuilder fp64(String name, int index) {
    checkIndex(index);
    fields.add(new FloatingPoint64Field(name, index));
    return this;
  }

  public StructBuilder string(String name, int index) {
    checkIndex(index);
    fields.add(new StringField(name, index));
    return this;
  }

  public StructBuilder byteBuffer(String name, int index) {
    checkIndex(index);
    fields.add(new ByteBufferField(name, index));
    return this;
  }

  public StructBuilder struct(String name, int index, Struct struct) {
    checkIndex(index);
    fields.add(new StructField(name, index, struct.getRoot().subFields()));
    return this;
  }

  public StructBuilder int32s(String name, int index) {
    checkIndex(index);
    fields.add(new ArrayField(name, index, new Int32Field(name, index)));
    return this;
  }

  public StructBuilder int64s(String name, int index) {
    checkIndex(index);
    fields.add(new ArrayField(name, index, new Int64Field(name, index)));
    return this;
  }

  public StructBuilder fp64s(String name, int index) {
    checkIndex(index);
    fields.add(new ArrayField(name, index, new FloatingPoint64Field(name, index)));
    return this;
  }

  public StructBuilder strings(String name, int index) {
    checkIndex(index);
    fields.add(new ArrayField(name, index, new StringField(name, index)));
    return this;
  }

  public StructBuilder structs(String name, int index, Struct struct) {
    checkIndex(index);
    fields.add(new ArrayField(name, index, new StructField(name, index, struct.getRoot().subFields())));
    return this;
  }

  private void checkIndex(int index) {
    if (index <= 0) {
      throw new IllegalArgumentException("Index must be greater than zero : " + index);
    }
    if (index <= lastIndex) {
      throw new IllegalArgumentException("Index must be registered in growing order : " + index + " (last registered one : " + lastIndex + ")");
    }
    lastIndex = index;
  }

  private void checkName(String name) {
    if (names.contains(name)) {
      throw new IllegalArgumentException("Duplicate name : " + name);
    }
    names.add(name);
  }

}
