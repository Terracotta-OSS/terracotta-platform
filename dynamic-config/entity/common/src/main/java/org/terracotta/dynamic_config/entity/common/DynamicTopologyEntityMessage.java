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
    EVENT_NODE_REMOVAL,
    EVENT_SETTING_CHANGED,
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
    return "DynamicTopologyEntityMessage{" +
        "type=" + type +
        ", payload=" + payload +
        '}';
  }
}
