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
package org.terracotta.runnel.decoding;

import org.terracotta.runnel.decoding.fields.StructField;
import org.terracotta.runnel.utils.ReadBuffer;
import org.terracotta.runnel.utils.RunnelDecodingException;

import java.util.NoSuchElementException;

/**
 * @author Ludovic Orban
 */
public class StructArrayDecoder<P> implements DecodingIterator<StructDecoder<StructArrayDecoder<P>>> {
  private final P parent;
  private final ReadBuffer arrayReadBuffer;
  private final int arrayLength;
  private final StructField field;

  private StructDecoder<StructArrayDecoder<P>> current = null;
  private int count = 0;

  public StructArrayDecoder(StructField field, ReadBuffer readBuffer, P parent) throws RunnelDecodingException {
    this.parent = parent;
    this.field = field;
    int arraySize = readBuffer.getVlqInt();
    this.arrayReadBuffer = readBuffer.limit(arraySize);
    this.arrayLength = readBuffer.getVlqInt();
  }

  public P end() {
    arrayReadBuffer.skipAll();
    return parent;
  }

  @Override
  public boolean hasNext() {
    return count < arrayLength;
  }

  @Override
  public StructDecoder<StructArrayDecoder<P>> next() throws RunnelDecodingException {
    if (current != null) {
      current.end();
      current = null;
    }

    if (count >= arrayLength) {
      throw new NoSuchElementException();
    }

    current = new StructDecoder<>(field, arrayReadBuffer, this);
    count++;

    return current;
  }
}
