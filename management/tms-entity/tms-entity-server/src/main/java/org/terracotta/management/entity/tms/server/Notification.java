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
import org.terracotta.management.service.monitoring.Mutation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * @author Mathieu Carbou
 */
class Notification {

  private final Mutation mutation;
  private Context context = null;
  private Map<String, String> attributes = null;
  private String type;

  Notification(Mutation mutation) {
    this.mutation = mutation;
    this.type = PlatformNotificationType.getType(mutation);
  }

  String getType() {
    return type;
  }

  PlatformNotificationType getPlatformNotificationType() {
    try {
      return PlatformNotificationType.valueOf(type);
    } catch (IllegalArgumentException e) {
      return PlatformNotificationType.OTHER;
    }
  }

  long getIndex() {return mutation.getIndex();}

  void setType(String type) {
    this.type = type;
  }

  String getName() {return mutation.getName();}

  String getPath(int i) {return mutation.getPath(i);}

  boolean pathMatches(String... pathPatterns) {return mutation.pathMatches(pathPatterns);}

  Object getParentValue(int i) {return mutation.getParentValue(i);}

  boolean isValueChanged() {return mutation.isValueChanged();}

  Object getOldValue() {return mutation.getOldValue();}

  Object getNewValue() {return mutation.getNewValue();}

  Object getValue() {
    return getNewValue() == null ? getOldValue() : getNewValue();
  }

  boolean isAnyType(PlatformNotificationType... types) {
    PlatformNotificationType notificationType = getPlatformNotificationType();
    for (PlatformNotificationType type : types) {
      if (type.equals(notificationType)) {
        return true;
      }
    }
    return false;
  }

  Message toMessage() {
    return new DefaultMessage(
        BoundaryFlakeSequence.fromBytes(mutation.getSequence()),
        "NOTIFICATION",
        new ContextualNotification(
            requireNonNull(context),
            type,
            attributes == null ? Collections.emptyMap() : attributes));
  }

  void setAttribute(String key, String val) {
    if (attributes == null) {
      attributes = new LinkedHashMap<>();
    }
    attributes.put(key, val);
  }

  void setContext(Context context) {
    this.context = context;
  }

  Context getContext() {
    return context;
  }

  Notification copy() {
    Notification notification = new Notification(mutation);
    notification.context = context;
    notification.attributes = attributes == null ? null : new LinkedHashMap<>(attributes);
    notification.type = type;
    return notification;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("Notification{");
    sb.append("type='").append(type).append('\'');
    sb.append(", context=").append(context);
    sb.append('}');
    return sb.toString();
  }

}
