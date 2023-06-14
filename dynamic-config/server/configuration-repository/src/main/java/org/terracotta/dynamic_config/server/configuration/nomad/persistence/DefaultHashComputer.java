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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.model.Version;
import org.terracotta.dynamic_config.api.service.Props;
import org.terracotta.nomad.server.NomadException;
import org.terracotta.persistence.sanskrit.HashUtils;

/**
 * @author Mathieu Carbou
 */
public class DefaultHashComputer implements HashComputer {
  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultHashComputer.class);

  @Override
  public String computeHash(Config config) {
    String output = Props.toString(config.getTopology().getCluster().toProperties(false, false, true, config.getVersion()));
    String hash = HashUtils.generateHash(output);
    LOGGER.trace("Computed hash: {} for config:\n{}", hash, output);
    return hash;
  }

  @Override
  public void checkHash(Config config, String expectedHash) throws NomadException {
    if (config.getVersion() == Version.V1) {
      // we cannot check the hash of a V1 config because the V1 hash was computed based on the a json output
      // that was not controlled (field ordering, etc) and non deterministic.
      return;
    }

    String actualHash = computeHash(config);
    if (!actualHash.equals(expectedHash)) {
      throw new NomadException("Computed: " + actualHash + ". Expected: " + expectedHash + ". Configuration:\n" + config);
    }
  }
}
