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
package org.terracotta.management.entity.management.server;

import org.terracotta.entity.ClientDescriptor;
import org.terracotta.management.entity.management.ManagementAgent;
import org.terracotta.management.model.call.ContextualReturn;
import org.terracotta.management.model.capabilities.Capability;
import org.terracotta.management.model.context.ContextContainer;
import org.terracotta.management.model.notification.ContextualNotification;
import org.terracotta.management.model.stats.ContextualStatistics;
import org.terracotta.management.service.monitoring.ClientMonitoringService;
import org.terracotta.voltron.proxy.ClientId;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/***
 * @author Mathieu Carbou
 */
class ActiveManagementAgent implements ManagementAgent {

  private final ClientMonitoringService clientMonitoringService;

  ActiveManagementAgent(ClientMonitoringService clientMonitoringService) {
    this.clientMonitoringService = Objects.requireNonNull(clientMonitoringService);
  }

  @Override
  public Future<Void> pushNotification(@ClientId Object caller, ContextualNotification notification) {
    if (notification != null) {
      clientMonitoringService.pushNotification((ClientDescriptor) caller, notification);
    }
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public Future<Void> pushStatistics(@ClientId Object caller, ContextualStatistics... statistics) {
    if (statistics != null && statistics.length > 0) {
      clientMonitoringService.pushStatistics((ClientDescriptor) caller, statistics);
    }
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public Future<Void> exposeManagementMetadata(@ClientId Object caller, ContextContainer contextContainer, Capability... capabilities) {
    if (contextContainer != null && capabilities != null) {
      clientMonitoringService.exposeManagementRegistry((ClientDescriptor) caller, contextContainer, capabilities);
    }
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public Future<Void> exposeTags(@ClientId Object caller, String... tags) {
    if (tags != null) {
      clientMonitoringService.exposeTags((ClientDescriptor) caller, tags);
    }
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public Future<Void> answerManagementCall(@ClientId Object caller, String managementCallIdentifier, ContextualReturn<?> contextualReturn) {
    clientMonitoringService.answerManagementCall((ClientDescriptor) caller, managementCallIdentifier, contextualReturn);
    return CompletableFuture.completedFuture(null);
  }

}
