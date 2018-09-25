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

import org.terracotta.runnel.utils.RunnelDecodingException;

import java.nio.ByteBuffer;
import java.util.Optional;

/**
 * Primitive types decoder interface.
 */
public interface PrimitiveDecodingSupport {

  /**
   * Decode a boolean.
   * @param name the field name.
   * @return the decoded value wrapped in an Optional, or an empty optional if the field was absent from the data.
   * @throws RunnelDecodingException if there was a problem with the data to decode.
   */
  Optional<Boolean> optionalBool(String name) throws RunnelDecodingException;


  /**
   * Decode a boolean.
   * @param name the field name.
   * @return the decoded value.
   * @throws RunnelDecodingException if there was a problem with the data to decode or if the field was absent from
   * the data.
   */
  boolean mandatoryBool(String name) throws RunnelDecodingException;

  /**
   * Decode a character.
   * @param name the field name.
   * @return the decoded value wrapped in an Optional, or an empty optional if the field was absent from the data.
   * @throws RunnelDecodingException if there was a problem with the data to decode.
   */
  Optional<Character> optionalChr(String name) throws RunnelDecodingException;

  /**
   * Decode a character.
   * @param name the field name.
   * @return the decoded value.
   * @throws RunnelDecodingException if there was a problem with the data to decode or if the field was absent from
   * the data.
   */
  char mandatoryChr(String name) throws RunnelDecodingException;

  /**
   * Decode a 32-bit integer.
   * @param name the field name.
   * @return the decoded value wrapped in an Optional, or an empty optional if the field was absent from the data.
   * @throws RunnelDecodingException if there was a problem with the data to decode.
   */
  Optional<Integer> optionalInt32(String name) throws RunnelDecodingException;

  /**
   * Decode a 32-bit integer.
   * @param name the field name.
   * @return the decoded value.
   * @throws RunnelDecodingException if there was a problem with the data to decode or if the field was absent from
   * the data.
   */
  int mandatoryInt32(String name) throws RunnelDecodingException;

  /**
   * Decode an enumeration.
   * @param name the field name.
   * @param <E> the enumration's actual type.
   * @return the decoded enumeration representation which can never be null.
   * @throws RunnelDecodingException if there was a problem with the data to decode.
   */
  <E> Enm<E> optionalEnm(String name) throws RunnelDecodingException;

  /**
   * Decode an enumeration.
   * @param name the field name.
   * @param <E> the enumration's actual type.
   * @return the decoded enumeration representation which can never be null.
   * @throws RunnelDecodingException if there was a problem with the data to decode or if the field was absent from
   * the data or if the enum value was not valid.
   */
  <E> Enm<E> mandatoryEnm(String name) throws RunnelDecodingException;

  /**
   * Decode a 64-bit integer.
   * @param name the field name.
   * @return the decoded value wrapped in an Optional, or an empty optional if the field was absent from the data.
   * @throws RunnelDecodingException if there was a problem with the data to decode.
   */
  Optional<Long> optionalInt64(String name) throws RunnelDecodingException;

  /**
   * Decode a 64-bit integer.
   * @param name the field name.
   * @return the decoded value.
   * @throws RunnelDecodingException if there was a problem with the data to decode or if the field was absent from
   * the data.
   */
  long mandatoryInt64(String name) throws RunnelDecodingException;

  /**
   * Decode a 64-bit, double-precision floating point number.
   * @param name the field name.
   * @return the decoded value wrapped in an Optional, or an empty optional if the field was absent from the data.
   * @throws RunnelDecodingException if there was a problem with the data to decode.
   */
  Optional<Double> optionalFp64(String name) throws RunnelDecodingException;

  /**
   * Decode a 64-bit, double-precision floating point number.
   * @param name the field name.
   * @return the decoded value.
   * @throws RunnelDecodingException if there was a problem with the data to decode or if the field was absent from
   * the data.
   */
  double mandatoryFp64(String name) throws RunnelDecodingException;

  /**
   * Decode a character string.
   * @param name the field name.
   * @return the decoded value wrapped in an Optional, or an empty optional if the field was absent from the data.
   * @throws RunnelDecodingException if there was a problem with the data to decode.
   */
  Optional<String> optionalString(String name) throws RunnelDecodingException;

  /**
   * Decode a character string.
   * @param name the field name.
   * @return the decoded value, which can never be null.
   * @throws RunnelDecodingException if there was a problem with the data to decode or if the field was absent from
   * the data.
   */
  String mandatoryString(String name) throws RunnelDecodingException;

  /**
   * Decode a byte buffer.
   * @param name the field name.
   * @return the decoded value wrapped in an Optional, or an empty optional if the field was absent from the data.
   * @throws RunnelDecodingException if there was a problem with the data to decode.
   */
  Optional<ByteBuffer> optionalByteBuffer(String name) throws RunnelDecodingException;

  /**
   * Decode a byte buffer.
   * @param name the field name.
   * @return the decoded value, which can never be null.
   * @throws RunnelDecodingException if there was a problem with the data to decode or if the field was absent from
   * the data.
   */
  ByteBuffer mandatoryByteBuffer(String name) throws RunnelDecodingException;

}
