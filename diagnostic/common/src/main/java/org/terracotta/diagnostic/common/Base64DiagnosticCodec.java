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

import java.util.Base64;

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
    return Base64.getEncoder().encodeToString(o instanceof byte[] ? (byte[]) o : o.toString().getBytes(UTF_8));
  }

  @Override
  public <T> T deserialize(String response, Class<T> target) throws DiagnosticCodecException {
    requireNonNull(response);
    requireNonNull(target);
    if (target.isAssignableFrom(String.class)) {
      return target.cast(new String(Base64.getDecoder().decode(requireNonNull(response)), UTF_8));
    }
    if (target.isAssignableFrom(byte[].class)) {
      return target.cast(Base64.getDecoder().decode(requireNonNull(response)));
    }
    throw new IllegalArgumentException("Target type must be assignable from String or byte[]");
  }

  @Override
  public String toString() {
    return "Base64";
  }
}
