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
package org.terracotta.persistence.sanskrit;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author Mathieu Carbou
 */
public interface ObjectMapperSupplier {

  /**
   * @return the current ObjectMapper version
   */
  default ObjectMapper getObjectMapper() {
    return getObjectMapper(null);
  }

  /**
   * @return the an ObjectMapper for a specific version. Null means the most recent version.
   */
  ObjectMapper getObjectMapper(String version);

  String getCurrentVersion();

  static ObjectMapperSupplier notVersioned(ObjectMapper objectMapper) {
    return new ObjectMapperSupplier() {
      @Override
      public ObjectMapper getObjectMapper(String version) {
        return objectMapper;
      }

      @Override
      public String getCurrentVersion() {
        return "";
      }
    };
  }

  static ObjectMapperSupplierBuilder versioned(ObjectMapper current, String version) {
    return new ObjectMapperSupplierBuilder(current, version);
  }

  class ObjectMapperSupplierBuilder implements ObjectMapperSupplier {
    private final ObjectMapper current;
    private final String version;
    private final Map<String, ObjectMapper> deprecated = new HashMap<>();

    public ObjectMapperSupplierBuilder(ObjectMapper current, String version) {
      this.current = current;
      this.version = version;
    }

    public ObjectMapperSupplierBuilder withVersions(ObjectMapper objectMapper, String... versions) {
      for (String version : versions) {
        deprecated.put(version, objectMapper);
      }
      return this;
    }

    @Override
    public ObjectMapper getObjectMapper(String version) {
      return version == null ? current : Optional.ofNullable(deprecated.get(version))
          .orElseThrow(() -> new IllegalArgumentException("Unsupported version: " + version));
    }

    @Override
    public String getCurrentVersion() {
      return version;
    }
  }
}
