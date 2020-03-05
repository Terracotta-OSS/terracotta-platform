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
package org.terracotta.diagnostic.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.terracotta.json.Json;

import java.io.IOException;

import static java.util.Objects.requireNonNull;

/**
 * @author Mathieu Carbou
 */
public class JsonDiagnosticCodec extends DiagnosticCodecSkeleton<String> {

  private final ObjectMapper objectMapper;

  public JsonDiagnosticCodec() {
    this(false);
  }

  public JsonDiagnosticCodec(boolean pretty) {
    this(Json.copyObjectMapper(pretty));
  }

  public JsonDiagnosticCodec(ObjectMapper objectMapper) {
    super(String.class);
    this.objectMapper = requireNonNull(objectMapper);
  }

  @Override
  public String serialize(Object o) throws DiagnosticCodecException {
    requireNonNull(o);
    try {
      return objectMapper.writeValueAsString(o);
    } catch (JsonProcessingException | RuntimeException e) {
      throw new DiagnosticCodecException(e);
    }
  }

  @Override
  public <T> T deserialize(String json, Class<T> target) throws DiagnosticCodecException {
    requireNonNull(json);
    requireNonNull(target);
    try {
      return JsonNode.class.isAssignableFrom(target) ?
          target.cast(objectMapper.readTree(json)) :
          objectMapper.readValue(json, target);
    } catch (IOException | RuntimeException e) {
      throw new DiagnosticCodecException(e);
    }
  }

  @Override
  public String toString() {
    return "Json";
  }
}
