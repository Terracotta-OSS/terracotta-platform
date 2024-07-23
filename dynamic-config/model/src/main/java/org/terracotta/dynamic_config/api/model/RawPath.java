/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package org.terracotta.dynamic_config.api.model;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * A raw path is a {@link java.nio.file.Path} that is keeping the user input as-is
 * and only parses it when used.
 * <p>
 * Dynamic Config needs to keep all user inputs.
 * <p>
 * The problems with Path are:
 * <p>
 * 1. Parsing: Paths.get() will parse some user input into some segments.
 * The input and parsing is per-platform, so it can lead to
 * different outcomes in both parsing and toString()
 * <p>
 * - On Win: Paths.get("a/b") and Paths.get("a\\b") give 2 segments
 * <p>
 * - On Lin: Paths.get("a/b") give 2 segments and Paths.get("a\\b") gives 1 segment
 * <p>
 * 2. User input is lost: when parsed, we loose the path separator that was initially used.
 * As a consequence, all serializations could lead to different results that is different
 * from the user input since it depends on the system where we are calling toString().
 * <p>
 * So this class aims at resolving all of that, by keeping the user input,
 * that will be used for equality checks, serialization and deserialization,
 * and also provide the equivalent of the Path API when used.
 * <p>
 * The parsing of the user input will be only done when the object will be queried and
 * converted to a Path
 *
 * @author Mathieu Carbou
 */
public class RawPath {

  private final String value;
  private volatile transient Path cache;

  public static RawPath valueOf(String value) {
    return new RawPath(value);
  }

  private RawPath(String value) {
    this.value = requireNonNull(value);
  }

  /**
   * @return the user configuration
   */
  public String getValue() {
    return value;
  }

  public Path toPath() {
    if (cache == null) {
      // note: this is completely OK if several threads are creating the same value.
      // Method does not need synchronization.
      cache = Paths.get(value);
    }
    return cache;
  }

  @Override
  public String toString() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof RawPath)) return false;
    RawPath paths = (RawPath) o;
    return getValue().equals(paths.getValue());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getValue());
  }

  /**
   * Builds a new raw path where the parameter is appended to the current value.
   * <p>
   * This is up to the user to correctly set the path separator.
   * <p>
   * Example: {@code RawPath.valueOf("foo").append("/bar")}
   */
  public RawPath append(String segments) {
    return RawPath.valueOf(value + segments);
  }
}
