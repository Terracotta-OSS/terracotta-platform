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
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.terracotta.persistence.sanskrit.change.SanskritChangeVisitor;

/**
 * Generates JSON corresponding to the data change represented by a SanskritChange.
 */
public class JsonSanskritChangeVisitor implements SanskritChangeVisitor {

  private final ObjectMapperSupplier objectMapperSupplier;
  private final ObjectNode objectNode;

  public JsonSanskritChangeVisitor(ObjectMapperSupplier objectMapperSupplier) {
    this(objectMapperSupplier, objectMapperSupplier.getObjectMapper().createObjectNode());
  }

  public JsonSanskritChangeVisitor(ObjectMapperSupplier objectMapperSupplier, ObjectNode objectNode) {
    this.objectMapperSupplier = objectMapperSupplier;
    this.objectNode = objectNode;
  }

  public String getJson(String version) throws SanskritException {
    try {
      return objectMapperSupplier.getObjectMapper(version).writeValueAsString(objectNode);
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
    value.accept(new JsonSanskritChangeVisitor(objectMapperSupplier, childObjectNode));
    objectNode.set(key, childObjectNode);
  }

  @Override
  public void removeKey(String key) {
    NullNode nullNode = objectNode.nullNode();
    objectNode.set(key, nullNode);
  }

  @Override
  public <T> void setExternal(String key, T value, String version) {
    objectNode.set(key, value instanceof JsonNode ? (JsonNode) value : objectMapperSupplier.getObjectMapper(version).valueToTree(value));
  }
}
