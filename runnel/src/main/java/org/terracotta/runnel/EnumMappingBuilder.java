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

  @SuppressWarnings({"SuspiciousMethodCalls", "unchecked", "rawtypes"})
  public EnumMapping<E> build() {
    if (Enum.class.isAssignableFrom(enumClass)) {
      HashSet<Enum> unregisteredEnums = new HashSet<Enum>(EnumSet.allOf((Class)enumClass));
      unregisteredEnums.removeAll(enumToInteger.keySet());
      if (!unregisteredEnums.isEmpty()) {
        throw new IllegalStateException("Missing enum mappings for : " + unregisteredEnums);
      }
    }
    return new EnumMapping<>(enumToInteger, integerToEnum);
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
