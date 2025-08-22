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
package org.terracotta.management.entity.nms.agent.client;

import org.terracotta.management.model.capabilities.Capability;
import org.terracotta.management.model.context.ContextContainer;
import org.terracotta.management.model.notification.ContextualNotification;
import org.terracotta.management.model.stats.ContextualStatistics;

import java.io.Closeable;
import java.util.Collection;

/**
 * @author Mathieu Carbou
 */
public interface NmsAgentService extends Closeable {
  @Override
  void close();

  boolean isDisconnected();

  boolean isClosed();

  boolean isManagementRegistryBridged();

  void setCapabilities(ContextContainer contextContainer, Collection<? extends Capability> capabilities);

  void setCapabilities(ContextContainer contextContainer, Capability... capabilities);

  void setTags(Collection<String> tags);

  void setTags(String... tags);

  void pushNotification(ContextualNotification notification);

  void pushStatistics(Collection<ContextualStatistics> statistics);

  void pushStatistics(ContextualStatistics... statistics);

  /**
   * Sends registry and tags to server
   */
  void sendStates();

  /**
   * Clear the current entity which will force a recycling at next call
   */
  void flushEntity();
}
