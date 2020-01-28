/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.diagnostic.common;

import javax.xml.bind.DatatypeConverter;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

/**
 * This codec can be used to wrap the previously encoded request to generate a whole B64 string that won't contain any space.
 * <p>
 * Spaces in arguments are breaking the diagnostic handler.
 * <p>
 * Supports byte[] and String
 *
 * @author Mathieu Carbou
 */
public class Base64DiagnosticCodec extends DiagnosticCodecSkeleton<String> {
  public Base64DiagnosticCodec() {
    super(String.class);
  }

  @Override
  public String serialize(Object o) throws DiagnosticCodecException {
    requireNonNull(o);
    return DatatypeConverter.printBase64Binary(o instanceof byte[] ? (byte[]) o : o.toString().getBytes(UTF_8));
  }

  @Override
  public <T> T deserialize(String response, Class<T> target) throws DiagnosticCodecException {
    requireNonNull(response);
    requireNonNull(target);
    if (target.isAssignableFrom(String.class)) {
      return target.cast(new String(DatatypeConverter.parseBase64Binary(requireNonNull(response)), UTF_8));
    }
    if (target.isAssignableFrom(byte[].class)) {
      return target.cast(DatatypeConverter.parseBase64Binary(requireNonNull(response)));
    }
    throw new IllegalArgumentException("Target type must be assignable from String or byte[]");
  }

  @Override
  public String toString() {
    return "Base64";
  }
}
