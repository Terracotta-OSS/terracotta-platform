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
package org.terracotta.management.entity.tms.server;

import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.message.DefaultMessage;
import org.terracotta.management.model.message.Message;
import org.terracotta.management.model.notification.ContextualNotification;
import org.terracotta.management.sequence.BoundaryFlakeSequence;
import org.terracotta.management.service.monitoring.PlatformNotification;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * @author Mathieu Carbou
 */
class Notification {

  private final PlatformNotification platformNotification;
  private Context context = Context.empty();
  private Map<String, String> attributes = new LinkedHashMap<>(0);

  Notification(PlatformNotification platformNotification) {
    this.platformNotification = platformNotification;
  }

  Message toMessage() {
    return new DefaultMessage(
        platformNotification.getSequence(),
        "NOTIFICATION",
        new ContextualNotification(context, platformNotification.getType().name(), attributes));
  }

  void setAttribute(String key, String val) {
    attributes.put(key, val);
  }

  PlatformNotification.Type getType() {
    return platformNotification.getType();
  }

  long getIndex() {
    return platformNotification.getIndex();
  }

  <T extends Serializable> T getSource(Class<T> type) {
    return platformNotification.getSource(type);
  }

  void setContext(Context context) {
    this.context = requireNonNull(context);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("Notification{");
    sb.append("context=").append(context);
    sb.append(", platformNotification=").append(platformNotification);
    sb.append('}');
    return sb.toString();
  }
}
