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
import org.terracotta.entity.BasicServiceConfiguration;
import org.terracotta.entity.ServiceRegistry;
import org.terracotta.management.entity.tms.TmsAgent;
import org.terracotta.management.entity.tms.TmsAgentConfig;
import org.terracotta.management.model.call.ContextualReturn;
import org.terracotta.management.model.call.Parameter;
import org.terracotta.management.model.cluster.Cluster;
import org.terracotta.management.model.cluster.Server;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.message.DefaultMessage;
import org.terracotta.management.model.message.Message;
import org.terracotta.management.service.monitoring.MonitoringService;
import org.terracotta.management.service.monitoring.MonitoringServiceConfiguration;
import org.terracotta.management.service.monitoring.PlatformManagementRegistry;
import org.terracotta.management.service.monitoring.PlatformManagementRegistryConfiguration;
import org.terracotta.management.service.monitoring.ReadOnlyBuffer;
import org.terracotta.management.service.monitoring.SharedManagementRegistry;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * @author Mathieu Carbou
 */
class TmsAgentImpl implements TmsAgent, Closeable {

  private static final Logger LOGGER = LoggerFactory.getLogger(TmsAgentImpl.class);
  private static final Comparator<Message> MESSAGE_COMPARATOR = (o1, o2) -> o1.getSequence().compareTo(o2.getSequence());

  private final ReadOnlyBuffer<Message> buffer;
  private final TmsAgentConfig config;
  private final MonitoringService monitoringService;
  private final SharedManagementRegistry sharedManagementRegistry;
  private final PlatformManagementRegistry platformManagementRegistry;

  TmsAgentImpl(TmsAgentConfig config, ServiceRegistry serviceRegistry) {
    this.config = config;
    this.monitoringService = Objects.requireNonNull(serviceRegistry.getService(new MonitoringServiceConfiguration(serviceRegistry)));
    this.sharedManagementRegistry = Objects.requireNonNull(serviceRegistry.getService(new BasicServiceConfiguration<>(SharedManagementRegistry.class)));
    this.buffer = monitoringService.createMessageBuffer(config.getMaximumUnreadMessages());
    this.platformManagementRegistry = Objects.requireNonNull(serviceRegistry.getService(new PlatformManagementRegistryConfiguration(serviceRegistry)
        .setStatisticConfiguration(config.getStatisticConfiguration())));
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
  public Future<ContextualReturn<Void>> updateCollectedStatistics(Context context, String capabilityName, Collection<String> statisticNames) {
    return call(context,
        "StatisticCollectorCapability",
        "updateCollectedStatistics",
        Void.TYPE,
        new Parameter(capabilityName),
        new Parameter(statisticNames, Collection.class.getName()));
  }

  @Override
  public <T> Future<ContextualReturn<T>> call(Context context, String capabilityName, String methodName, Class<T> returnType, Parameter... parameters) {
    LOGGER.trace("call({}, {}, {})", context, capabilityName, methodName);
    if (!context.contains(Server.NAME_KEY) || !context.contains(Server.KEY)) {
      throw new IllegalArgumentException("Incomplete context: missing server name in context: " + context);
    }
    // validate entity
    if (!monitoringService.getServerEntityIdentifier(context).isPresent()) {
      LOGGER.warn("call({}, {}, {}): Entity not found on server {} matching this context.", context, capabilityName, methodName, monitoringService.getCurrentServerName());
      return CompletableFuture.completedFuture(ContextualReturn.notExecuted(capabilityName, context, methodName));
    }
    // validate server (active or passive)
    String serverName = context.get(Server.NAME_KEY);
    if (serverName == null) {
      serverName = context.get(Server.KEY);
    }
    if (!monitoringService.getCurrentServerName().equals(serverName)) {
      //TODO: A/P support: https://github.com/Terracotta-OSS/terracotta-platform/issues/162
      throw new UnsupportedOperationException("Unable to route management call to server " + serverName);
    }
    return CompletableFuture.completedFuture(sharedManagementRegistry.withCapability(capabilityName)
        .call(methodName, returnType, parameters)
        .on(context)
        .build()
        .execute()
        .getSingleResult());
  }

  void init() {
    platformManagementRegistry.init();
    platformManagementRegistry.refresh();
  }

  @Override
  public void close() {
    platformManagementRegistry.close();
  }

}
