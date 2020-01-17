/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.test_support.entity;

import com.tc.classloader.PermanentEntity;
import com.terracottatech.dynamic_config.handler.ConfigChangeHandler;
import com.terracottatech.dynamic_config.handler.ConfigChangeHandlerManager;
import com.terracottatech.dynamic_config.handler.SelectingConfigChangeHandler;
import com.terracottatech.dynamic_config.test_support.handler.GroupPortSimulateHandler;
import com.terracottatech.dynamic_config.test_support.handler.SimulationHandler;
import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.BasicServiceConfiguration;
import org.terracotta.entity.ConcurrencyStrategy;
import org.terracotta.entity.ConfigurationException;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.EntityServerService;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.NoConcurrencyStrategy;
import org.terracotta.entity.PassiveServerEntity;
import org.terracotta.entity.ServiceException;
import org.terracotta.entity.ServiceRegistry;
import org.terracotta.entity.SyncMessageCodec;

import java.util.Optional;

import static com.terracottatech.dynamic_config.model.Setting.TC_PROPERTIES;


@PermanentEntity(type = "entity.TestSimulationEntity", names = {"TEST_ENTITY"})
public class TestEntityServerService implements EntityServerService<EntityMessage, EntityResponse> {

  private static final String ENTITY_TYPE = "entity.TestSimulationEntity";

  @Override
  public long getVersion() {
    return 1;
  }

  @Override
  public boolean handlesEntityType(String typeName) {
    return ENTITY_TYPE.equals(typeName);
  }

  @Override
  public ActiveServerEntity<EntityMessage, EntityResponse> createActiveEntity(ServiceRegistry registry, byte[] configuration) throws ConfigurationException {
    wireChangeHandler(registry);
    return new TestActiveEntity();
  }

  @Override
  public PassiveServerEntity<EntityMessage, EntityResponse> createPassiveEntity(ServiceRegistry registry, byte[] configuration) throws ConfigurationException {
    wireChangeHandler(registry);
    return new TestPassiveEntity();
  }

  @Override
  public ConcurrencyStrategy<EntityMessage> getConcurrencyStrategy(byte[] configuration) {
    return new NoConcurrencyStrategy<>();
  }

  @Override
  public MessageCodec<EntityMessage, EntityResponse> getMessageCodec() {
    return null;
  }

  @Override
  public SyncMessageCodec<EntityMessage> getSyncMessageCodec() {
    return null;
  }

  @SuppressWarnings("unchecked")
  protected void wireChangeHandler(ServiceRegistry serviceRegistry) throws ConfigurationException {
    try {
      Optional.ofNullable(
          serviceRegistry.getService(new BasicServiceConfiguration<>(ConfigChangeHandlerManager.class)))
          .ifPresent(manager -> {
            final ConfigChangeHandler handler = manager.findConfigChangeHandler(TC_PROPERTIES).get();
            if (handler instanceof SelectingConfigChangeHandler) {
              SelectingConfigChangeHandler<String> selectingConfigChangeHandler = (SelectingConfigChangeHandler<String>) handler;
              selectingConfigChangeHandler.add("com.terracottatech.dynamic-config.simulate", new SimulationHandler());
              selectingConfigChangeHandler.add("com.terracottatech.group-port.simulate", new GroupPortSimulateHandler());
            }
          });
    } catch (ServiceException e) {
      throw new IllegalStateException("Failed to obtain status " + e);
    }
  }

}
