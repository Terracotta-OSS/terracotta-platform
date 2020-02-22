/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.test_support.entity;

import com.tc.classloader.PermanentEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.service.ConfigChangeHandler;
import org.terracotta.dynamic_config.api.service.ConfigChangeHandlerManager;
import org.terracotta.dynamic_config.api.service.SelectingConfigChangeHandler;
import org.terracotta.dynamic_config.test_support.handler.GroupPortSimulateHandler;
import org.terracotta.dynamic_config.test_support.handler.SimulationHandler;
import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.BasicServiceConfiguration;
import org.terracotta.entity.ConcurrencyStrategy;
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

import static org.terracotta.dynamic_config.api.model.Setting.NODE_LOGGER_OVERRIDES;


@PermanentEntity(type = "entity.TestSimulationEntity", names = {"TEST_ENTITY"})
public class TestEntityServerService implements EntityServerService<EntityMessage, EntityResponse> {

  private static final Logger LOGGER = LoggerFactory.getLogger(TestEntityServerService.class);

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
  public ActiveServerEntity<EntityMessage, EntityResponse> createActiveEntity(ServiceRegistry registry, byte[] configuration) {
    wireChangeHandler(registry);
    return new TestActiveEntity();
  }

  @Override
  public PassiveServerEntity<EntityMessage, EntityResponse> createPassiveEntity(ServiceRegistry registry, byte[] configuration) {
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

  protected void wireChangeHandler(ServiceRegistry serviceRegistry) {
    try {
      Optional.ofNullable(
          serviceRegistry.getService(new BasicServiceConfiguration<>(ConfigChangeHandlerManager.class)))
          .ifPresent(manager -> {
            LOGGER.info("Installing: " + SimulationHandler.class.getName());
            ConfigChangeHandler handler = manager.findConfigChangeHandler(NODE_LOGGER_OVERRIDES).get();
            // override the logging handler by hooking into some special properties
            SelectingConfigChangeHandler<String> selectingConfigChangeHandler = new SelectingConfigChangeHandler<String>()
                .add("org.terracotta.dynamic-config.simulate", new SimulationHandler())
                .add("org.terracotta.group-port.simulate", new GroupPortSimulateHandler())
                .fallback(handler)
                .selector(configuration -> {
                  String key = configuration.getKey();
                  LOGGER.info("Selecting handler for key: {}", key);
                  return key;
                });
            // install our new handler
            manager.remove(NODE_LOGGER_OVERRIDES);
            manager.add(NODE_LOGGER_OVERRIDES, selectingConfigChangeHandler);
          });
    } catch (ServiceException e) {
      throw new IllegalStateException("Failed to obtain status " + e);
    }
  }
}
