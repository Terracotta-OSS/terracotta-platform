/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2026
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

import java.util.Map;

/**
 * Mapping for non-Java-enum ("pseudo-enum") mapped types whose int ids are densely packed:
 * the enum-to-int direction keeps a map (there are no ordinals to index by), while the
 * int-to-enum direction is answered from an array. Unmapped ids inside the table's range are
 * {@code null} slots - identical to what a map would return for the same id - and ids outside
 * the range are unmapped by construction.
 */
final class DenseEnumMapping<E> implements EnumMapping<E> {

  private final Map<E, Integer> enumToInteger;
  private final E[] denseToEnum;

  DenseEnumMapping(Map<E, Integer> enumToInteger, E[] denseToEnum) {
    this.enumToInteger = enumToInteger;
    this.denseToEnum = denseToEnum;
  }

  @Override
  public int toInt(E e) {
    Integer integer = enumToInteger.get(e);
    if (integer == null) {
      throw new IllegalArgumentException("Unmapped enum : " + e);
    }
    return integer;
  }

  @Override
  public E toEnum(int intValue) {
    if (intValue >= 0 && intValue < denseToEnum.length) {
      return denseToEnum[intValue];
    }
    return null;
  }
}