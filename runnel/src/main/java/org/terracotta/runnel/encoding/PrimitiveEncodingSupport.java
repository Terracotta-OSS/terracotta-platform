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
package org.terracotta.runnel.encoding;

import java.nio.ByteBuffer;

/**
 * Primitive types encoder interface.
 * @param <T> The actual encoder type.
 */
public interface PrimitiveEncodingSupport<T extends PrimitiveEncodingSupport> {

  /**
   * Encode a boolean.
   * @param name the field name.
   * @param value the value to encode.
   * @return this.
   */
  T bool(String name, boolean value);

  /**
   * Encode a character.
   * @param name the field name.
   * @param value the value to encode.
   * @return this.
   */
  T chr(String name, char value);

  /**
   * Encode an enumeration.
   * @param name the field name.
   * @param value the boolean value to encode.
   * @param <E> the enumration's actual type.
   * @return this.
   */
  <E> T enm(String name, E value);

  /**
   * Encode a 32-bit integer.
   * @param name the field name.
   * @param value the value to encode.
   * @return this.
   */
  T int32(String name, int value);

  /**
   * Encode a 64-bit integer.
   * @param name the field name.
   * @param value the value to encode.
   * @return this.
   */
  T int64(String name, long value);

  /**
   * Encode a 64-bit, double-precision floating point number.
   * @param name the field name.
   * @param value the value to encode.
   * @return this.
   */
  T fp64(String name, double value);

  /**
   * Encode a character string.
   * @param name the field name.
   * @param value the value to encode.
   * @return this.
   */
  T string(String name, String value);

  /**
   * Encode a byte buffer.
   * @param name the field name.
   * @param value the value to encode.
   * @return this.
   */
  T byteBuffer(String name, ByteBuffer value);

}
