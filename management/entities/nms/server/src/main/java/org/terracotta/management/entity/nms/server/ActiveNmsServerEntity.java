/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package org.terracotta.management.entity.nms.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.StateDumpCollector;
import org.terracotta.management.entity.nms.Nms;
import org.terracotta.management.entity.nms.NmsConfig;
import org.terracotta.management.model.call.ContextualCall;
import org.terracotta.management.model.call.ContextualReturn;
import org.terracotta.management.model.call.Parameter;
import org.terracotta.management.model.cluster.Client;
import org.terracotta.management.model.cluster.Cluster;
import org.terracotta.management.model.cluster.Server;
import org.terracotta.management.model.cluster.Stripe;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.context.Contextual;
import org.terracotta.management.model.message.Message;
import org.terracotta.management.registry.CapabilityManagementSupport;
import org.terracotta.management.registry.CombiningCapabilityManagementSupport;
import org.terracotta.management.service.monitoring.EntityManagementRegistry;
import org.terracotta.management.service.monitoring.ManagementExecutor;
import org.terracotta.management.service.monitoring.ManagementService;
import org.terracotta.management.service.monitoring.SharedEntityManagementRegistry;
import org.terracotta.voltron.proxy.ClientId;
import org.terracotta.voltron.proxy.server.ActiveProxiedServerEntity;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * @author Mathieu Carbou
 */
class ActiveNmsServerEntity extends ActiveProxiedServerEntity<Void, Void, NmsCallback> implements Nms, ManagementExecutor {

  private static final Logger LOGGER = LoggerFactory.getLogger(ActiveNmsServerEntity.class);

  private final ManagementService managementService;
  private final String stripeName;
  private final EntityManagementRegistry entityManagementRegistry;
  private final CapabilityManagementSupport capabilityManagementSupport;
  private final long consumerId;

  ActiveNmsServerEntity(NmsConfig config, ManagementService managementService, EntityManagementRegistry entityManagementRegistry, SharedEntityManagementRegistry sharedEntityManagementRegistry, TopologyService topologyService) {
    this.entityManagementRegistry = Objects.requireNonNull(entityManagementRegistry);
    // we create a group of registries from the entities shared registries plus this management registry (of this NMS entity)
    this.capabilityManagementSupport = new CombiningCapabilityManagementSupport(sharedEntityManagementRegistry, entityManagementRegistry);
    this.managementService = Objects.requireNonNull(managementService);
    this.stripeName = topologyService.getRuntimeNodeContext().getStripe().getName();
    this.consumerId = entityManagementRegistry.getMonitoringService().getConsumerId();
  }

  // ActiveProxiedServerEntity

  @Override
  public void destroy() {
    entityManagementRegistry.close();
    managementService.close();
    super.destroy();
  }

  @Override
  public void createNew() {
    super.createNew();
    LOGGER.trace("[{}] createNew()", consumerId);
    entityManagementRegistry.entityCreated();
    entityManagementRegistry.refresh();
  }

  @Override
  public void loadExisting() {
    super.loadExisting();
    LOGGER.trace("[{}] loadExisting()", consumerId);
    entityManagementRegistry.entityPromotionCompleted();
    entityManagementRegistry.refresh();
  }

  @Override
  protected void dumpState(StateDumpCollector dump) {
    dump.addState("consumerId", String.valueOf(consumerId));
    dump.addState("stripeName", String.valueOf(stripeName));
  }

  // Nms

  @Override
  public Future<Cluster> readTopology() {
    return CompletableFuture.completedFuture(readCluster());
  }

  @Override
  public Future<String> call(@ClientId Object callerDescriptor, Context context, String capabilityName, String methodName, Class<?> returnType, Parameter... parameters) {
    if (context.contains(Stripe.KEY)) {
      context = context.with(Stripe.KEY, stripeName);
    }
    return CompletableFuture.completedFuture(managementService.sendManagementCallRequest((ClientDescriptor) callerDescriptor, context, capabilityName, methodName, returnType, parameters));
  }

  private Cluster readCluster() {
    return managementService.readTopology();
  }

  // ManagementExecutor

  @Override
  public void executeManagementCallOnServer(String managementCallIdentifier, ContextualCall<?> call) {
    String serverName = call.getContext().get(Server.NAME_KEY);
    if (serverName == null) {
      throw new IllegalArgumentException("Bad context: " + call.getContext());
    }

    if (entityManagementRegistry.getMonitoringService().getServerName().equals(serverName)) {
      LOGGER.trace("[{}] executeManagementCallOnServer({}, {}, {}, {})", consumerId, managementCallIdentifier, call.getContext(), call.getCapability(), call.getMethodName());
      ContextualReturn<?> contextualReturn = capabilityManagementSupport.withCapability(call.getCapability())
          .call(call.getMethodName(), call.getReturnType(), call.getParameters())
          .on(call.getContext())
          .build()
          .execute()
          .getSingleResult();
      if (contextualReturn.hasExecuted()) {
        entityManagementRegistry.getMonitoringService().answerManagementCall(managementCallIdentifier, contextualReturn);
      }

    } else {
      getMessenger().executeManagementCallOnPassive(managementCallIdentifier, call);
    }
  }

  @Override
  public void sendMessageToClients(Message message) {
    LOGGER.trace("[{}] sendMessageToClients({})", consumerId, message);
    // add stripe info to the message
    addStripeName(message);
    // send message
    fireMessage(Message.class, message, false);
  }

  @Override
  public void sendMessageToClient(Message message, ClientDescriptor to) {
    if (getClients().contains(to)) {
      LOGGER.trace("[{}] sendMessageToClient({}, {})", consumerId, message, to);
      // add stripe info to the message
      addStripeName(message);
      // send message
      fireMessage(Message.class, message, to);
    }
  }

  private void addStripeName(Message message) {
    message.unwrap(Contextual.class)
        .stream()
        .filter(contextual -> !contextual.getContext().contains(Client.KEY))
        .forEach(contextual -> contextual.setContext(contextual.getContext().with(Stripe.KEY, stripeName)));
  }

}
