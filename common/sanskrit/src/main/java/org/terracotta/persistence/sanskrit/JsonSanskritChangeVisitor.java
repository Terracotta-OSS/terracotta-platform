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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.terracotta.persistence.sanskrit.change.SanskritChangeVisitor;

/**
 * Generates JSON corresponding to the data change represented by a SanskritChange.
 */
public class JsonSanskritChangeVisitor implements SanskritChangeVisitor {

  private final ObjectMapper objectMapper;
  private final ObjectNode objectNode;

  public JsonSanskritChangeVisitor(ObjectMapper objectMapper) {
    this(objectMapper, objectMapper.createObjectNode());
  }

  public JsonSanskritChangeVisitor(ObjectMapper objectMapper, ObjectNode objectNode) {
    this.objectMapper = objectMapper;
    this.objectNode = objectNode;
  }

  public String getJson() throws SanskritException {
    try {
      return objectMapper.writeValueAsString(objectNode);
    } catch (JsonProcessingException e) {
      throw new SanskritException(e);
    }
  }

  @Override
  public void setString(String key, String value) {
    objectNode.put(key, value);
  }

  @Override
  public void setLong(String key, long value) {
    objectNode.put(key, value);
  }

  @Override
  public void setObject(String key, SanskritObject value) {
    ObjectNode childObjectNode = objectNode.objectNode();
    value.accept(new JsonSanskritChangeVisitor(objectMapper, childObjectNode));
    objectNode.set(key, childObjectNode);
  }

  @Override
  public void removeKey(String key) {
    NullNode nullNode = objectNode.nullNode();
    objectNode.set(key, nullNode);
  }

  @Override
  public <T> void setExternal(String key, T value) {
    objectNode.set(key, value instanceof JsonNode ? (JsonNode) value : objectMapper.valueToTree(value));
  }
}
