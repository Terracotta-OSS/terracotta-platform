/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.api.service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;

/**
 * @author Mathieu Carbou
 */
public class PathResolver {

  public static PathResolver NOOP = new PathResolver();

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
