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

import org.terracotta.runnel.decoding.fields.StructField;
import org.terracotta.runnel.encoding.dataholders.DataHolder;
import org.terracotta.runnel.encoding.dataholders.StructDataHolder;
import org.terracotta.runnel.utils.SizeAccumulator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Ludovic Orban
 */
public abstract class StructArrayEncoder<P extends StructEncoder<?>> {

  private final List<StructDataHolder> values;
  private final P parent;
  private final StructField structField;
  private List<DataHolder> currentData;
  private StructEncoder<StructArrayEncoder<P>> lastStructEncoder;

  StructArrayEncoder(List<StructDataHolder> values, P parent, StructField structField) {
    this.structField = structField;
    this.values = values;
    this.parent = parent;
    this.currentData = new ArrayList<DataHolder>();
  }

  public StructEncoder<StructArrayEncoder<P>> add() {
    return add((size) -> {});
  }

  public StructEncoder<StructArrayEncoder<P>> add(SizeAccumulator sizeAccumulator) {
    if (!currentData.isEmpty()) {
      StructDataHolder dh = new StructDataHolder(currentData, -1);
      dh.cacheSize(lastStructEncoder.encodedSize);
      values.add(dh);
      // onDataHolderAddition is going to call size() on the data holder to calculate the header size,
      // so we must pass it a StructDataHolder with an empty DataHolder list
      StructDataHolder emptyDh = new StructDataHolder(Collections.emptyList(), -1);
      emptyDh.cacheSize(lastStructEncoder.encodedSize);
      onDataHolderAddition(emptyDh);
    }
    if (lastStructEncoder != null) {
      lastStructEncoder.end();
    }
    lastStructEncoder = new StructEncoder<StructArrayEncoder<P>>(structField, currentData = new ArrayList<>(), this) {
      boolean ended = false;
      @Override
      public StructArrayEncoder<P> end() {
        if (!ended) {
          sizeAccumulator.accumulate(encodedSize);
          ended = true;
        }
        return super.end();
      }
    };
    return lastStructEncoder;
  }

  public P end() {
    if (!currentData.isEmpty()) {
      StructDataHolder dh = new StructDataHolder(currentData, -1);
      dh.cacheSize(lastStructEncoder.encodedSize);
      values.add(dh);
      // onDataHolderAddition is going to call size() on the data holder to calculate the header size,
      // so we must pass it a StructDataHolder with an empty DataHolder list
      StructDataHolder emptyDh = new StructDataHolder(Collections.emptyList(), -1);
      emptyDh.cacheSize(lastStructEncoder.encodedSize);
      onDataHolderAddition(emptyDh);
    }
    if (lastStructEncoder != null) {
      lastStructEncoder.end();
    }
    return parent;
  }

  protected abstract void onDataHolderAddition(StructDataHolder dh);
}
