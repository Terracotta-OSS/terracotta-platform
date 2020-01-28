/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.diagnostic.common;

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
