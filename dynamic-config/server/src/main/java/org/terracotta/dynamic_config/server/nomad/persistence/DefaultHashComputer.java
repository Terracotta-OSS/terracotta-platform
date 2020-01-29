/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.server.nomad.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.persistence.sanskrit.HashUtils;

/**
 * @author Mathieu Carbou
 */
public class DefaultHashComputer implements HashComputer<NodeContext> {
  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultHashComputer.class);

  private final ObjectMapper mapper;

  public DefaultHashComputer(ObjectMapper mapper) {
    this.mapper = mapper.copy();
    this.mapper.configure(SerializationFeature.INDENT_OUTPUT, false);
  }

  @Override
  public String computeHash(NodeContext o) {
    try {
      String json = mapper.writeValueAsString(o);
      String hash = HashUtils.generateHash(json);
      LOGGER.debug("Computed hash: {} for json: {}", hash, json);
      return hash;
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }
}
