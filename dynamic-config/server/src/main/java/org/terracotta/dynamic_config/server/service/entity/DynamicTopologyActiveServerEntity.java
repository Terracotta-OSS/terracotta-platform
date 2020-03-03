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
package org.terracotta.dynamic_config.server.service.entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Configuration;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.nomad.SettingNomadChange;
import org.terracotta.dynamic_config.api.service.DynamicConfigEventService;
import org.terracotta.dynamic_config.api.service.DynamicConfigListenerAdapter;
import org.terracotta.dynamic_config.api.service.EventRegistration;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.entity.common.DynamicTopologyEntityMessage;
import org.terracotta.entity.ActiveInvokeContext;
import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.ClientCommunicator;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.ConfigurationException;
import org.terracotta.entity.EntityUserException;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.entity.PassiveSynchronizationChannel;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;
import static org.terracotta.dynamic_config.entity.common.DynamicTopologyEntityMessage.Type.EVENT_NODE_ADDITION;
import static org.terracotta.dynamic_config.entity.common.DynamicTopologyEntityMessage.Type.EVENT_NODE_REMOVAL;
import static org.terracotta.dynamic_config.entity.common.DynamicTopologyEntityMessage.Type.EVENT_SETTING_CHANGED;


public class DynamicTopologyActiveServerEntity implements ActiveServerEntity<DynamicTopologyEntityMessage, DynamicTopologyEntityMessage> {
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
  public void synchronizeKeyToPassive(PassiveSynchronizationChannel<DynamicTopologyEntityMessage> syncChannel, int concurrencyKey) {
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
  public DynamicTopologyEntityMessage invokeActive(ActiveInvokeContext<DynamicTopologyEntityMessage> context, DynamicTopologyEntityMessage message) throws EntityUserException {
    LOGGER.trace("invokeActive({})", message);
    switch (message.getType()) {
      case REQ_UPCOMING_CLUSTER: {
        return new DynamicTopologyEntityMessage(message.getType(), topologyService.getUpcomingNodeContext().getCluster());
      }
      case REQ_RUNTIME_CLUSTER: {
        return new DynamicTopologyEntityMessage(message.getType(), topologyService.getRuntimeNodeContext().getCluster());
      }
      case REQ_MUST_BE_RESTARTED: {
        return new DynamicTopologyEntityMessage(message.getType(), topologyService.mustBeRestarted());
      }
      case REQ_HAS_INCOMPLETE_CHANGE: {
        return new DynamicTopologyEntityMessage(message.getType(), topologyService.hasIncompleteChange());
      }
      case REQ_LICENSE: {
        return new DynamicTopologyEntityMessage(message.getType(), topologyService.getLicense().orElse(null));
      }
      default:
        throw new AssertionError(message);
    }
  }

  private void listen() {
    if (eventRegistration == null) {
      eventRegistration = eventService.register(new DynamicConfigListenerAdapter() {
        @Override
        public void onNodeAddition(int stripeId, Node addedNode) {
          fire(new DynamicTopologyEntityMessage(EVENT_NODE_ADDITION, new Object[]{stripeId, addedNode}));
        }

        @Override
        public void onNodeRemoval(int stripeId, Node removedNode) {
          fire(new DynamicTopologyEntityMessage(EVENT_NODE_REMOVAL, new Object[]{stripeId, removedNode}));
        }

        @Override
        public void onSettingChanged(SettingNomadChange change, Cluster updated) {
          Configuration configuration = change.toConfiguration(updated);
          fire(new DynamicTopologyEntityMessage(EVENT_SETTING_CHANGED, new Object[]{configuration, updated}));
        }
      });
    }
  }

  private void fire(DynamicTopologyEntityMessage msg) {
    if (!clients.isEmpty()) {
      LOGGER.trace("fire({})", msg);
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
