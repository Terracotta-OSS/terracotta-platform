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
package org.terracotta.dynamic_config.api.service;

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

  static boolean containsSubstitutionParams(String source) {
    // Tries to find in the string some characters to be substituted
    // See org.terracotta.config.util.ParameterSubstitutor.substitute(source) for the supported variables
    return Stream.of("d", "D", "h", "c", "i", "H", "n", "o", "a", "v", "t", "(")
        .map(c -> "%" + c)
        .anyMatch(source::contains);
  }
}
