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
import org.terracotta.entity.CommonServerEntity;
import org.terracotta.management.model.call.ContextualReturn;
import org.terracotta.management.model.capabilities.Capability;
import org.terracotta.management.model.context.ContextContainer;
import org.terracotta.management.model.notification.ContextualNotification;
import org.terracotta.management.model.stats.ContextualStatistics;

import java.io.Closeable;

/**
 * Class used by to push some data into the monitoring service concerning client management information
 *
 * @author Mathieu Carbou
 */
@CommonComponent
public interface ClientMonitoringService extends Closeable {

  /**
   * Push a new client-side notification coming from a client descriptor in the monitoring service. This will be put in a best effort-buffer.
   * <p>
   * Can be called from active entity only
   */
  void pushNotification(ClientDescriptor from, ContextualNotification notification);

  /**
   * Push some client statistics coming fro
   * <p>
   * Can be called from active entity onlym a client descriptor into the service. This will be put in a best effort-buffer.
   */
  void pushStatistics(ClientDescriptor from, ContextualStatistics... statistics);

  /**
   * Associate some tagging information to a client
   * <p>
   * Can be called from active entity only
   */
  void exposeTags(ClientDescriptor from, String... tags);

  /**
   * Expose a management registry onto the client identified by a client descriptor
   * <p>
   * Can be called from active entity only
   */
  void exposeManagementRegistry(ClientDescriptor caller, ContextContainer contextContainer, Capability... capabilities);

  /**
   * Answer a management call we received and executed
   * <p>
   * Can be called from active entity only
   *
   * @param managementCallIdentifier The unique identifier of the management call
   * @param contextualReturn         The result of the call
   */
  void answerManagementCall(ClientDescriptor caller, String managementCallIdentifier, ContextualReturn<?> contextualReturn);

  /**
   * Closes this service from {@link CommonServerEntity#destroy()}
   */
  @Override
  void close();
}
