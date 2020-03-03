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
