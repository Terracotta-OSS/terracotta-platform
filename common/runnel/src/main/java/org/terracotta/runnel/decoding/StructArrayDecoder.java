/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;

/**
 * @author Ludovic Orban
 */
public class StructArrayDecoder<P> implements Iterator<StructDecoder<StructArrayDecoder<P>>> {
  private final P parent;
  private final ReadBuffer arrayReadBuffer;
  private final int arrayLength;
  private final StructField field;

  private StructDecoder<StructArrayDecoder<P>> current = null;
  private StructDecoder<StructArrayDecoder<P>> reusable = null;

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
      return current = new StructDecoder<>(field, arrayReadBuffer, this);
    }
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }

  /**
   * Decodes all remaining elements of this array using the given decoder function, reusing a
   * single {@link StructDecoder} instance across elements to avoid one decoder allocation per
   * element. The decoder instance passed to the function is only valid for the duration of the
   * call and <b>must not be retained</b> by the function — its content is overwritten on the
   * next element. As with {@link #next()}, the function reads each element's fields in
   * ascending index order (out-of-order or repeated requests throw, exactly like
   * iterator-based decoding); any unread remainder of an element is skipped automatically.
   * <p>
   * This method must not be mixed with {@link #next()} iteration over the same array. As with
   * iterator-based consumption, call {@link #end()} before reading further fields of the
   * parent struct: {@code decodeAll} consumes the elements but does not skip the array field
   * itself.
   *
   * @param elementDecoder decodes one element and returns its result
   * @param <T> the element result type
   * @return the list of decoded elements, in array order
   */
  public <T> List<T> decodeAll(Function<StructDecoder<StructArrayDecoder<P>>, T> elementDecoder) {
    if (current != null) {
      current.end();
      current = null;
    }
    List<T> results = new ArrayList<>(Math.min(Math.max(arrayLength, 0), 4096));
    while (hasNext()) {
      if (reusable == null) {
        reusable = new StructDecoder<>(field, arrayReadBuffer, this);
      } else {
        reusable.reinit(arrayReadBuffer, this);
      }
      results.add(elementDecoder.apply(reusable));
      reusable.end();
    }
    return results;
  }
}
