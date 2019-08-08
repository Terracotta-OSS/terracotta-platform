/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.nomad.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.terracottatech.dynamic_config.model.NodeContext;
import com.terracottatech.persistence.sanskrit.HashUtils;

/**
 * @author Mathieu Carbou
 */
public class DefaultHashComputer implements HashComputer<NodeContext> {

  private final ObjectMapper mapper;

  public DefaultHashComputer(ObjectMapper mapper) {
    this.mapper = mapper.copy();
    this.mapper.configure(SerializationFeature.INDENT_OUTPUT, false);
  }

  @Override
  public String computeHash(NodeContext o) {
    try {
      return HashUtils.generateHash(mapper.writeValueAsString(o));
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }
}
