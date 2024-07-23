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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.nio.file.Path;
import java.util.Collection;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;

/**
 * @author Mathieu Carbou
 */
public enum ConfigFormat {
  JSON(false, "json"),
  PROPERTIES(true, "properties"),
  CONFIG(true, "cfg", "conf", "config"),
  UNKNOWN(false, null);

  private final boolean exposed;
  private final String primaryExtension;
  private final String[] supportedExtensions;

  ConfigFormat(boolean exposed, String primaryExtension, String... supportedExtensions) {
    this.primaryExtension = primaryExtension;
    this.supportedExtensions = supportedExtensions;
    this.exposed = exposed && primaryExtension != null;
  }

  @Override
  public String toString() {
    return primaryExtension == null ? "<unknown>" : primaryExtension;
  }

  @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
  public static ConfigFormat from(Path file) {
    String name = (file == null || file.getFileName() == null) ? "" : file.getFileName().toString().toLowerCase();
    for (ConfigFormat format : values()) {
      if (format.primaryExtension != null) {
        // not unknown
        if (concat(Stream.of(format.primaryExtension), Stream.of(format.supportedExtensions)).anyMatch(ext -> name.endsWith("." + ext))) {
          return format;
        }
      }
    }
    return UNKNOWN;
  }

  public static Collection<String> supported() {
    return Stream.of(values())
        .filter(configFormat -> configFormat.exposed)
        .map(configFormat -> configFormat.primaryExtension)
        .sorted()
        .collect(toList());
  }
}
