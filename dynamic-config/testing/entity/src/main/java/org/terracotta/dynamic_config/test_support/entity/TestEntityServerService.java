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
package org.terracotta.dynamic_config.test_support.entity;

import com.tc.classloader.PermanentEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.server.api.ConfigChangeHandler;
import org.terracotta.dynamic_config.server.api.ConfigChangeHandlerManager;
import org.terracotta.dynamic_config.server.api.SelectingConfigChangeHandler;
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
