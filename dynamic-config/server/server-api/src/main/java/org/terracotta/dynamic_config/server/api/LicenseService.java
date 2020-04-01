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
package org.terracotta.dynamic_config.server.api;

import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.License;

import java.nio.file.Path;

/**
 * @author Mathieu Carbou
 */
public interface LicenseService {

  /**
   * Parses a license. Must return a license, or throw.
   */
  License parse(Path file) throws InvalidLicenseException;

  /**
   * Validate the license against a cluster and throws if not valid
   */
  void validate(License license, Cluster cluster) throws InvalidLicenseException;

  /**
   * Validate a license file against a cluster and throws if not valid
   */
  default void validate(Path file, Cluster cluster) throws InvalidLicenseException {
    validate(parse(file), cluster);
  }

  static LicenseService unsupported() {
    return new LicenseService() {
      @Override
      public License parse(Path file) {
        throw new UnsupportedOperationException("Licensing is not supported");
      }

      @Override
      public void validate(License license, Cluster cluster) throws InvalidLicenseException {
        throw new UnsupportedOperationException("Licensing is not supported");
      }
    };
  }
}
