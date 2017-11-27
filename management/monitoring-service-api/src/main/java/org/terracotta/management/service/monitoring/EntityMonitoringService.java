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
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.management.model.call.ContextualReturn;
import org.terracotta.management.model.capabilities.Capability;
import org.terracotta.management.model.cluster.ClientIdentifier;
import org.terracotta.management.model.context.ContextContainer;
import org.terracotta.management.model.notification.ContextualNotification;
import org.terracotta.management.model.stats.ContextualStatistics;

import java.util.concurrent.CompletableFuture;

/**
 * Class used by entities requiring to push some data into the monitoring service
 *
 * @author Mathieu Carbou
 */
@CommonComponent
public interface EntityMonitoringService {

  /**
   * Expose a management registry onto the server entity that is currently consuming this service.
   * <p>
   * Can be called from active or passive entity
   */
  void exposeManagementRegistry(ContextContainer contextContainer, Capability... capabilities);

  /**
   * Push a new server-side notification coming from the entity consuming this service. This will be put in a best effort-buffer.
   * <p>
   * Can be called from active or passive entity
   */
  void pushNotification(ContextualNotification notification);

  /**
   * Push some server-side statistics coming from the entity consuming this service. This will be put in a best effort-buffer.
   * <p>
   * Can be called from active or passive entity
   */
  void pushStatistics(ContextualStatistics... statistics);

  /**
   * Answer a management call we received and executed
   * <p>
   * Can be called from active entity only
   *
   * @param managementCallIdentifier The unique identifier of the management call
   * @param contextualReturn         The result of the call
   */
  void answerManagementCall(String managementCallIdentifier, ContextualReturn<?> contextualReturn);

  /**
   * Can be called from active or passive entity
   *
   * @return This consumer identifier, unique on current server only.
   */
  long getConsumerId();

  /**
   * @return The current server name
   */
  String getServerName();

  /**
   * Map this {@link ClientDescriptor} to a useful {@link ClientIdentifier} to go over the network
   * as soon as monitoring service is made aware of the fetch
   */
  CompletableFuture<ClientIdentifier> getClientIdentifier(ClientDescriptor clientDescriptor);
}
