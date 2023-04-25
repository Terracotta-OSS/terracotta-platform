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

  private final Json json = new DefaultJsonFactory().withModule(new NomadEntityJsonModule()).create();

  @Override
  public byte[] encodeMessage(NomadEntityMessage message) throws MessageCodecException {
    try {
      return json.toString(message).getBytes(UTF_8);
    } catch (RuntimeException e) {
      throw new MessageCodecException(e.getMessage(), e);
    }
  }

  @Override
  public NomadEntityMessage decodeMessage(byte[] payload) throws MessageCodecException {
    try {
      return json.parse(new String(payload, UTF_8), NomadEntityMessage.class);
    } catch (RuntimeException e) {
      throw new MessageCodecException(e.getMessage(), e);
    }
  }

  @Override
  public byte[] encodeResponse(NomadEntityResponse response) throws MessageCodecException {
    try {
      return json.toString(response).getBytes(UTF_8);
    } catch (RuntimeException e) {
      throw new MessageCodecException(e.getMessage(), e);
    }
  }

  @Override
  public NomadEntityResponse decodeResponse(byte[] payload) throws MessageCodecException {
    try {
      return json.parse(new String(payload, UTF_8), NomadEntityResponse.class);
    } catch (RuntimeException e) {
      throw new MessageCodecException(e.getMessage(), e);
    }
  }
}
