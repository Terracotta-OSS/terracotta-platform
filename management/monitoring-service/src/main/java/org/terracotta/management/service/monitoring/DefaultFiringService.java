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
import org.terracotta.management.model.cluster.Client;
import org.terracotta.management.model.message.DefaultManagementCallMessage;
import org.terracotta.management.model.message.DefaultMessage;
import org.terracotta.management.model.notification.ContextualNotification;
import org.terracotta.management.model.stats.ContextualStatistics;
import org.terracotta.management.sequence.SequenceGenerator;

import java.util.Map;
import java.util.Objects;

/**
 * @author Mathieu Carbou
 */
class DefaultFiringService implements FiringService {

  private final SequenceGenerator sequenceGenerator;
  private final Map<Long, DefaultManagementService> managementServices;
  private final Map<Long, DefaultClientMonitoringService> clientMonitoringServices;

  DefaultFiringService(SequenceGenerator sequenceGenerator, Map<Long, DefaultManagementService> managementServices, Map<Long, DefaultClientMonitoringService> clientMonitoringServices) {
    this.sequenceGenerator = Objects.requireNonNull(sequenceGenerator);
    this.managementServices = Objects.requireNonNull(managementServices);
    this.clientMonitoringServices = Objects.requireNonNull(clientMonitoringServices);
  }

  @Override
  public void fireNotification(ContextualNotification notification) {
    DefaultMessage message = new DefaultMessage(sequenceGenerator.next(), "NOTIFICATION", notification);
    managementServices.values().forEach(managementService -> managementService.fireMessage(message));
  }

  @Override
  public void fireStatistics(ContextualStatistics[] statistics) {
    DefaultMessage message = new DefaultMessage(sequenceGenerator.next(), "STATISTICS", statistics);
    managementServices.values().forEach(managementService -> managementService.fireMessage(message));
  }

  @Override
  public void fireManagementCallAnswer(String managementCallIdentifier, ContextualReturn<?> answer) {
    DefaultManagementCallMessage message = new DefaultManagementCallMessage(managementCallIdentifier, sequenceGenerator.next(), "MANAGEMENT_CALL_RETURN", answer);
    managementServices.values().forEach(managementService -> managementService.fireMessage(message));
  }

  @Override
  public void fireManagementCallRequest(String managementCallIdentifier, ContextualCall<?> call) {
    DefaultManagementCallMessage message = new DefaultManagementCallMessage(managementCallIdentifier, sequenceGenerator.next(), "MANAGEMENT_CALL", call);
    if (call.getContext().contains(Client.KEY)) {
      clientMonitoringServices.values().forEach(clientMonitoringService -> clientMonitoringService.fireMessage(message));
    } else {
      managementServices.values().forEach(managementService -> managementService.fireMessage(message));
    }
  }

}
