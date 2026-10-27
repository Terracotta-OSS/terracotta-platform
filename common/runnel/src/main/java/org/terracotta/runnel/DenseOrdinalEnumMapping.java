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

/**
 * Mapping for Java enums whose int ids are densely packed: both directions are answered from
 * arrays - an ordinal-indexed enum-to-int table and an int-indexed enum table - with no hash
 * maps at all.
 * <p>
 * Only built for Java enum mappings, where the builder guarantees every constant of the enum
 * class is registered, so the ordinal table is complete. The {@code constants} array also
 * guards the reverse lookup: the encoder API is unchecked, so a constant of an unrelated enum
 * type (or one whose ordinal falls inside this table) must still be rejected as unmapped.
 * Unmapped ids inside the dense table's range are {@code null} slots - identical to what a map
 * would return for the same id - and ids outside the range are unmapped by construction.
 */
final class DenseOrdinalEnumMapping<E> implements EnumMapping<E> {

  private final E[] constants;
  private final int[] ordinalToInt;
  private final E[] denseToEnum;

  DenseOrdinalEnumMapping(E[] constants, int[] ordinalToInt, E[] denseToEnum) {
    this.constants = constants;
    this.ordinalToInt = ordinalToInt;
    this.denseToEnum = denseToEnum;
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
    if (intValue >= 0 && intValue < denseToEnum.length) {
      return denseToEnum[intValue];
    }
    return null;
  }
}