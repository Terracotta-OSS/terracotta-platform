/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.server.service.entity;

import com.tc.classloader.PermanentEntity;
import org.terracotta.dynamic_config.api.service.DynamicConfigEventService;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.entity.common.Constants;
import org.terracotta.dynamic_config.entity.common.DynamicTopologyEntityMessage;
import org.terracotta.dynamic_config.entity.common.DynamicTopologyMessageCodec;
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

@PermanentEntity(type = Constants.ENTITY_TYPE, name = Constants.ENTITY_NAME)
public class DynamicTopologyServerEntityService implements EntityServerService<DynamicTopologyEntityMessage, DynamicTopologyEntityMessage> {

  private final DynamicTopologyMessageCodec messageCodec = new DynamicTopologyMessageCodec();

  @Override
  public ActiveServerEntity<DynamicTopologyEntityMessage, DynamicTopologyEntityMessage> createActiveEntity(ServiceRegistry registry, byte[] configuration) throws ConfigurationException {
    try {
      TopologyService topologyService = registry.getService(new BasicServiceConfiguration<>(TopologyService.class));
      DynamicConfigEventService eventService = registry.getService(new BasicServiceConfiguration<>(DynamicConfigEventService.class));
      ClientCommunicator clientCommunicator = registry.getService(new BasicServiceConfiguration<>(ClientCommunicator.class));
      // In case teh server is started with the old script, returns an active entity which will fail all remote calls.
      // This entity won't be used anyway.
      return topologyService == null || eventService == null ?
          new DisabledDynamicTopologyActiveServerEntity() :
          new DynamicTopologyActiveServerEntity(topologyService, eventService, clientCommunicator);
    } catch (ServiceException e) {
      throw new ConfigurationException("Could not retrieve service ", e);
    }
  }

  @Override
  public PassiveServerEntity<DynamicTopologyEntityMessage, DynamicTopologyEntityMessage> createPassiveEntity(ServiceRegistry registry, byte[] configuration) {
    return new DynamicTopologyPassiveServerEntity();
  }

  @Override
  public DynamicTopologyMessageCodec getMessageCodec() {
    return messageCodec;
  }

  @Override
  public SyncMessageCodec<DynamicTopologyEntityMessage> getSyncMessageCodec() {
    return new SyncMessageCodec<DynamicTopologyEntityMessage>() {
      @Override
      public byte[] encode(int concurrencyKey, DynamicTopologyEntityMessage response) throws MessageCodecException {
        return getMessageCodec().encodeMessage(response);
      }

      @Override
      public DynamicTopologyEntityMessage decode(int concurrencyKey, byte[] payload) throws MessageCodecException {
        return getMessageCodec().decodeMessage(payload);
      }
    };
  }

  @Override
  public ConcurrencyStrategy<DynamicTopologyEntityMessage> getConcurrencyStrategy(byte[] configuration) {
    return new UltimateConcurrency();
  }

  @Override
  public ExecutionStrategy<DynamicTopologyEntityMessage> getExecutionStrategy(byte[] configuration) {
    return message -> ExecutionStrategy.Location.ACTIVE;
  }

  @Override
  public long getVersion() {
    return 1;
  }

  @Override
  public boolean handlesEntityType(String typeName) {
    return Constants.ENTITY_TYPE.equals(typeName);
  }
}
