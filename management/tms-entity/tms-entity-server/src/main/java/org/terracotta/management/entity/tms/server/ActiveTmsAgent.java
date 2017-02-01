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
import org.terracotta.management.model.cluster.Stripe;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.context.Contextual;
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
  private final String stripeName;

  ActiveTmsAgent(TmsAgentConfig config, ManagementService managementService, ConsumerManagementRegistry consumerManagementRegistry, EntityMonitoringService entityMonitoringService, SharedManagementRegistry sharedManagementRegistry) {
    super(consumerManagementRegistry, entityMonitoringService, sharedManagementRegistry);
    this.managementService = Objects.requireNonNull(managementService);
    this.buffer = managementService.createMessageBuffer(config.getMaximumUnreadMessages());
    this.stripeName = config.getStripeName();
  }

  @Override
  public Future<Cluster> readTopology() {
    return CompletableFuture.completedFuture(readCluster());
  }

  @Override
  public synchronized Future<List<Message>> readMessages() {
    List<Message> messages = new ArrayList<>(this.buffer.size());
    buffer.drainTo(messages);

    if (!messages.isEmpty()) {
      for (Message message : messages) {
        message.unwrap(Contextual.class)
            .stream()
            .filter(contextual -> contextual.getContext().contains(Stripe.KEY))
            .forEach(contextual -> contextual.setContext(contextual.getContext().with(Stripe.KEY, stripeName)));
      }
      messages.add(new DefaultMessage(managementService.nextSequence(), "TOPOLOGY", readCluster()));
      messages.sort(MESSAGE_COMPARATOR);
    }

    return CompletableFuture.completedFuture(messages);
  }

  @Override
  public Future<String> call(@ClientId Object callerDescriptor, Context context, String capabilityName, String methodName, Class<?> returnType, Parameter... parameters) {
    if(context.contains(Stripe.KEY)) {
      context = context.with(Stripe.KEY, "SINGLE");
    }
    return CompletableFuture.completedFuture(managementService.sendManagementCallRequest((ClientDescriptor) callerDescriptor, context, capabilityName, methodName, returnType, parameters));
  }

  private Cluster readCluster() {
    Cluster cluster = managementService.readTopology();
    // if we want a specific name for our stripe, just rename it
    if (!stripeName.equals(cluster.getSingleStripe().getName())) {
      Stripe namedStripe = Stripe.create(stripeName);
      Stripe currentStripe = cluster.getSingleStripe();
      // move servers
      currentStripe.serverStream().forEach(namedStripe::addServer);
      // remove current stripe and add new one
      cluster.removeStripe(currentStripe.getId());
      cluster.addStripe(namedStripe);
    }
    return cluster;
  }

}
