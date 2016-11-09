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
package org.terracotta.management.entity.tms.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.entity.tms.TmsAgent;
import org.terracotta.management.entity.tms.TmsAgentConfig;
import org.terracotta.management.model.call.ContextualReturn;
import org.terracotta.management.model.call.Parameter;
import org.terracotta.management.model.cluster.Cluster;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.message.DefaultMessage;
import org.terracotta.management.model.message.Message;
import org.terracotta.management.service.monitoring.MonitoringService;
import org.terracotta.management.service.monitoring.ReadOnlyBuffer;
import org.terracotta.management.service.monitoring.SharedManagementRegistry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * @author Mathieu Carbou
 */
class TmsAgentImpl implements TmsAgent {

  private static final Logger LOGGER = LoggerFactory.getLogger(TmsAgentImpl.class);
  private static final Comparator<Message> MESSAGE_COMPARATOR = (o1, o2) -> o1.getSequence().compareTo(o2.getSequence());

  private final ReadOnlyBuffer<Message> buffer;
  private final MonitoringService monitoringService;
  private final SharedManagementRegistry managementRegistry;

  TmsAgentImpl(TmsAgentConfig config, MonitoringService monitoringService, SharedManagementRegistry managementRegistry) {
    this.monitoringService = Objects.requireNonNull(monitoringService);
    this.managementRegistry = Objects.requireNonNull(managementRegistry);
    this.buffer = monitoringService.createMessageBuffer(config.getMaximumUnreadMessages());
  }

  @Override
  public Future<Cluster> readTopology() {
    return CompletableFuture.completedFuture(monitoringService.readTopology());
  }

  @Override
  public synchronized Future<List<Message>> readMessages() {
    List<Message> messages = new ArrayList<>(this.buffer.size());
    buffer.drainTo(messages);

    if (!messages.isEmpty()) {
      Cluster cluster = monitoringService.readTopology();
      messages.add(new DefaultMessage(monitoringService.nextSequence(), "TOPOLOGY", cluster));
      messages.sort(MESSAGE_COMPARATOR);
    }

    return CompletableFuture.completedFuture(messages);
  }

  @Override
  public <T> Future<ContextualReturn<T>> call(Context context, String capabilityName, String methodName, Class<T> returnType, Parameter... parameters) {
    LOGGER.trace("call({}, {}, {})", context, capabilityName, methodName);
    if (!monitoringService.getServerEntityIdentifier(context).isPresent()) {
      LOGGER.warn("call({}, {}, {}): Entity not found on server {} matching this context.", context, capabilityName, methodName, monitoringService.getCurrentServerName());
      return CompletableFuture.completedFuture(ContextualReturn.notExecuted(capabilityName, context, methodName));
    }
    return CompletableFuture.completedFuture(managementRegistry.withCapability(capabilityName)
        .call(methodName, returnType, parameters)
        .on(context)
        .build()
        .execute()
        .getSingleResult());
  }

}
