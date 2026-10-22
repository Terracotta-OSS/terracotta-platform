/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2026
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

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * @author Ludovic Orban
 */
public class EnumMappingBuilder<E> {

  private final Map<E, Integer> enumToInteger;
  private final Map<Integer, E> integerToEnum = new HashMap<>();
  private final Class<E> enumClass;

  @SuppressWarnings({"unchecked", "rawtypes"})
  private EnumMappingBuilder(Class<E> enumClass) {
    this.enumClass = enumClass;
    if (Enum.class.isAssignableFrom(enumClass)) {
      this.enumToInteger = new EnumMap(enumClass);
    } else {
      this.enumToInteger = new HashMap<>();
    }
  }

  public static <E> EnumMappingBuilder<E> newEnumMappingBuilder(Class<E> enumClass) {
    return new EnumMappingBuilder<>(enumClass);
  }

  /** Hard cap on the int value range for which the dense int->enum table is built. */
  private static final int MAX_DENSE_ID = 4096;
  /**
   * Footprint floor for the dense table: small id spaces (e.g. ids 10, 20, 30) always get the
   * fast path even though they are not fully populated.
   */
  private static final int MIN_DENSE_TABLE_SIZE = 1024;

  @SuppressWarnings({"SuspiciousMethodCalls", "unchecked", "rawtypes"})
  public EnumMapping<E> build() {
    if (Enum.class.isAssignableFrom(enumClass)) {
      HashSet<Enum> unregisteredEnums = new HashSet<Enum>(EnumSet.allOf((Class)enumClass));
      unregisteredEnums.removeAll(enumToInteger.keySet());
      if (!unregisteredEnums.isEmpty()) {
        throw new IllegalStateException("Missing enum mappings for : " + unregisteredEnums);
      }
    }

    /*
     * Dense fast paths: when the mapped int values are small non-negative ints (the common
     * case, since the builder rejects duplicates and negatives), an int->enum array lookup
     * replaces a HashMap hit (and its Integer boxing) on the decode side. For Java enums, an
     * ordinal-indexed int[] also replaces the forward map hit on the encode side. The dense
     * table is only built when its footprint stays proportional to the entry count, so a
     * sparse-but-small id space (e.g. ids 0 and 4096 for a two-value enum) stays map-backed
     * instead of pinning a 4097-slot array for the mapping's lifetime.
     */
    E[] denseToEnum = null;
    if (!integerToEnum.isEmpty()) {
      int max = integerToEnum.keySet().stream().mapToInt(Integer::intValue).max().getAsInt();
      int count = integerToEnum.size();
      if (max <= MAX_DENSE_ID && max + 1 <= Math.max(MIN_DENSE_TABLE_SIZE, count * 8)) {
        denseToEnum = (E[]) java.lang.reflect.Array.newInstance(enumClass, max + 1);
        for (Map.Entry<Integer, E> entry : integerToEnum.entrySet()) {
          denseToEnum[entry.getKey()] = entry.getValue();
        }
      }
    }
    // The enum->int table is indexed by ordinal, so it does not depend on the id space density.
    E[] ordinalConstants = null;
    int[] ordinalToInt = null;
    if (Enum.class.isAssignableFrom(enumClass)) {
      ordinalConstants = enumClass.getEnumConstants();
      ordinalToInt = new int[ordinalConstants.length];
      for (E e : ordinalConstants) {
        Integer mapped = enumToInteger.get(e);
        if (mapped == null) {
          // unreachable: the unregistered-enums check above has already rejected this mapping
          throw new IllegalStateException("No mapping for enum constant : " + e);
        }
        ordinalToInt[((Enum<?>) e).ordinal()] = mapped;
      }
    }
    return new EnumMapping<>(enumToInteger, integerToEnum, denseToEnum, ordinalConstants, ordinalToInt);
  }

  public EnumMappingBuilder<E> mapping(E e, int value) {
    if (enumToInteger.containsKey(e)) {
      throw new IllegalArgumentException("Duplicate enum value : " + e);
    }
    if (value < 0) {
      throw new IllegalArgumentException("Int value must be >= 0, got : " + value);
    }
    if (integerToEnum.containsKey(value)) {
      throw new IllegalArgumentException("Duplicate int value : " + value);
    }
    enumToInteger.put(e, value);
    integerToEnum.put(value, e);
    return this;
  }

}
