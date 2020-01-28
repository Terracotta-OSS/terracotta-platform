/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.diagnostic.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.terracottatech.json.Json;

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
