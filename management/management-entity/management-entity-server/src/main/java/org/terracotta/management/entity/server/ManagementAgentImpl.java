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
package org.terracotta.management.entity.server;

import org.terracotta.management.entity.ManagementAgent;
import org.terracotta.management.entity.ManagementAgentConfig;
import org.terracotta.management.model.capabilities.Capability;
import org.terracotta.management.model.cluster.ClientIdentifier;
import org.terracotta.management.model.context.ContextContainer;
import org.terracotta.management.service.monitoring.IMonitoringConsumer;
import org.terracotta.monitoring.IMonitoringProducer;
import org.terracotta.voltron.proxy.ClientId;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static org.terracotta.management.entity.server.Utils.array;

/**
 * Consumes:
 * <ul>
 * <li>{@code platform/clients/<id> PlatformConnectedClient}</li>
 * <li>{@code platform/fetched/<id> PlatformClientFetchedEntity}</li>
 * <li>{@code platform/entities/<id> PlatformEntity}</li>
 * </ul>
 * Produces:
 * <ul>
 * <li>{@code management/clients/<client-identifier>/tags String[]}</li>
 * <li>{@code management/clients/<client-identifier>/registry}</li>
 * <li>{@code management/clients/<client-identifier>/registry/contextContainer ContextContainer}</li>
 * <li>{@code management/clients/<client-identifier>/registry/capabilities Capability[]}</li>
 * </ul>
 *
 * @author Mathieu Carbou
 */
class ManagementAgentImpl implements ManagementAgent {

  private final ManagementAgentConfig config;
  private final IMonitoringProducer producer;
  private final IMonitoringConsumer consumer;

  ManagementAgentImpl(ManagementAgentConfig config, IMonitoringConsumer consumer, IMonitoringProducer producer) {
    this.config = config;
    this.producer = Objects.requireNonNull(producer, "IMonitoringProducer service is missing");
    this.consumer = Objects.requireNonNull(consumer, "IMonitoringConsumer service is missing");
  }

  @Override
  public Future<ClientIdentifier> getClientIdentifier(@ClientId Object clientDescriptor) {
    return CompletableFuture.completedFuture(Utils.getClientIdentifier(consumer, clientDescriptor).get());
  }

  @Override
  public Future<Void> exposeManagementMetadata(@ClientId Object clientDescriptor, ContextContainer contextContainer, Capability... capabilities) {
    Utils.getClientIdentifier(consumer, clientDescriptor).ifPresent(clientIdentifier -> {
      producer.addNode(array("management", "clients", clientIdentifier.getClientId(), "registry"), "contextContainer", contextContainer);
      producer.addNode(array("management", "clients", clientIdentifier.getClientId(), "registry"), "capabilities", capabilities);
    });
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public Future<Void> exposeTags(@ClientId Object clientDescriptor, String... tags) {
    Utils.getClientIdentifier(consumer, clientDescriptor).ifPresent(clientIdentifier ->
        producer.addNode(array("management", "clients", clientIdentifier.getClientId()), "tags", tags == null ? new String[0] : tags));
    return CompletableFuture.completedFuture(null);
  }

}
