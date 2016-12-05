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
import org.terracotta.management.entity.tms.TmsAgent;
import org.terracotta.management.entity.tms.TmsAgentConfig;
import org.terracotta.management.model.call.Parameter;
import org.terracotta.management.model.cluster.Cluster;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.context.ContextContainer;
import org.terracotta.management.model.message.DefaultMessage;
import org.terracotta.management.model.message.Message;
import org.terracotta.management.service.monitoring.ConsumerManagementRegistry;
import org.terracotta.management.service.monitoring.ManagementService;
import org.terracotta.management.service.monitoring.ReadOnlyBuffer;
import org.terracotta.management.service.monitoring.registry.provider.StatisticCollectorManagementProvider;
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
class TmsAgentImpl implements TmsAgent {

  private static final Comparator<Message> MESSAGE_COMPARATOR = (o1, o2) -> o1.getSequence().compareTo(o2.getSequence());

  private final ReadOnlyBuffer<Message> buffer;
  private final ManagementService managementService;
  private final ConsumerManagementRegistry consumerManagementRegistry;

  TmsAgentImpl(TmsAgentConfig config, ManagementService managementService, ConsumerManagementRegistry consumerManagementRegistry) {
    this.managementService = Objects.requireNonNull(managementService);
    this.consumerManagementRegistry = Objects.requireNonNull(consumerManagementRegistry);
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

  void init() {
    ContextContainer contextContainer = consumerManagementRegistry.getContextContainer();

    // the context for the collector, created from the the registry of the tms entity
    Context context = Context.create(contextContainer.getName(), contextContainer.getValue());

    // we create a provider that will receive management calls to control the global voltron's statistic collector
    // this provider will thus be on top of the tms entity
    StatisticCollectorManagementProvider collectorManagementProvider = new StatisticCollectorManagementProvider(context);
    consumerManagementRegistry.addManagementProvider(collectorManagementProvider);

    // start the stat collector (it won't collect any stats though, because they need to be configured through a management call)
    collectorManagementProvider.init();

    consumerManagementRegistry.refresh();
  }

}
