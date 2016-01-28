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
package org.terracotta.management.message;

import org.terracotta.management.call.ContextualReturn;
import org.terracotta.management.notification.ContextualNotification;
import org.terracotta.management.stats.ContextualStatistics;

import java.io.Serializable;
import java.util.Arrays;

/**
 * @author Mathieu Carbou
 */
public final class DefaultMessage implements Serializable, Message {

  private final Object data;
  private final long timeMillis;
  private final String messageType;

  public DefaultMessage(long timeMillis, String messageType, Object data) {
    this.timeMillis = timeMillis;
    this.messageType = messageType;
    this.data = data;
  }

  public DefaultMessage(long timeMillis, ContextualNotification notification) {
    this(timeMillis, MessageType.NOTIFICATION.name(), notification);
  }

  public DefaultMessage(long timeMillis, ContextualReturn<?> response) {
    this(timeMillis, MessageType.RETURN.name(), response);
  }

  public DefaultMessage(long timeMillis, ContextualStatistics... statistics) {
    this(timeMillis, MessageType.STATISTICS.name(), statistics);
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

}
