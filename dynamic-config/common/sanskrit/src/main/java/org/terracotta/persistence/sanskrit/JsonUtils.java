/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.persistence.sanskrit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;

import java.io.IOException;
import java.util.Map;

public class JsonUtils {
  public static void parse(ObjectMapper objectMapper, String json, MutableSanskritObject result) throws SanskritException {
    try {
      JsonNode jsonNode = objectMapper.readTree(json);
      jsonNodeToSanskritObject(objectMapper, result, jsonNode);
    } catch (IOException e) {
      throw new SanskritException(e);
    }
  }

  private static void jsonNodeToSanskritObject(ObjectMapper objectMapper, MutableSanskritObject sanskritObject, JsonNode jsonNode) throws SanskritException {
    for (Map.Entry<String, JsonNode> field : (Iterable<Map.Entry<String, JsonNode>>) jsonNode::fields) {
      String key = field.getKey();
      JsonNode value = field.getValue();

      JsonNodeType nodeType = value.getNodeType();
      switch (nodeType) {
        case NUMBER:
          sanskritObject.setLong(key, value.longValue());
          break;
        case STRING:
          sanskritObject.setString(key, value.textValue());
          break;
        case OBJECT:
          sanskritObject.setObject(key, jsonNodeToSanskritObject(objectMapper, value));
          break;
        case NULL:
          sanskritObject.removeKey(key);
          break;
        default:
          sanskritObject.setExternal(key, value);
      }
    }
  }

  private static MutableSanskritObject jsonNodeToSanskritObject(ObjectMapper objectMapper, JsonNode jsonNode) throws SanskritException {
    SanskritObjectImpl sanskritObject = new SanskritObjectImpl(objectMapper);
    jsonNodeToSanskritObject(objectMapper, sanskritObject, jsonNode);
    return sanskritObject;
  }
}
