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
package org.terracotta.management.model.message;

import org.terracotta.management.sequence.Sequence;

import java.io.Serializable;
import java.util.Arrays;

/**
 * @author Mathieu Carbou
 */
public class DefaultMessage implements Message {

  private final Serializable data;
  private final Sequence sequence;
  private final String messageType;

  public DefaultMessage(Sequence sequence, String messageType, Serializable data) {
    this.sequence = sequence;
    this.messageType = messageType;
    this.data = data;
  }

  @Override
  public <T extends Serializable> T unwrap(Class<T> type) {
    return type.cast(data);
  }

  @Override
  public Sequence getSequence() {
    return sequence;
  }

  @Override
  public long getTimestamp() {
    return sequence.getTimestamp();
  }

  @Override
  public String getType() {
    return messageType;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() +
        "{" +
        "sequence=" + sequence +
        ", type=" + messageType +
        ", data=" + (data == null ? null : data.getClass().isArray() ? Arrays.toString((Object[]) data) : data) +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    DefaultMessage that = (DefaultMessage) o;

    if (!sequence.equals(that.sequence)) return false;
    if (data.getClass() != that.data.getClass()) return false;
    if (data.getClass().isArray()) {
      if (!Arrays.equals((Object[]) data, (Object[]) that.data)) return false;
    } else {
      if (!data.equals(that.data)) return false;
    }
    return messageType.equals(that.messageType);

  }

  @Override
  public int hashCode() {
    int result = data.getClass().isArray() ? Arrays.hashCode((Object[]) data) : data.hashCode();
    result = 31 * result + sequence.hashCode();
    result = 31 * result + messageType.hashCode();
    return result;
  }

}
