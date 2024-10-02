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
public interface DataDirs extends Closeable {

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
