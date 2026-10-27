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

/**
 * Maps enum constants to int values and back.
 * <p>
 * Instances are created by {@link EnumMappingBuilder#build()}, which picks one of four
 * package-private implementations based on whether the mapped type is a Java enum and whether
 * the int id space is dense enough to be array-indexed, so the hot paths avoid hash lookups
 * (and the boxing that goes with them) where possible.
 */
public interface EnumMapping<E> {

  /**
   * Maps an enum constant to its int value.
   *
   * @param e the enum constant
   * @return the int value
   * @throws IllegalArgumentException if the constant is not mapped (including {@code null})
   */
  int toInt(E e);

  /**
   * Maps an int value to its enum constant.
   *
   * @param intValue the int value to map
   * @return the mapped enum constant, or {@code null} if the value is unmapped
   */
  E toEnum(int intValue);
}