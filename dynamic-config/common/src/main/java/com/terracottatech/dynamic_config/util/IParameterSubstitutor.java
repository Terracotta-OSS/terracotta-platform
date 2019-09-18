/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.util;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

@FunctionalInterface
public interface IParameterSubstitutor {
  String substitute(String source);

  static IParameterSubstitutor identity() {
    return t -> t;
  }

  static IParameterSubstitutor unsupported() {
    return source -> {
      throw new UnsupportedOperationException("Parameter substitution is not supported");
    };
  }

  default Path substitute(Path source) {
    if (source == null) return null;
    return Paths.get(substitute(source.toString()));
  }

  default boolean containsSubstitutionParams(String source) {
    // tries to find in the string some characters to be substituted
    return Stream.of("d", "D", "h", "c", "i", "H", "n", "o", "a", "v", "t", "(")
        .map(c -> "%" + c)
        .anyMatch(source::contains);
  }
}
