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
package org.terracotta.dynamic_config.api.model;

import java.util.Collection;

/**
 * @author Mathieu Carbou
 */
public enum Version {

  /**
   * initial version of config file, where version is not included in property file
   */
  V1("1"),

  /**
   * Second version of the config file, which adds support for:
   * lock-context,
   * stripe-name
   */
  V2("2"),

  CURRENT("2");

  private final String value;

  Version(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return getValue();
  }

  public boolean amongst(Collection<Version> versions) {
    return versions.stream().anyMatch(this::is);
  }

  public boolean is(Version version) {
    return version.value.equals(this.value);
  }

  public static Version fromValue(String value) {
    for (Version version : values()) {
      if (version.value.equals(value)) {
        return version;
      }
    }
    throw new IllegalArgumentException(value);
  }
}
