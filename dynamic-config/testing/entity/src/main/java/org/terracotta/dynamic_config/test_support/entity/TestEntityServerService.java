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
import org.terracotta.dynamic_config.api.model.nomad.NodeAdditionNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.NodeRemovalNomadChange;
import org.terracotta.dynamic_config.api.service.ClusterValidator;
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.server.api.ConfigChangeHandler;
import org.terracotta.dynamic_config.server.api.ConfigChangeHandlerManager;
import org.terracotta.dynamic_config.server.api.DynamicConfigEventFiring;
import org.terracotta.dynamic_config.server.api.NomadRoutingChangeProcessor;
import org.terracotta.dynamic_config.server.api.PathResolver;
import org.terracotta.dynamic_config.server.api.SelectingConfigChangeHandler;
import org.terracotta.dynamic_config.test_support.handler.GroupPortSimulateHandler;
import org.terracotta.dynamic_config.test_support.handler.SimulationHandler;
import org.terracotta.dynamic_config.test_support.processor.MyDummyNomadAdditionChangeProcessor;
import org.terracotta.dynamic_config.test_support.processor.MyDummyNomadRemovalChangeProcessor;
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
import org.terracotta.monitoring.PlatformService;

import static java.util.Objects.requireNonNull;
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

  @SuppressWarnings("unchecked")
  protected void wireChangeHandler(ServiceRegistry serviceRegistry) {
    try {
      ConfigChangeHandlerManager manager = serviceRegistry.getService(new BasicServiceConfiguration<>(ConfigChangeHandlerManager.class));
      NomadRoutingChangeProcessor nomadRoutingChangeProcessor = serviceRegistry.getService(new BasicServiceConfiguration<>(NomadRoutingChangeProcessor.class));
      TopologyService topologyService = serviceRegistry.getService(new BasicServiceConfiguration<>(TopologyService.class));
      DynamicConfigEventFiring dynamicConfigEventFiring = serviceRegistry.getService(new BasicServiceConfiguration<>(DynamicConfigEventFiring.class));
      PlatformService platformService = serviceRegistry.getService(new BasicServiceConfiguration<>(PlatformService.class));
      IParameterSubstitutor parameterSubstitutor = serviceRegistry.getService(new BasicServiceConfiguration<>(IParameterSubstitutor.class));
      PathResolver pathResolver = serviceRegistry.getService(new BasicServiceConfiguration<>(PathResolver.class));
      ClusterValidator clusterValidator = serviceRegistry.getService(new BasicServiceConfiguration<>(ClusterValidator.class));
      requireNonNull(nomadRoutingChangeProcessor);
      requireNonNull(topologyService);
      requireNonNull(dynamicConfigEventFiring);
      requireNonNull(clusterValidator);

      nomadRoutingChangeProcessor.register(
          NodeAdditionNomadChange.class,
          new MyDummyNomadAdditionChangeProcessor(topologyService, dynamicConfigEventFiring, platformService, clusterValidator));

      nomadRoutingChangeProcessor.register(
          NodeRemovalNomadChange.class,
          new MyDummyNomadRemovalChangeProcessor(topologyService, dynamicConfigEventFiring, platformService, parameterSubstitutor, pathResolver, clusterValidator));

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
      manager.clear(NODE_LOGGER_OVERRIDES);
      manager.set(NODE_LOGGER_OVERRIDES, selectingConfigChangeHandler);
    } catch (ServiceException e) {
      throw new IllegalStateException("Failed to obtain status " + e);
    }
  }
}
