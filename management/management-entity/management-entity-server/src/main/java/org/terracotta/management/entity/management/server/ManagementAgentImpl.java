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

import org.terracotta.entity.ClientCommunicator;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.management.entity.management.ManagementAgent;
import org.terracotta.management.entity.management.ManagementCallEvent;
import org.terracotta.management.entity.management.ManagementCallReturnEvent;
import org.terracotta.management.entity.management.ManagementEvent;
import org.terracotta.management.model.call.ContextualReturn;
import org.terracotta.management.model.call.Parameter;
import org.terracotta.management.model.capabilities.Capability;
import org.terracotta.management.model.cluster.ClientIdentifier;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.context.ContextContainer;
import org.terracotta.management.model.message.DefaultMessage;
import org.terracotta.management.model.notification.ContextualNotification;
import org.terracotta.management.model.stats.ContextualStatistics;
import org.terracotta.management.sequence.SequenceGenerator;
import org.terracotta.monitoring.IMonitoringProducer;
import org.terracotta.voltron.proxy.ClientId;
import org.terracotta.voltron.proxy.server.messages.ProxyEntityResponse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import static org.terracotta.management.entity.management.server.Utils.array;

/**
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

  private final IMonitoringProducer producer;
  private final SequenceGenerator sequenceGenerator;
  private final ClientCommunicator communicator;
  private final ConcurrentMap<ClientDescriptor, ClientIdentifier> connectedClients = new ConcurrentHashMap<>();
  private final ConcurrentMap<ClientDescriptor, AtomicLong> pendingCalls = new ConcurrentHashMap<>();
  private final ConcurrentMap<ClientDescriptor, Class<Void>> haveRegistry = new ConcurrentHashMap<>();

  ManagementAgentImpl(IMonitoringProducer producer, SequenceGenerator sequenceGenerator, ClientCommunicator communicator) {
    this.producer = Objects.requireNonNull(producer, "IMonitoringProducer service is missing");
    this.sequenceGenerator = Objects.requireNonNull(sequenceGenerator, "SequenceGenerator service is missing");
    this.communicator = Objects.requireNonNull(communicator);
  }

  @Override
  public Future<ClientIdentifier> getClientIdentifier(@ClientId Object clientDescriptor) {
    ClientIdentifier clientIdentifier = findClientIdentifier((ClientDescriptor) clientDescriptor);
    return CompletableFuture.completedFuture(clientIdentifier);
  }

  @Override
  public Future<Void> pushNotification(@ClientId Object clientDescriptor, ContextualNotification notification) {
    ClientIdentifier clientIdentifier = findClientIdentifier((ClientDescriptor) clientDescriptor);
    // ensure the clientId is there
    notification.setContext(notification.getContext().with("clientId", clientIdentifier.getClientId()));
    // store in voltron tree
    fireNotif(notification);
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public Future<Void> pushStatistics(@ClientId Object clientDescriptor, ContextualStatistics... statistics) {
    if (statistics.length > 0) {
      ClientIdentifier clientIdentifier = findClientIdentifier((ClientDescriptor) clientDescriptor);
      // ensure the clientId is there
      for (ContextualStatistics statistic : statistics) {
        statistic.setContext(statistic.getContext().with("clientId", clientIdentifier.getClientId()));
      }
      // store in voltron tree
      producer.pushBestEffortsData("client-statistics", new DefaultMessage(
          sequenceGenerator.next(),
          "STATISTICS",
          statistics));
    }
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public Future<Void> exposeManagementMetadata(@ClientId Object clientDescriptor, ContextContainer contextContainer, Capability... capabilities) {
    ClientIdentifier clientIdentifier = findClientIdentifier((ClientDescriptor) clientDescriptor);
    // expose the registry into this entity tree
    String[] path = array("management", "clients", clientIdentifier.getClientId(), "registry");
    producer.addNode(array("management", "clients", clientIdentifier.getClientId()), "registry", null);
    producer.addNode(path, "contextContainer", contextContainer);
    producer.addNode(path, "capabilities", capabilities);
    // marker that we keep, saying this client has a registry
    haveRegistry.put((ClientDescriptor) clientDescriptor, Void.TYPE);
    // fire notification
    fireNotif(new ContextualNotification(Context.create("clientId", clientIdentifier.getClientId()), "CLIENT_REGISTRY_UPDATED"));
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public Future<Void> exposeTags(@ClientId Object clientDescriptor, String... tags) {
    ClientIdentifier clientIdentifier = findClientIdentifier((ClientDescriptor) clientDescriptor);
    // expose tags
    producer.addNode(array("management", "clients", clientIdentifier.getClientId()), "tags", tags == null ? new String[0] : tags);
    // fire notification
    fireNotif(new ContextualNotification(Context.create("clientId", clientIdentifier.getClientId()), "CLIENT_TAGS_UPDATED"));
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public Future<Collection<ClientIdentifier>> getManageableClients(@ClientId Object clientDescriptor) {
    return CompletableFuture.completedFuture(new ArrayList<>(connectedClients.values()));
  }

  @Override
  public Future<String> call(@ClientId Object clientDescriptor, ClientIdentifier to, Context context, String capabilityName, String methodName, Class<?> returnType, Parameter... parameters) {
    ClientIdentifier caller = findClientIdentifier((ClientDescriptor) clientDescriptor);
    ClientDescriptor toClientDescriptor = findClientDescriptor(to);
    String id = UUID.randomUUID().toString();
    context = context.with("clientId", to.getClientId());
    ManagementCallEvent event = new ManagementCallEvent(id, caller, context, capabilityName, methodName, returnType, parameters);

    if (!haveRegistry.containsKey(toClientDescriptor)) {
      throw new SecurityException("Client " + to + " is not manageable");
    }

    try {
      communicator.sendNoResponse(toClientDescriptor, ProxyEntityResponse.response(ManagementEvent.class, event));
    } catch (MessageCodecException e) {
      throw new RuntimeException(e);
    }

    // atomically increase the counter of management calls made by this client
    pendingCalls
        .computeIfAbsent((ClientDescriptor) clientDescriptor, descriptor -> new AtomicLong())
        .incrementAndGet();

    return CompletableFuture.completedFuture(id);
  }

  @Override
  public Future<Void> callReturn(@ClientId Object clientDescriptor, ClientIdentifier to, String managementCallId, ContextualReturn<?> contextualReturn) {
    ClientIdentifier caller = findClientIdentifier((ClientDescriptor) clientDescriptor);
    ClientDescriptor toClientDescriptor = findClientDescriptor(to);
    // ensure the clientId is there
    contextualReturn.setContext(contextualReturn.getContext().with("clientId", caller.getClientId()));
    // create event
    ManagementCallReturnEvent event = new ManagementCallReturnEvent(caller, managementCallId, contextualReturn);

    // atomically decrease the counter of management calls made by this client
    // check if it has a counter, and check if the counter value was 0
    if (Optional.ofNullable(pendingCalls.get(toClientDescriptor))
        .orElseThrow(() -> new SecurityException("Client " + to + " did not ask for a management call"))
        .getAndUpdate(current -> Math.max(0, current - 1)) <= 0) {
      throw new SecurityException("Client " + to + " did not ask for a management call");
    }

    try {
      communicator.sendNoResponse(toClientDescriptor, ProxyEntityResponse.response(ManagementEvent.class, event));
    } catch (MessageCodecException e) {
      throw new RuntimeException(e);
    }
    return CompletableFuture.completedFuture(null);
  }

  void connected(ClientDescriptor clientDescriptor, ClientIdentifier clientIdentifier) {
    connectedClients.put(clientDescriptor, clientIdentifier);
  }

  void disconnected(ClientDescriptor clientDescriptor) {
    connectedClients.remove(clientDescriptor);
    haveRegistry.remove(clientDescriptor);
    pendingCalls.remove(clientDescriptor);
  }

  private void fireNotif(ContextualNotification notification) {
    producer.pushBestEffortsData("client-notifications", new DefaultMessage(
        sequenceGenerator.next(),
        "NOTIFICATION",
        notification));
  }

  private ClientIdentifier findClientIdentifier(ClientDescriptor clientDescriptor) {
    return Optional.ofNullable(connectedClients.get(clientDescriptor))
        .orElseThrow(() -> new IllegalArgumentException("No client identifier found for client descriptor " + clientDescriptor));
  }

  private ClientDescriptor findClientDescriptor(ClientIdentifier to) {
    return connectedClients.entrySet()
        .stream()
        .filter(entry -> entry.getValue().equals(to))
        .map(Map.Entry::getKey)
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("No client descriptor found for client identifier " + to));
  }

}
