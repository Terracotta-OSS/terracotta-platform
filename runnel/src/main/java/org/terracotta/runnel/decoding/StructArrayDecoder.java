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

import org.terracotta.runnel.decoding.fields.BoolField;
import org.terracotta.runnel.decoding.fields.ByteBufferField;
import org.terracotta.runnel.decoding.fields.CharField;
import org.terracotta.runnel.decoding.fields.EnumField;
import org.terracotta.runnel.decoding.fields.FloatingPoint64Field;
import org.terracotta.runnel.decoding.fields.Int32Field;
import org.terracotta.runnel.decoding.fields.Int64Field;
import org.terracotta.runnel.decoding.fields.StringField;
import org.terracotta.runnel.decoding.fields.StructField;
import org.terracotta.runnel.metadata.FieldDecoder;
import org.terracotta.runnel.utils.ReadBuffer;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author Ludovic Orban
 */
public class StructArrayDecoder<P> implements Iterator<StructDecoder<StructArrayDecoder<P>>> {
  private final P parent;
  private final ReadBuffer arrayReadBuffer;
  private final int arrayLength;
  private final StructField field;

  private StructDecoder<StructArrayDecoder<P>> current = null;

  public StructArrayDecoder(StructField field, ReadBuffer readBuffer, P parent) {
    this.parent = parent;
    this.field = field;
    int arraySize = readBuffer.getVlqInt();
    this.arrayReadBuffer = readBuffer.limit(arraySize);
    this.arrayLength = readBuffer.getVlqInt();

  }

  public int length() {
    return arrayLength;
  }

  public P end() {
    arrayReadBuffer.skipAll();
    return parent;
  }

  @Override
  public boolean hasNext() {
    return !arrayReadBuffer.limitReached();
  }

  public StructDecoder<StructArrayDecoder<P>> next() {
    if (current != null) {
      current.end();
    }

    if (arrayReadBuffer.limitReached()) {
      throw new NoSuchElementException();
    } else {
      return current = new StructDecoder<StructArrayDecoder<P>>(field, arrayReadBuffer, this);
    }
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }
}
