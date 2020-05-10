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
package org.terracotta.dynamic_config.entity.topology.common;

import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

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

  private final Object payload;

  public DynamicTopologyEntityMessage(Type type) {
    this(type, null);
  }

  public DynamicTopologyEntityMessage(Type type, Object payload) {
    this.type = requireNonNull(type);
    this.payload = payload;
  }

  public Type getType() {
    return type;
  }

  @SuppressWarnings("unchecked")
  public <T> T getPayload() {
    return (T) payload;
  }

  @Override
  public String toString() {
    return "DynamicTopologyEntityMessage{" +
        "type=" + type +
        ", payload=" + payload +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof DynamicTopologyEntityMessage)) return false;
    DynamicTopologyEntityMessage that = (DynamicTopologyEntityMessage) o;
    return getType() == that.getType() &&
        Objects.equals(getPayload(), that.getPayload());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getType(), getPayload());
  }
}
