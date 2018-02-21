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

package org.terracotta.management.service.monitoring;

import com.tc.classloader.CommonComponent;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author Mathieu Carbou
 */
@CommonComponent
public class ManagementMessage implements Serializable {

  private static final long serialVersionUID = 1;

  @CommonComponent
  public enum Type {
    NOTIFICATION,
    STATISTICS,
    REGISTRY,
    MANAGEMENT_ANSWER
  }

  private final Type type;
  private final Serializable data;
  private final MessageSource messageSource;

  public ManagementMessage(String serverName, long consumerId, boolean activeEntity, Type type, Serializable data) {
    this.type = Objects.requireNonNull(type);
    this.data = Objects.requireNonNull(data);
    this.messageSource = new MessageSource(serverName, consumerId, activeEntity);
  }

  public Type getType() {
    return type;
  }

  public Serializable getData() {
    return data;
  }

  public MessageSource getMessageSource() {
    return messageSource;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("ManagementMessage{");
    sb.append("type=").append(type);
    sb.append(", messageSource=").append(messageSource);
    sb.append(", data=").append(data);
    sb.append('}');
    return sb.toString();
  }
}
