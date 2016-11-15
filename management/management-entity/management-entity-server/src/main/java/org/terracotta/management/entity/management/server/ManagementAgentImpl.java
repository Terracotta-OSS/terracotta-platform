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
import org.terracotta.management.model.call.Parameter;
import org.terracotta.management.model.capabilities.Capability;
import org.terracotta.management.model.cluster.ClientIdentifier;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.context.ContextContainer;
import org.terracotta.management.model.notification.ContextualNotification;
import org.terracotta.management.model.stats.ContextualStatistics;
import org.terracotta.management.service.monitoring.MonitoringService;
import org.terracotta.voltron.proxy.ClientId;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/***
 * @author Mathieu Carbou
 */
class ManagementAgentImpl implements ManagementAgent {

  private final MonitoringService monitoringService;

  ManagementAgentImpl(MonitoringService monitoringService) {
    this.monitoringService = monitoringService;
  }

  public Future<ClientIdentifier> getClientIdentifier(@ClientId Object clientDescriptor) {
    return CompletableFuture.completedFuture(monitoringService.getClientIdentifier((ClientDescriptor) clientDescriptor));
  }

  @Override
  public Future<Void> pushNotification(@ClientId Object caller, ContextualNotification notification) {
    monitoringService.pushClientNotification((ClientDescriptor) caller, notification);
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public Future<Void> pushStatistics(@ClientId Object caller, ContextualStatistics... statistics) {
    if (statistics.length > 0) {
      monitoringService.pushClientStatistics((ClientDescriptor) caller, statistics);
    }
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public Future<Void> exposeManagementMetadata(@ClientId Object caller, ContextContainer contextContainer, Capability... capabilities) {
    monitoringService.exposeClientManagementRegistry((ClientDescriptor) caller, contextContainer, capabilities);
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public Future<Void> exposeTags(@ClientId Object caller, String... tags) {
    monitoringService.exposeClientTags((ClientDescriptor) caller, tags);
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public Future<String> call(@ClientId Object callerDescriptor, ClientIdentifier to, Context context, String capabilityName, String methodName, Class<?> returnType, Parameter... parameters) {
    String managementCallIdentifier = monitoringService.sendManagementCallRequest((ClientDescriptor) callerDescriptor, to, context, capabilityName, methodName, returnType, parameters);
    return CompletableFuture.completedFuture(managementCallIdentifier);
  }

  @Override
  public Future<Void> callReturn(@ClientId Object calledDescriptor, ClientIdentifier to, String managementCallIdentifier, ContextualReturn<?> contextualReturn) {
    monitoringService.answerManagementCall((ClientDescriptor) calledDescriptor, to, managementCallIdentifier, contextualReturn);
    return CompletableFuture.completedFuture(null);
  }

}
