/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.persistence.sanskrit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.terracottatech.utilities.Json;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

public class SanskritObjectImpl implements MutableSanskritObject {
  private final ObjectNode mappings;
  private final ObjectMapper objectMapper;

  SanskritObjectImpl() {
    this(Json.copyObjectMapper());
  }

  public SanskritObjectImpl(ObjectMapper objectMapper) {
    this(objectMapper, objectMapper.createObjectNode());
  }

  SanskritObjectImpl(ObjectMapper objectMapper, ObjectNode node) {
    this.objectMapper = objectMapper;
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
    mappings.set(key, CopyUtils.makeCopy(objectMapper, object).mappings);
  }

  @Override
  public <T> void setExternal(String key, T o) {
    if (o instanceof SanskritObject) {
      setObject(key, (SanskritObject) o);
    } else {
      mappings.set(key, o instanceof JsonNode ? (JsonNode) o : objectMapper.valueToTree(o));
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
        visitor.setObject(key, new SanskritObjectImpl(objectMapper, (ObjectNode) value));
      } else {
        visitor.setExternal(key, value);
      }
    }
  }

  @Override
  public <T> T getExternal(String key, Class<T> type) {
    JsonNode jsonNode = mappings.get(key);
    if (jsonNode == null) {
      return null;
    }
    if (type.isInstance(jsonNode)) {
      return type.cast(jsonNode);
    }
    try {
      return type.cast(objectMapper.treeToValue(jsonNode, type));
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
        .map(node -> new SanskritObjectImpl(objectMapper, node))
        .orElse(null);
  }

  @Override
  public void removeKey(String key) {
    mappings.remove(key);
  }
}
