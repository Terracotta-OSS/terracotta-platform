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
package org.terracotta.runnel.decoding;

import java.nio.ByteBuffer;

/**
 * Primitive types decoder interface.
 */
public interface PrimitiveDecodingSupport {

  /**
   * Decode a boolean.
   * @param name the field name.
   * @return the decoded value, or null if it was absent from the data.
   */
  Boolean bool(String name);

  /**
   * Decode a character.
   * @param name the field name.
   * @return the decoded value, or null if it was absent from the data.
   */
  Character chr(String name);

  /**
   * Decode a 32-bit integer.
   * @param name the field name.
   * @return the decoded value, or null if it was absent from the data.
   */
  Integer int32(String name);

  /**
   * Decode an enumeration.
   * @param name the field name.
   * @param <E> the enumration's actual type.
   * @return the decoded enumeration representation which can never be null.
   */
  <E> Enm<E> enm(String name);

  /**
   * Decode a 64-bit integer.
   * @param name the field name.
   * @return the decoded value, or null if it was absent from the data.
   */
  Long int64(String name);

  /**
   * Decode a 64-bit, double-precision floating point number.
   * @param name the field name.
   * @return the decoded value, or null if it was absent from the data.
   */
  Double fp64(String name);

  /**
   * Decode a character string.
   * @param name the field name.
   * @return the decoded value, or null if it was absent from the data.
   */
  String string(String name);

  /**
   * Decode a byte buffer.
   * @param name the field name.
   * @return the decoded value, or null if it was absent from the data.
   */
  ByteBuffer byteBuffer(String name);

}
