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

import org.terracotta.management.model.Objects;
import org.terracotta.management.model.call.ContextualReturn;
import org.terracotta.management.model.notification.ContextualNotification;
import org.terracotta.management.model.stats.ContextualStatistics;

import java.io.Serializable;
import java.util.Arrays;

/**
 * @author Mathieu Carbou
 */
public class DefaultMessage implements Serializable, Message {

  private final Object data;
  private final long timeMillis;
  private final String messageType;

  protected DefaultMessage(long timeMillis, String messageType, Object data) {
    this.timeMillis = timeMillis;
    this.messageType = Objects.requireNonNull(messageType);
    this.data = Objects.requireNonNull(data);
  }

  public DefaultMessage(long timeMillis, ContextualNotification notification) {
    this(timeMillis, "NOTIFICATION", notification);
  }

  public DefaultMessage(long timeMillis, ContextualReturn<?> response) {
    this(timeMillis, "RETURN", response);
  }

  public DefaultMessage(long timeMillis, ContextualStatistics... statistics) {
    this(timeMillis, "STATISTICS", statistics);
  }

  @Override
  public <T> T unwrap(Class<T> type) {
    return type.cast(data);
  }

  @Override
  public long getTimeMillis() {
    return timeMillis;
  }

  @Override
  public String getType() {
    return messageType;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() +
        "{" +
        "timeMillis=" + getTimeMillis() +
        ", type=" + getType() +
        ", data=" + (data == null ? null : data.getClass().isArray() ? Arrays.toString((Object[]) data) : data) +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    DefaultMessage that = (DefaultMessage) o;

    if (timeMillis != that.timeMillis) return false;
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
    result = 31 * result + (int) (timeMillis ^ (timeMillis >>> 32));
    result = 31 * result + messageType.hashCode();
    return result;
  }

}
