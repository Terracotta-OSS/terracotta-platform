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
package org.terracotta.diagnostic.common;

import static java.util.Objects.requireNonNull;

/**
 * A code that is handling the serialization of complex objects to an encoded type to be sent over the diagnostic port
 *
 * @author Mathieu Carbou
 */
public interface DiagnosticCodec<E> {

  Class<E> getEncodedType();

  E serialize(Object o) throws DiagnosticCodecException;

  <T> T deserialize(E encoded, Class<T> target) throws DiagnosticCodecException;

  String toString();

  /**
   * Composes codecs
   *
   * @param first The first codec to apply when running serialization, before this one, and in reverse order for deserialization.
   * @return The codec composition
   */
  default <F> DiagnosticCodec<E> around(DiagnosticCodec<F> first) {
    requireNonNull(first);
    DiagnosticCodec<E> second = this;
    return new DiagnosticCodecSkeleton<E>(second.getEncodedType()) {
      @Override
      public E serialize(Object o) throws DiagnosticCodecException {
        return second.serialize(first.serialize(o));
      }

      @Override
      public <T> T deserialize(E encoded, Class<T> target) throws DiagnosticCodecException {
        F temp = second.deserialize(encoded, first.getEncodedType());
        return first.deserialize(temp, target);
      }

      @Override
      public String toString() {
        return first + " => " + second;
      }
    };
  }
}
