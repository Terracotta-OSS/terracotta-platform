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

import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * @author Mathieu Carbou
 */
public class Message implements EntityMessage {

  private final Type type;

  public Message(Type type) {
    this.type = requireNonNull(type);
  }

  public Type getType() {
    return type;
  }

  @Override
  public String toString() {
    return "Message{" +
        "type=" + type +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Message)) return false;
    Message that = (Message) o;
    return getType() == that.getType();
  }

  @Override
  public int hashCode() {
    return Objects.hash(getType());
  }
}
