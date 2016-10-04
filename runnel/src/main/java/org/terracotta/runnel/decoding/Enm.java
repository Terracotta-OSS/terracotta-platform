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

import java.util.NoSuchElementException;

/**
 * @author Ludovic Orban
 */
public class Enm<E> {
  private final boolean found;
  private final int raw;
  private final E value;

  public Enm() {
    this.found = false;
    this.raw = 0;
    this.value = null;
  }

  public Enm(int raw, E value) {
    this.found = true;
    this.raw = raw;
    this.value = value;
  }

  public E get() throws NoSuchElementException {
    if (!found) {
      throw new NoSuchElementException("Enum was not found in stream");
    }
    if (value == null) {
      throw new NoSuchElementException("Enum '" + raw + "' cannot be mapped");
    }
    return value;
  }

  public boolean isFound() {
    return found;
  }

  public boolean isValid() {
    return value != null;
  }

  public int raw() throws NoSuchElementException {
    if (!found) {
      throw new NoSuchElementException("Enum was not found in stream");
    }
    return raw;
  }
}
