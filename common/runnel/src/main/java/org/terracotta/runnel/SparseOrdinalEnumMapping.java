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
 * Mapping for Java enums whose int ids are too sparsely distributed to array-index: the
 * enum-to-int direction is answered from an ordinal-indexed table (which does not depend on
 * the id space at all), while the int-to-enum direction falls back to a map.
 * <p>
 * Only built for Java enum mappings, where the builder guarantees every constant of the enum
 * class is registered, so the ordinal table is complete. The {@code constants} array also
 * guards the reverse lookup: the encoder API is unchecked, so a constant of an unrelated enum
 * type (or one whose ordinal falls inside this table) must still be rejected as unmapped.
 */
final class SparseOrdinalEnumMapping<E> implements EnumMapping<E> {

  private final E[] constants;
  private final int[] ordinalToInt;
  private final Map<Integer, E> integerToEnum;

  SparseOrdinalEnumMapping(E[] constants, int[] ordinalToInt, Map<Integer, E> integerToEnum) {
    this.constants = constants;
    this.ordinalToInt = ordinalToInt;
    this.integerToEnum = integerToEnum;
  }

  @Override
  public int toInt(E e) {
    if (e instanceof Enum) {
      int ordinal = ((Enum<?>) e).ordinal();
      if (ordinal < constants.length && constants[ordinal] == e) {
        return ordinalToInt[ordinal];
      }
    }
    throw new IllegalArgumentException("Unmapped enum : " + e);
  }

  @Override
  public E toEnum(int intValue) {
    return integerToEnum.get(intValue);
  }
}