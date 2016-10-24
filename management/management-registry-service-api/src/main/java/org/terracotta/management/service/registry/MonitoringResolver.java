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
package org.terracotta.management.service.registry;

import com.tc.classloader.CommonComponent;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.management.model.cluster.ClientIdentifier;
import org.terracotta.management.model.notification.ContextualNotification;
import org.terracotta.management.model.stats.ContextualStatistics;

/**
 * Delegates all method call to a MonitoringService - this interface is to be used within management providers that needs to access the monitoring service
 *
 * @author Mathieu Carbou
 */
@CommonComponent
public interface MonitoringResolver {

  ClientIdentifier getConnectedClientIdentifier(ClientDescriptor clientDescriptor);

  long getConsumerId();

  /**
   * Push a new server-side notification coming from the entity consuming this service. This will be put in a best effort-buffer.
   * <p>
   * Can be called from active or passive entity
   */
  void pushServerEntityNotification(ContextualNotification notification);

  /**
   * Push some server-side statistics coming from the entity consuming this service. This will be put in a best effort-buffer.
   * <p>
   * Can be called from active or passive entity
   */
  void pushServerEntityStatistics(ContextualStatistics... statistics);

}
