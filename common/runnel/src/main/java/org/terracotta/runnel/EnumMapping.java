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

  EnumMapping(Map<E, Integer> enumToInteger, Map<Integer, E> integerToEnum) {
    this.enumToInteger = enumToInteger;
    this.integerToEnum = integerToEnum;
  }

  public int toInt(E e) {
    Integer integer = enumToInteger.get(e);
    if (integer == null) {
      throw new IllegalArgumentException("Unmapped enum : " + e);
    }
    return integer;
  }

  public E toEnum(int intValue) {
    return integerToEnum.get(intValue);
  }
}
