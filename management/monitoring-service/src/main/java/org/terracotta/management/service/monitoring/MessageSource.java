/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
public class MessageSource implements Serializable {

  private static final long serialVersionUID = 1;
  
  private final String serverName;
  private final long consumerId;
  private final boolean activeEntity;

  public MessageSource(String serverName, long consumerId, boolean activeEntity) {
    this.serverName = Objects.requireNonNull(serverName);
    this.consumerId = consumerId;
    this.activeEntity = activeEntity;  
  }

  public String getServerName() {
    return serverName;
  }

  public long getConsumerId() {
    return consumerId;
  }

  public boolean isActiveEntity() {
    return activeEntity;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("MessageSource{");
    sb.append("serverName='").append(serverName).append('\'');
    sb.append(", consumerId=").append(consumerId);
    sb.append(", activeEntity=").append(activeEntity);
    sb.append('}');
    return sb.toString();
  }
}
