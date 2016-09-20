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

import java.util.HashMap;
import java.util.Map;

/**
 * @author Ludovic Orban
 */
public class EnmBuilder<E extends Enum<E>> {

  private final Map<E, Integer> enumToInteger = new HashMap<E, Integer>();
  private final Map<Integer, E> integerToEnum = new HashMap<Integer, E>();

  private EnmBuilder() {
  }

  public static <E extends Enum<E>> EnmBuilder<E> newEnumBuilder() {
    return new EnmBuilder<E>();
  }

  public Enm<E> build() {
    return new Enm<E>(enumToInteger, integerToEnum);
  }

  public EnmBuilder<E> mapping(E e, int value) {
    if (enumToInteger.containsKey(e)) {
      throw new IllegalArgumentException("Duplicate enum value : " + e);
    }
    if (integerToEnum.containsKey(value)) {
      throw new IllegalArgumentException("Duplicate int value : " + value);
    }
    enumToInteger.put(e, value);
    integerToEnum.put(value, e);
    return this;
  }

}
