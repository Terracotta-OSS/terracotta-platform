/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.diagnostic.common;

import static java.util.Objects.requireNonNull;

/**
 * The DiagnosticsHandler is poorly written and incorrectly parses argument lines, especially for JMX method arguments.
 * <p>
 * The parsing fails in case the argument is an empty string for example.
 * <p>
 * So this codec can wrap other codec to ensure no empty string is passed to a JMX call and consequently bring support for
 * empty string parameters.
 *
 * @author Mathieu Carbou
 */
public class EmptyParameterDiagnosticCodec extends DiagnosticCodecSkeleton<String> {

  static final String EOF = "EOF";

  public EmptyParameterDiagnosticCodec() {
    super(String.class);
  }

  @Override
  public String serialize(Object o) throws DiagnosticCodecException {
    requireNonNull(o);
    return o.toString() + EOF;
  }

  @Override
  public <T> T deserialize(String encoded, Class<T> target) throws DiagnosticCodecException {
    requireNonNull(encoded);
    requireNonNull(target);
    if (!target.isAssignableFrom(String.class)) {
      throw new IllegalArgumentException("Target type must be assignable from String");
    }
    if (!encoded.endsWith(EOF)) {
      throw new DiagnosticCodecException("Unsupported encoded input");
    }
    return target.cast(encoded.substring(0, encoded.length() - 3));
  }

  @Override
  public String toString() {
    return "Empty";
  }
}
