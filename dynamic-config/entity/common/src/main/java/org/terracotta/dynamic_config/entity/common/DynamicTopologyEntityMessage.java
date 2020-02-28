/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.entity.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;

/**
 * @author Mathieu Carbou
 */
public class DynamicTopologyEntityMessage implements EntityMessage, EntityResponse {

  public enum Type {
    REQ_UPCOMING_CLUSTER,
    REQ_RUNTIME_CLUSTER,
    REQ_MUST_BE_RESTARTED,
    REQ_HAS_INCOMPLETE_CHANGE,
    REQ_LICENSE,
    EVENT_NODE_ADDITION,
    EVENT_NODE_REMOVAL
  }

  private final Type type;

  @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
  private final Object payload;

  public DynamicTopologyEntityMessage(Type type) {
    this(type, null);
  }

  @JsonCreator
  public DynamicTopologyEntityMessage(@JsonProperty(value = "type", required = true) Type type,
                                      @JsonProperty(value = "payload") Object payload) {
    this.type = type;
    this.payload = payload;
  }

  public Type getType() {
    return type;
  }

  public Object getPayload() {
    return payload;
  }

  @Override
  public String toString() {
    return "DynamicConfigEntityMessage{" +
        "type=" + type +
        ", payload=" + payload +
        '}';
  }
}
