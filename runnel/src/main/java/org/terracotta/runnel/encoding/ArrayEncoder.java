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

import org.terracotta.runnel.encoding.dataholders.DataHolder;

import java.util.List;

/**
 * @author Ludovic Orban
 */
public abstract class ArrayEncoder<T, P> {

  private final P parent;
  private final List<DataHolder> values;

  ArrayEncoder(List<DataHolder> values, P parent) {
    this.values = values;
    this.parent = parent;
  }

  public ArrayEncoder<T, P> value(T value) {
    DataHolder dataHolder = buildDataHolder(value);
    this.values.add(dataHolder);
    return this;
  }

  protected abstract DataHolder buildDataHolder(T value);

  public P end() {
    return parent;
  }

}
