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

import org.terracotta.entity.ClientDescriptor;
import org.terracotta.management.entity.tms.TmsAgentConfig;
import org.terracotta.management.model.call.Parameter;
import org.terracotta.management.model.cluster.Cluster;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.message.DefaultMessage;
import org.terracotta.management.model.message.Message;
import org.terracotta.management.service.monitoring.ConsumerManagementRegistry;
import org.terracotta.management.service.monitoring.EntityMonitoringService;
import org.terracotta.management.service.monitoring.ManagementService;
import org.terracotta.management.service.monitoring.ReadOnlyBuffer;
import org.terracotta.management.service.monitoring.SharedManagementRegistry;
import org.terracotta.voltron.proxy.ClientId;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * @author Mathieu Carbou
 */
class ActiveTmsAgent extends AbstractTmsAgent {

  private static final Comparator<Message> MESSAGE_COMPARATOR = Comparator.comparing(Message::getSequence);

  private final ReadOnlyBuffer<Message> buffer;
  private final ManagementService managementService;

  ActiveTmsAgent(TmsAgentConfig config, ManagementService managementService, ConsumerManagementRegistry consumerManagementRegistry, EntityMonitoringService entityMonitoringService, SharedManagementRegistry sharedManagementRegistry) {
    super(consumerManagementRegistry, entityMonitoringService, sharedManagementRegistry);
    this.managementService = Objects.requireNonNull(managementService);
    this.buffer = managementService.createMessageBuffer(config.getMaximumUnreadMessages());
  }

  @Override
  public Future<Cluster> readTopology() {
    return CompletableFuture.completedFuture(managementService.readTopology());
  }

  @Override
  public synchronized Future<List<Message>> readMessages() {
    List<Message> messages = new ArrayList<>(this.buffer.size());
    buffer.drainTo(messages);

    if (!messages.isEmpty()) {
      Cluster cluster = managementService.readTopology();
      messages.add(new DefaultMessage(managementService.nextSequence(), "TOPOLOGY", cluster));
      messages.sort(MESSAGE_COMPARATOR);
    }

    return CompletableFuture.completedFuture(messages);
  }

  @Override
  public Future<String> call(@ClientId Object callerDescriptor, Context context, String capabilityName, String methodName, Class<?> returnType, Parameter... parameters) {
    return CompletableFuture.completedFuture(managementService.sendManagementCallRequest((ClientDescriptor) callerDescriptor, context, capabilityName, methodName, returnType, parameters));
  }

}
