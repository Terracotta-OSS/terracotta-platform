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
package org.terracotta.dynamic_config.server.configuration.nomad.persistence;

import org.terracotta.nomad.server.NomadException;

/**
 * @author Mathieu Carbou
 */
@FunctionalInterface
public interface HashComputer {
  String computeHash(Config config);

  default void checkHash(Config config, String expectedHash) throws NomadException {
    String actualHash = computeHash(config);
    if (!actualHash.equals(expectedHash)) {
      throw new NomadException("Computed: " + actualHash + ". Expected: " + expectedHash + ". Configuration: " + config);
    }
  }
}
