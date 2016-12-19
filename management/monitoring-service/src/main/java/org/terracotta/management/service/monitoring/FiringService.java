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

import org.terracotta.management.model.call.ContextualCall;
import org.terracotta.management.model.call.ContextualReturn;
import org.terracotta.management.model.notification.ContextualNotification;
import org.terracotta.management.model.stats.ContextualStatistics;

/**
 * Handles all events that needs to be sent into the ring buffers or through a client communicator
 *
 * @author Mathieu Carbou
 */
interface FiringService {
  void fireNotification(ContextualNotification notification);

  void fireStatistics(ContextualStatistics[] statistics);

  void fireManagementCallAnswer(String managementCallIdentifier, ContextualReturn<?> answer);

  void fireManagementCallRequest(String managementCallIdentifier, ContextualCall<?> contextualCall);
}
