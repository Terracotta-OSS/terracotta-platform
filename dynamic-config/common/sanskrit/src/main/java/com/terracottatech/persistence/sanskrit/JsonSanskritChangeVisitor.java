/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.persistence.sanskrit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.terracottatech.persistence.sanskrit.change.SanskritChangeVisitor;

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
