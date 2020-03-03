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
package org.terracotta.dynamic_config.entity.common;

import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.json.Json;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author Mathieu Carbou
 */
public class DynamicTopologyMessageCodec implements MessageCodec<DynamicTopologyEntityMessage, DynamicTopologyEntityMessage> {
  @Override
  public byte[] encodeMessage(DynamicTopologyEntityMessage message) throws MessageCodecException {
    try {
      return Json.toJson(message).getBytes(UTF_8);
    } catch (RuntimeException e) {
      throw new MessageCodecException(e.getMessage(), e);
    }
  }

  @Override
  public DynamicTopologyEntityMessage decodeMessage(byte[] payload) throws MessageCodecException {
    try {
      return Json.parse(new String(payload, UTF_8), DynamicTopologyEntityMessage.class);
    } catch (RuntimeException e) {
      throw new MessageCodecException(e.getMessage(), e);
    }
  }

  @Override
  public byte[] encodeResponse(DynamicTopologyEntityMessage response) throws MessageCodecException {
    try {
      return Json.toJson(response).getBytes(UTF_8);
    } catch (RuntimeException e) {
      throw new MessageCodecException(e.getMessage(), e);
    }
  }

  @Override
  public DynamicTopologyEntityMessage decodeResponse(byte[] payload) throws MessageCodecException {
    try {
      return Json.parse(new String(payload, UTF_8), DynamicTopologyEntityMessage.class);
    } catch (RuntimeException e) {
      throw new MessageCodecException(e.getMessage(), e);
    }
  }
}
