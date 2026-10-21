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
package org.terracotta.runnel;

import java.util.Map;

/**
 * @author Ludovic Orban
 */
public class EnumMapping<E> {
  private final Map<E, Integer> enumToInteger;
  private final Map<Integer, E> integerToEnum;
  // Dense int->enum lookup table, present when the mapping's int values are small enough to be
  // array-indexed cheaply; null for sparse/large id spaces.
  private final E[] denseToEnum;
  // Ordinal-indexed enum->int lookup tables, present for Java enum mappings; both null otherwise.
  // ordinalConstants guards the lookup: callers can pass a constant of an unrelated enum type
  // (the encoder API is unchecked), which must still be rejected as unmapped.
  private final E[] ordinalConstants;
  private final int[] ordinalToInt;

  EnumMapping(Map<E, Integer> enumToInteger, Map<Integer, E> integerToEnum) {
    this(enumToInteger, integerToEnum, null, null, null);
  }

  EnumMapping(Map<E, Integer> enumToInteger, Map<Integer, E> integerToEnum, E[] denseToEnum, E[] ordinalConstants, int[] ordinalToInt) {
    this.enumToInteger = enumToInteger;
    this.integerToEnum = integerToEnum;
    this.denseToEnum = denseToEnum;
    this.ordinalConstants = ordinalConstants;
    this.ordinalToInt = ordinalToInt;
  }

  public int toInt(E e) {
    if (ordinalConstants != null && e instanceof Enum) {
      int ordinal = ((Enum<?>) e).ordinal();
      if (ordinal < ordinalConstants.length && ordinalConstants[ordinal] == e) {
        return ordinalToInt[ordinal];
      }
    }
    Integer integer = enumToInteger.get(e);
    if (integer == null) {
      throw new IllegalArgumentException("Unmapped enum : " + e);
    }
    return integer;
  }

  /**
   * Maps an int value to its enum constant.
   * <p>
   * When the dense fast-path table is present, ids within its range are answered from the
   * table directly: holes (unmapped ids inside the range) are {@code null} slots, identical
   * to what the map would return for the same id, so there is deliberately no fall-through —
   * the mapping is immutable, so a hole can never become a mapped value later.
   *
   * @param intValue the int value to map
   * @return the mapped enum constant, or {@code null} if the value is unmapped
   */
  public E toEnum(int intValue) {
    if (denseToEnum != null && intValue >= 0 && intValue < denseToEnum.length) {
      return denseToEnum[intValue];
    }
    return integerToEnum.get(intValue);
  }
}
