/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.config.data_roots;

import com.tc.classloader.CommonComponent;

import java.io.Closeable;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

/**
 * DataDirectories
 */
@CommonComponent
public interface DataDirectories extends Closeable {

  /**
   * Returns data directory for given {@code name} which is specified in the server's configuration
   *
   * @param name Unique name to look up the path
   * @return corresponding data directory
   * @throws NullPointerException if the given {@code name} is {@code null}
   * @throws IllegalArgumentException if the given {@code name} is not configured in the server's configuration
   */
  Path getDataDirectory(String name);

  /**
   * Returns the optional data directory to be used by platform
   *
   * @return the platform data directory
   */
  Optional<String> getPlatformDataDirectoryIdentifier();

  /**
   * Returns all configured data directories names in server's configuration
   *
   * @return A set of data directory names
   */
  Set<String> getDataDirectoryNames();

}
