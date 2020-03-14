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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.persistence.sanskrit.HashUtils;

/**
 * @author Mathieu Carbou
 */
public class DefaultHashComputer implements HashComputer<NodeContext> {
  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultHashComputer.class);

  private final ObjectMapper mapper;

  public DefaultHashComputer(ObjectMapper mapper) {
    this.mapper = mapper.copy();
    this.mapper.configure(SerializationFeature.INDENT_OUTPUT, false);
  }

  @Override
  public String computeHash(NodeContext o) {
    try {
      String json = mapper.writeValueAsString(o);
      String hash = HashUtils.generateHash(json);
      LOGGER.debug("Computed hash: {} for json: {}", hash, json);
      return hash;
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }
}
