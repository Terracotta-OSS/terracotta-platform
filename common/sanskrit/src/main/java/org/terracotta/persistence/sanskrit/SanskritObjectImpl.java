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
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

public class SanskritObjectImpl implements MutableSanskritObject {
  private final ObjectNode mappings;
  private final ObjectMapperSupplier objectMapperSupplier;

  public SanskritObjectImpl(ObjectMapperSupplier objectMapperSupplier) {
    this(objectMapperSupplier, objectMapperSupplier.getObjectMapper().createObjectNode());
  }

  SanskritObjectImpl(ObjectMapperSupplier objectMapperSupplier, ObjectNode node) {
    this.objectMapperSupplier = objectMapperSupplier;
    this.mappings = node;
  }

  @Override
  public void setString(String key, String value) {
    mappings.put(key, value);
  }

  @Override
  public void setLong(String key, long value) {
    mappings.put(key, value);
  }

  @Override
  public void setObject(String key, SanskritObject object) {
    mappings.set(key, CopyUtils.makeCopy(objectMapperSupplier, object).mappings);
  }

  @Override
  public <T> void setExternal(String key, T o, String version) {
    if (o instanceof SanskritObject) {
      setObject(key, (SanskritObject) o);
    } else {
      mappings.set(key, o instanceof JsonNode ? (JsonNode) o : objectMapperSupplier.getObjectMapper(version).valueToTree(o));
    }
  }

  @Override
  public void accept(SanskritVisitor visitor) {
    Iterator<Map.Entry<String, JsonNode>> fields = mappings.fields();
    while (fields.hasNext()) {
      Map.Entry<String, JsonNode> entry = fields.next();
      String key = entry.getKey();
      JsonNode value = entry.getValue();
      if (value.isTextual()) {
        visitor.setString(key, value.textValue());
      } else if (value.isLong()) {
        visitor.setLong(key, value.longValue());
      } else if (value.isObject()) {
        visitor.setObject(key, new SanskritObjectImpl(objectMapperSupplier, (ObjectNode) value));
      } else {
        // value IS always a JsonNode, so we do not care about the versioning for the mapping
        visitor.setExternal(key, value, null);
      }
    }
  }

  @Override
  public <T> T getObject(String key, Class<T> type, String version) {
    JsonNode jsonNode = mappings.get(key);
    if (jsonNode == null) {
      return null;
    }
    if (type.isInstance(jsonNode)) {
      return type.cast(jsonNode);
    }
    try {
      return type.cast(objectMapperSupplier.getObjectMapper(version).treeToValue(jsonNode, type));
    } catch (JsonProcessingException e) {
      // should never happen because the json in the append log
      // has already been serialized by sanskrit and cannot be updated by a user
      throw new IllegalStateException(e);
    }
  }

  @Override
  public String getString(String key) {
    return Optional.ofNullable(mappings.get(key))
        .map(TextNode.class::cast)
        .map(TextNode::textValue)
        .orElse(null);
  }

  @Override
  public Long getLong(String key) {
    return Optional.ofNullable(mappings.get(key))
        .map(NumericNode.class::cast)
        .map(NumericNode::longValue)
        .orElse(null);
  }

  @Override
  public SanskritObject getObject(String key) {
    return Optional.ofNullable(mappings.get(key))
        .map(ObjectNode.class::cast)
        .map(node -> new SanskritObjectImpl(objectMapperSupplier, node))
        .orElse(null);
  }

  @Override
  public void removeKey(String key) {
    mappings.remove(key);
  }
}
