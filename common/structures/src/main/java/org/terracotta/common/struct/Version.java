/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
package org.terracotta.common.struct;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * Inspired by Version from Angela.
 * <p>
 * Parses a version in the format:
 * <ul>
 *   <li>a[.b.c.d...]</li>
 *   <li>a[.b.c.d...].suffix</li>
 *   <li>a[.b.c.d...]-suffix</li>
 * </ul>
 */
public final class Version implements Comparable<Version> {

  private final List<Integer> parts;
  private final String suffix;
  private final boolean snapshot;

  private Version(List<Integer> parts, String suffix, boolean snapshot) {
    this.parts = requireNonNull(parts);
    this.suffix = suffix;
    this.snapshot = snapshot;
  }

  public boolean isSnapshot() {
    return snapshot;
  }

  public boolean lowerThan(Version other) {
    return compareTo(other) < 0;
  }

  public boolean greaterThan(Version other) {
    return compareTo(other) > 0;
  }

  @Override
  public String toString() {
    String v = parts.stream().map(Objects::toString).collect(Collectors.joining("."));
    if (suffix != null) {
      v += suffix;
    }
    if (snapshot) {
      v += "-SNAPSHOT";
    }
    return v;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Version version = (Version) o;
    return snapshot == version.snapshot && parts.equals(version.parts) && Objects.equals(suffix, version.suffix);
  }

  @Override
  public int hashCode() {
    return Objects.hash(parts, suffix, snapshot);
  }

  @Override
  public int compareTo(Version other) {
    if (equals(other)) {
      return 0;
    }

    for (int i = 0, max = Math.min(parts.size(), other.parts.size()); i < max; i++) {
      if (parts.get(i) > other.parts.get(i)) {
        return 1;
      } else if (parts.get(i) < other.parts.get(i)) {
        return -1;
      }
    }

    if (parts.size() > other.parts.size()) {
      // this: 4.0.1 vs other: 4.0
      return 1;
    }

    if (parts.size() < other.parts.size()) {
      // this: 4.0 vs other: 4.0.1
      return -1;
    }

    // same base version (parts.size() == other.parts.size())

    if (suffix != null && other.suffix == null) {
      // 4.0.foo vs 4.0
      return 1;
    }

    if (suffix == null && other.suffix != null) {
      // 4.0 vs 4.0.foo
      return -1;
    }

    if (suffix != null) {
      final int c = suffix.compareTo(other.suffix);
      if (c != 0) {
        // 4.0.bar vs 4.0.foo
        return c;
      }
    }

    // same base version and same suffix (null or same value)

    if (snapshot && other.snapshot) {
      // 4.0.bar-SNAPSHOT vs 4.0.bar-SNAPSHOT
      // this should never be called since handled at the
      // start of the method with the equal check
      return 0;
    }
    if (snapshot) {
      // 4.0.bar-SNAPSHOT vs 4.0.bar
      return -1;
    }
    // 4.0.bar vs 4.0.bar-SNAPSHOT
    return 1;
  }

  public static Version valueOf(String version) {
    requireNonNull(version);

    List<Integer> parts = new ArrayList<>(5);
    String suffix = null;
    boolean snapshot = false;

    // snapshot detection
    if (version.endsWith("-SNAPSHOT")) {
      snapshot = true;
      version = version.substring(0, version.length() - 9);
    }

    // version == 1.0 or 1.0.bar or 1.0-bar

    int since = 0;
    while (true) {
      int last = version.indexOf('.', since);
      if (last > 0) {
        // we found a part and there are some other after
        try {
          parts.add(Integer.parseInt(version.substring(since, last)));
        } catch (NumberFormatException e) {
          throw new IllegalArgumentException(version, e);
        }
        since = last + 1;
      } else {
        // this is the end
        try {
          // last part is a number ?
          parts.add(Integer.parseInt(version.substring(since)));
        } catch (NumberFormatException ignored) {
          // if not, there is a suffix ?
          int p = version.indexOf('-', since);
          if (p == -1) {
            // 1.2.foo
            suffix = "." + version.substring(since);
          } else {
            // 1.0-bar
            suffix = version.substring(p);
            try {
              parts.add(Integer.parseInt(version.substring(since, p)));
            } catch (NumberFormatException e) {
              throw new IllegalArgumentException(version, e);
            }
          }
        }
        break;
      }
    }
    if (suffix != null && !suffix.matches("[-\\w.]+")) {
      throw new IllegalArgumentException(version);
    }
    if (parts.isEmpty()) {
      throw new IllegalArgumentException(version);
    }
    return new Version(parts, suffix, snapshot);
  }
}
