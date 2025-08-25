/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
package org.terracotta.dynamic_config.entity.topology.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Configuration;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.UID;
import org.terracotta.dynamic_config.api.model.nomad.SettingNomadChange;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.entity.topology.common.Message;
import org.terracotta.dynamic_config.entity.topology.common.Response;
import org.terracotta.dynamic_config.server.api.DynamicConfigEventService;
import org.terracotta.dynamic_config.server.api.DynamicConfigListener;
import org.terracotta.dynamic_config.server.api.EventRegistration;
import org.terracotta.entity.ActiveInvokeContext;
import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.ClientCommunicator;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.ConfigurationException;
import org.terracotta.entity.EntityUserException;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.entity.PassiveSynchronizationChannel;
import org.terracotta.entity.StateDumpCollector;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.terracotta.dynamic_config.entity.topology.common.Type.EVENT_NODE_ADDITION;
import static org.terracotta.dynamic_config.entity.topology.common.Type.EVENT_NODE_REMOVAL;
import static org.terracotta.dynamic_config.entity.topology.common.Type.EVENT_SETTING_CHANGED;
import static org.terracotta.dynamic_config.entity.topology.common.Type.EVENT_STRIPE_ADDITION;
import static org.terracotta.dynamic_config.entity.topology.common.Type.EVENT_STRIPE_REMOVAL;


public class DynamicTopologyActiveServerEntity implements ActiveServerEntity<Message, Response> {
  private static final Logger LOGGER = LoggerFactory.getLogger(DynamicTopologyActiveServerEntity.class);

  private final TopologyService topologyService;
  private final DynamicConfigEventService eventService;
  private final ClientCommunicator clientCommunicator;
  private final Collection<ClientDescriptor> clients = ConcurrentHashMap.newKeySet();

  private volatile EventRegistration eventRegistration;

  public DynamicTopologyActiveServerEntity(TopologyService topologyService, DynamicConfigEventService eventService, ClientCommunicator clientCommunicator) {
    this.topologyService = requireNonNull(topologyService);
    this.eventService = requireNonNull(eventService);
    this.clientCommunicator = requireNonNull(clientCommunicator);
  }

  @Override
  public void loadExisting() {
  }

  @Override
  public void connected(ClientDescriptor clientDescriptor) {
    clients.add(clientDescriptor);
  }

  @Override
  public void disconnected(ClientDescriptor clientDescriptor) {
    clients.remove(clientDescriptor);
  }

  @Override
  public void synchronizeKeyToPassive(PassiveSynchronizationChannel<Message> syncChannel, int concurrencyKey) {
  }

  @Override
  public void createNew() throws ConfigurationException {
    listen();
  }

  @Override
  public void destroy() {
    if (eventRegistration != null) {
      eventRegistration.unregister();
      eventRegistration = null;
    }
  }

  @Override
  public ReconnectHandler startReconnect() {
    return (clientDescriptor, extendedReconnectData) -> {
    };
  }

  @Override
  public Response invokeActive(ActiveInvokeContext<Response> context, Message message) throws EntityUserException {
    LOGGER.trace("invokeActive({})", message);
    switch (message.getType()) {
      case REQ_UPCOMING_CLUSTER: {
        return new Response(message.getType(), topologyService.getUpcomingNodeContext().getCluster());
      }
      case REQ_RUNTIME_CLUSTER: {
        return new Response(message.getType(), topologyService.getRuntimeNodeContext().getCluster());
      }
      case REQ_MUST_BE_RESTARTED: {
        return new Response(message.getType(), topologyService.mustBeRestarted());
      }
      case REQ_HAS_INCOMPLETE_CHANGE: {
        return new Response(message.getType(), topologyService.hasIncompleteChange());
      }
      case REQ_LICENSE: {
        return new Response(message.getType(), topologyService.getLicense().orElse(null));
      }
      default:
        throw new AssertionError(message);
    }
  }

  @Override
  public void addStateTo(StateDumpCollector stateDumpCollector) {
    stateDumpCollector.addState("clients", clients.stream().map(Object::toString).collect(toList()));
  }

  private void listen() {
    if (eventRegistration == null) {
      eventRegistration = eventService.register(new DynamicConfigListener() {
        @Override
        public void onNodeAddition(UID stripeUID, Node addedNode) {
          Cluster cluster = topologyService.getRuntimeNodeContext().getCluster();
          cluster.getStripe(stripeUID).get().addNode(addedNode);
          fire(new Response(EVENT_NODE_ADDITION, asList(cluster, addedNode.getUID())));
        }

        @Override
        public void onNodeRemoval(UID stripeUID, Node removedNode) {
          Cluster cluster = topologyService.getRuntimeNodeContext().getCluster();
          cluster.getStripe(stripeUID).get().removeNode(removedNode.getUID());
          fire(new Response(EVENT_NODE_REMOVAL, asList(cluster, stripeUID, removedNode)));
        }

        @Override
        public void onStripeAddition(Stripe addedStripe) {
          Cluster cluster = topologyService.getRuntimeNodeContext().getCluster();
          cluster.addStripe(addedStripe);
          fire(new Response(EVENT_STRIPE_ADDITION, asList(cluster, addedStripe.getUID())));
        }

        @Override
        public void onStripeRemoval(Stripe removedStripe) {
          Cluster cluster = topologyService.getRuntimeNodeContext().getCluster();
          cluster.removeStripe(removedStripe.getUID());
          fire(new Response(EVENT_STRIPE_REMOVAL, asList(cluster, removedStripe)));
        }

        @Override
        public void onSettingChanged(SettingNomadChange change, Cluster updated) {
          Configuration configuration = change.toConfiguration(updated);
          fire(new Response(EVENT_SETTING_CHANGED, asList(updated, configuration)));
        }
      });
    }
  }

  private void fire(Response msg) {
    if (!clients.isEmpty()) {
      LOGGER.trace("fire({}): clients: {}", msg, clients);
      for (ClientDescriptor client : clients) {
        try {
          clientCommunicator.sendNoResponse(client, msg);
        } catch (MessageCodecException e) {
          throw new AssertionError(e); // should never occur
        }
      }
    }
  }
}
