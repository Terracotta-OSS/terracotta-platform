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
public class EnumMappingBuilder<E extends Enum<E>> {

  private final EnumMap<E, Integer> enumToInteger;
  private final Map<Integer, E> integerToEnum = new HashMap<Integer, E>();
  private final Class<E> enumClass;

  private EnumMappingBuilder(Class<E> enumClass) {
    this.enumClass = enumClass;
    this.enumToInteger = new EnumMap<E, Integer>(enumClass);
  }

  public static <E extends Enum<E>> EnumMappingBuilder<E> newEnumMappingBuilder(Class<E> enumClass) {
    return new EnumMappingBuilder<E>(enumClass);
  }

  public EnumMapping<E> build() {
    HashSet<E> unregisteredEnums = new HashSet<E>(EnumSet.allOf(enumClass));
    unregisteredEnums.removeAll(enumToInteger.keySet());
    if (!unregisteredEnums.isEmpty()) {
      throw new IllegalStateException("Missing enum mappings for : " + unregisteredEnums);
    }
    return new EnumMapping<E>(enumToInteger, integerToEnum);
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
