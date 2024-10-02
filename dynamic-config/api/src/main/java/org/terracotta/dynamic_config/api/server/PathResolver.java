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
package org.terracotta.dynamic_config.api.server;

import java.nio.file.Path;
import java.nio.file.Paths;
import static java.util.Objects.requireNonNull;
import java.util.function.Function;
import static java.util.function.Function.identity;

/**
 * @author Mathieu Carbou
 */
public class PathResolver {

  private final Path baseDir;
  private final Function<Path, Path> transform;

  private PathResolver() {
    this(Paths.get(""), identity());
  }

  public PathResolver(Path baseDir) {
    this(baseDir, identity());
  }

  public PathResolver(Path baseDir, Function<Path, Path> transform) {
    this.baseDir = requireNonNull(baseDir);
    this.transform = requireNonNull(transform);
  }

  public Path getBaseDir() {
    return baseDir;
  }

  public Path resolve(Path path) {
    if (path == null) {
      return null;
    }
    Path transformed = transform.apply(path);
    if (transformed.isAbsolute()) {
      return path; // keep original path and placeholders
    }
    return baseDir.resolve(path); // keep original path and placeholders
  }

  @Override
  public String toString() {
    return baseDir.toAbsolutePath().toString();
  }
}
