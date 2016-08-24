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

import org.terracotta.entity.ClientCommunicator;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.management.entity.ManagementAgent;
import org.terracotta.management.entity.ManagementAgentConfig;
import org.terracotta.management.entity.ManagementCallEvent;
import org.terracotta.management.entity.ManagementCallReturnEvent;
import org.terracotta.management.entity.ManagementEvent;
import org.terracotta.management.model.call.ContextualReturn;
import org.terracotta.management.model.call.Parameter;
import org.terracotta.management.model.capabilities.Capability;
import org.terracotta.management.model.cluster.ClientIdentifier;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.context.ContextContainer;
import org.terracotta.management.model.notification.ContextualNotification;
import org.terracotta.management.model.stats.ContextualStatistics;
import org.terracotta.management.sequence.SequenceGenerator;
import org.terracotta.management.service.monitoring.IMonitoringConsumer;
import org.terracotta.management.service.monitoring.IMonitoringProducer;
import org.terracotta.voltron.proxy.ClientId;
import org.terracotta.voltron.proxy.server.messages.ProxyEntityResponse;

import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.logging.Logger;

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
 * Buffers:
 * <ul>
 * <li>{@code client-statistics [byte[] sequence, ContextualStatistics[]]>}</li>
 * <li>{@code client-notifications [byte[] sequence, ContextualNotification]>}</li>
 * </ul>
 *
 * @author Mathieu Carbou
 */
class ManagementAgentImpl implements ManagementAgent {

  private static final Logger LOGGER = Logger.getLogger(ManagementAgentImpl.class.getName());

  private final ManagementAgentConfig config;
  private final IMonitoringProducer producer;
  private final IMonitoringConsumer consumer;
  private final SequenceGenerator sequenceGenerator;
  private final ClientCommunicator communicator;

  ManagementAgentImpl(ManagementAgentConfig config, IMonitoringConsumer consumer, IMonitoringProducer producer, SequenceGenerator sequenceGenerator, ClientCommunicator communicator) {
    this.config = config;
    this.producer = Objects.requireNonNull(producer, "IMonitoringProducer service is missing");
    this.consumer = Objects.requireNonNull(consumer, "IMonitoringConsumer service is missing");
    this.sequenceGenerator = Objects.requireNonNull(sequenceGenerator, "SequenceGenerator service is missing");
    this.communicator = Objects.requireNonNull(communicator);
  }

  @Override
  public Future<ClientIdentifier> getClientIdentifier(@ClientId Object clientDescriptor) {
    return CompletableFuture.completedFuture(Utils.getClientIdentifier(consumer, clientDescriptor).get());
  }

  @Override
  public Future<Void> pushNotification(@ClientId Object clientDescriptor, ContextualNotification notification) {
    Utils.getClientIdentifier(consumer, clientDescriptor).ifPresent(clientIdentifier -> {
      // ensure the clientId is there
      notification.setContext(notification.getContext().with("clientId", clientIdentifier.getClientId()));
      // store in voltron tree
      Serializable[] o = new Serializable[]{sequenceGenerator.next().toBytes(), notification};
      producer.pushBestEffortsData("client-notifications", o);
    });
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public Future<Void> pushStatistics(@ClientId Object clientDescriptor, ContextualStatistics... statistics) {
    if (statistics.length > 0) {
      Utils.getClientIdentifier(consumer, clientDescriptor).ifPresent(clientIdentifier -> {
        // ensure the clientId is there
        for (ContextualStatistics statistic : statistics) {
          statistic.setContext(statistic.getContext().with("clientId", clientIdentifier.getClientId()));
        }
        // store in voltron tree
        Serializable[] o = new Serializable[]{sequenceGenerator.next().toBytes(), statistics};
        producer.pushBestEffortsData("client-statistics", o);
      });
    }
    return CompletableFuture.completedFuture(null);
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

  @Override
  public Future<Collection<ClientIdentifier>> getManageableClients(@ClientId Object clientDescriptor) {
    return CompletableFuture.completedFuture(Utils.getManageableClients(consumer));
  }

  @Override
  public Future<String> call(@ClientId Object clientDescriptor, ClientIdentifier to, Context context, String capabilityName, String methodName, Class<?> returnType, Parameter... parameters) {
    ClientIdentifier caller = Utils.getClientIdentifier(consumer, clientDescriptor).orElseThrow(() -> new IllegalStateException("Unable to get client identifier for client descriptor " + clientDescriptor));
    ClientDescriptor toClientDescriptor = Utils.getClientDescriptor(consumer, to).orElseThrow(() -> new IllegalStateException("Target not found " + to));
    String id = UUID.randomUUID().toString();
    context = context.with("clientId", to.getClientId());
    ManagementCallEvent event = new ManagementCallEvent(id, caller, context, capabilityName, methodName, returnType, parameters);
    securityCheck(to, toClientDescriptor, event);
    try {
      communicator.sendNoResponse(toClientDescriptor, ProxyEntityResponse.response(ManagementEvent.class, event));
    } catch (MessageCodecException e) {
      throw new RuntimeException(e);
    }
    return CompletableFuture.completedFuture(id);
  }

  @Override
  public Future<Void> callReturn(@ClientId Object clientDescriptor, ClientIdentifier to, String managementCallId, ContextualReturn<?> contextualReturn) {
    Utils.getClientIdentifier(consumer, clientDescriptor).ifPresent(caller -> {
      Utils.getClientDescriptor(consumer, to).ifPresent(toClientDescriptor -> {
        // ensure the clientId is there
        contextualReturn.setContext(contextualReturn.getContext().with("clientId", caller.getClientId()));
        // create event
        ManagementCallReturnEvent event = new ManagementCallReturnEvent(caller, managementCallId, contextualReturn);
        securityCheck(to, toClientDescriptor, event);
        try {
          communicator.sendNoResponse(toClientDescriptor, ProxyEntityResponse.response(ManagementEvent.class, event));
        } catch (MessageCodecException e) {
          throw new RuntimeException(e);
        }
      });
    });
    return CompletableFuture.completedFuture(null);
  }

  private void securityCheck(ClientIdentifier to, ClientDescriptor toClientDescriptor, ManagementEvent event) throws SecurityException {
    //TODO: MATHIEU: Security checks for management calls (https://github.com/Terracotta-OSS/terracotta-platform/issues/115)
    if (!Utils.isManageableClient(consumer, to)) {
      throw new SecurityException("Client " + to + " cannot be targeted");
    }
  }

}
