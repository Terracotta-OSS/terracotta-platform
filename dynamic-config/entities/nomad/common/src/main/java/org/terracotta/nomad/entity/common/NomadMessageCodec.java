/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2026
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
package org.terracotta.nomad.entity.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.json.DynamicConfigJsonModule;
import org.terracotta.entity.MessageCodec;
import org.terracotta.json.DefaultJsonFactory;
import org.terracotta.json.Json;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author Mathieu Carbou
 */
public class NomadMessageCodec implements MessageCodec<NomadEntityMessage, NomadEntityResponse> {
  private static final Logger LOGGER = LoggerFactory.getLogger(NomadMessageCodec.class);
  private final Json json = new DefaultJsonFactory().withModule(new DynamicConfigJsonModule()).create();

  @Override
  public byte[] encodeMessage(NomadEntityMessage message) {
    final String json = this.json.toString(message);
    LOGGER.trace("encodeMessage({}): {}", message, json);
    return json.getBytes(UTF_8);
  }

  @Override
  public NomadEntityMessage decodeMessage(byte[] payload) {
    final String json = new String(payload, UTF_8);
    final NomadEntityMessage parsed = this.json.parse(json, NomadEntityMessage.class);
    LOGGER.trace("decodeMessage({}): {}", json, parsed);
    return parsed;
  }

  @Override
  public byte[] encodeResponse(NomadEntityResponse response) {
    final String json = this.json.toString(response);
    LOGGER.trace("encodeResponse({}): {}", response, json);
    return json.getBytes(UTF_8);
  }

  @Override
  public NomadEntityResponse decodeResponse(byte[] payload) {
    final String json = new String(payload, UTF_8);
    final NomadEntityResponse parsed = this.json.parse(json, NomadEntityResponse.class);
    LOGGER.trace("decodeResponse({}): {}", json, parsed);
    return parsed;
  }
}
