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
package org.terracotta.nomad.entity.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.json.DefaultJsonFactory;
import org.terracotta.json.Json;
import org.terracotta.nomad.entity.common.json.NomadEntityJsonModule;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author Mathieu Carbou
 */
public class NomadMessageCodec implements MessageCodec<NomadEntityMessage, NomadEntityResponse> {
  private static final Logger LOGGER = LoggerFactory.getLogger(NomadMessageCodec.class);
  private final Json json = new DefaultJsonFactory().withModule(new NomadEntityJsonModule()).create();

  @Override
  public byte[] encodeMessage(NomadEntityMessage message) throws MessageCodecException {
    try {
      final String json = this.json.toString(message);
      LOGGER.trace("encodeMessage({}): {}", message, json);
      return json.getBytes(UTF_8);
    } catch (RuntimeException e) {
      LOGGER.trace("encodeMessage({}): {}", message, e.getMessage(), e);
      throw new MessageCodecException(e.getMessage(), e);
    }
  }

  @Override
  public NomadEntityMessage decodeMessage(byte[] payload) throws MessageCodecException {
    final String json = new String(payload, UTF_8);
    try {
      final NomadEntityMessage parsed = this.json.parse(json, NomadEntityMessage.class);
      LOGGER.trace("decodeMessage({}): {}", json, parsed);
      return parsed;
    } catch (RuntimeException e) {
      LOGGER.trace("decodeMessage({}): {}", json, e.getMessage(), e);
      throw new MessageCodecException(e.getMessage(), e);
    }
  }

  @Override
  public byte[] encodeResponse(NomadEntityResponse response) throws MessageCodecException {
    try {
      final String json = this.json.toString(response);
      LOGGER.trace("encodeResponse({}): {}", response, json);
      return json.getBytes(UTF_8);
    } catch (RuntimeException e) {
      LOGGER.trace("encodeResponse({}): {}", response, e.getMessage(), e);
      throw new MessageCodecException(e.getMessage(), e);
    }
  }

  @Override
  public NomadEntityResponse decodeResponse(byte[] payload) throws MessageCodecException {
    final String json = new String(payload, UTF_8);
    try {
      final NomadEntityResponse parsed = this.json.parse(json, NomadEntityResponse.class);
      LOGGER.trace("decodeResponse({}): {}", json, parsed);
      return parsed;
    } catch (RuntimeException e) {
      LOGGER.trace("decodeResponse({}): {}", json, e.getMessage(), e);
      throw new MessageCodecException(e.getMessage(), e);
    }
  }
}
