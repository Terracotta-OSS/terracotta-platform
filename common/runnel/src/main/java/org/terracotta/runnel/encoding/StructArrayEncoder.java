/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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

import org.terracotta.runnel.decoding.fields.StructField;
import org.terracotta.runnel.encoding.dataholders.DataHolder;
import org.terracotta.runnel.encoding.dataholders.StructDataHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ludovic Orban
 */
public class StructArrayEncoder<P> {

  private final List<StructDataHolder> values;
  private final P parent;
  private final StructField structField;
  private List<DataHolder> currentData;

  StructArrayEncoder(List<StructDataHolder> values, P parent, StructField structField) {
    this.structField = structField;
    this.values = values;
    this.parent = parent;
    this.currentData = new ArrayList<>();
  }

  public StructEncoder<StructArrayEncoder<P>> add() {
    if (!currentData.isEmpty()) {
      values.add(new StructDataHolder(currentData, -1));
    }
    return new StructEncoder<>(structField, currentData = new ArrayList<>(), this);
  }

  public P end() {
    if (!currentData.isEmpty()) {
      values.add(new StructDataHolder(currentData, -1));
    }
    return parent;
  }
}
