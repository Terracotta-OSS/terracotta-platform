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
package org.terracotta.dynamic_config.entity.topology.server;

import com.tc.classloader.PermanentEntity;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.entity.topology.common.Codec;
import org.terracotta.dynamic_config.entity.topology.common.DynamicTopologyEntityConstants;
import org.terracotta.dynamic_config.entity.topology.common.Message;
import org.terracotta.dynamic_config.entity.topology.common.Response;
import org.terracotta.dynamic_config.server.api.DynamicConfigEventService;
import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.BasicServiceConfiguration;
import org.terracotta.entity.ClientCommunicator;
import org.terracotta.entity.ConcurrencyStrategy;
import org.terracotta.entity.ConfigurationException;
import org.terracotta.entity.EntityServerService;
import org.terracotta.entity.ExecutionStrategy;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.entity.PassiveServerEntity;
import org.terracotta.entity.ServiceException;
import org.terracotta.entity.ServiceRegistry;
import org.terracotta.entity.SyncMessageCodec;

@PermanentEntity(type = DynamicTopologyEntityConstants.ENTITY_TYPE, name = DynamicTopologyEntityConstants.ENTITY_NAME)
public class DynamicTopologyServerEntityService implements EntityServerService<Message, Response> {

  private final Codec messageCodec = new Codec();

  @Override
  public ActiveServerEntity<Message, Response> createActiveEntity(ServiceRegistry registry, byte[] configuration) throws ConfigurationException {
    try {
      TopologyService topologyService = registry.getService(new BasicServiceConfiguration<>(TopologyService.class));
      DynamicConfigEventService eventService = registry.getService(new BasicServiceConfiguration<>(DynamicConfigEventService.class));
      ClientCommunicator clientCommunicator = registry.getService(new BasicServiceConfiguration<>(ClientCommunicator.class));
      // In case the server is started with the old script, returns an active entity which will fail all remote calls.
      // This entity won't be used anyway.
      if (topologyService == null || eventService == null) {
        throw new AssertionError("Server started without dynamic config");
      }
      return new DynamicTopologyActiveServerEntity(topologyService, eventService, clientCommunicator);
    } catch (ServiceException e) {
      throw new ConfigurationException("Could not retrieve service ", e);
    }
  }

  @Override
  public PassiveServerEntity<Message, Response> createPassiveEntity(ServiceRegistry registry, byte[] configuration) {
    return new DynamicTopologyPassiveServerEntity();
  }

  @Override
  public Codec getMessageCodec() {
    return messageCodec;
  }

  @Override
  public SyncMessageCodec<Message> getSyncMessageCodec() {
    return new SyncMessageCodec<Message>() {
      @Override
      public byte[] encode(int concurrencyKey, Message response) throws MessageCodecException {
        return getMessageCodec().encodeMessage(response);
      }

      @Override
      public Message decode(int concurrencyKey, byte[] payload) throws MessageCodecException {
        return getMessageCodec().decodeMessage(payload);
      }
    };
  }

  @Override
  public ConcurrencyStrategy<Message> getConcurrencyStrategy(byte[] configuration) {
    return new UltimateConcurrency();
  }

  @Override
  public ExecutionStrategy<Message> getExecutionStrategy(byte[] configuration) {
    return message -> ExecutionStrategy.Location.ACTIVE;
  }

  @Override
  public long getVersion() {
    return 1;
  }

  @Override
  public boolean handlesEntityType(String typeName) {
    return DynamicTopologyEntityConstants.ENTITY_TYPE.equals(typeName);
  }
}
