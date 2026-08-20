/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2026
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
package org.terracotta.dynamic_config.test_support;

import com.tc.classloader.BuiltinService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.model.nomad.NodeAdditionNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.NodeRemovalNomadChange;
import org.terracotta.dynamic_config.api.server.ConfigChangeHandler;
import org.terracotta.dynamic_config.api.server.ConfigChangeHandlerManager;
import org.terracotta.dynamic_config.api.server.DynamicConfigEventFiring;
import org.terracotta.dynamic_config.api.server.NomadRoutingChangeProcessor;
import org.terracotta.dynamic_config.api.server.PathResolver;
import org.terracotta.dynamic_config.api.server.SelectingConfigChangeHandler;
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.test_support.handler.GroupPortSimulateHandler;
import org.terracotta.dynamic_config.test_support.handler.LoggerOverrideConfigChangeHandler;
import org.terracotta.dynamic_config.test_support.handler.ReplicaChangeHandler;
import org.terracotta.dynamic_config.test_support.handler.ReplicaSimulationHandler;
import org.terracotta.dynamic_config.test_support.handler.SimulationHandler;
import org.terracotta.dynamic_config.test_support.processor.MyDummyNomadAdditionChangeProcessor;
import org.terracotta.dynamic_config.test_support.processor.MyDummyNomadRemovalChangeProcessor;
import org.terracotta.entity.PlatformConfiguration;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderConfiguration;
import org.terracotta.server.Server;

import java.util.Collection;
import java.util.Collections;

import static java.util.Objects.requireNonNull;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_LOGGER_OVERRIDES;
import static org.terracotta.dynamic_config.api.model.Setting.REPLICA;
import static org.terracotta.dynamic_config.test_support.handler.ReplicaSimulationHandler.SIMULATE_ACTION_TC_PROP;

@BuiltinService
public class TestServiceProvider implements ServiceProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(TestServiceProvider.class);

  @Override
  public boolean initialize(ServiceProviderConfiguration serviceProviderConfiguration, PlatformConfiguration platformConfiguration) {
    ConfigChangeHandlerManager manager = platformConfiguration.getExtendedConfiguration(ConfigChangeHandlerManager.class).iterator().next();
    NomadRoutingChangeProcessor nomadRoutingChangeProcessor = platformConfiguration.getExtendedConfiguration(NomadRoutingChangeProcessor.class).iterator().next();
    TopologyService topologyService = platformConfiguration.getExtendedConfiguration(TopologyService.class).iterator().next();
    DynamicConfigEventFiring dynamicConfigEventFiring = platformConfiguration.getExtendedConfiguration(DynamicConfigEventFiring.class).iterator().next();
    IParameterSubstitutor parameterSubstitutor = platformConfiguration.getExtendedConfiguration(IParameterSubstitutor.class).iterator().next();
    PathResolver pathResolver = platformConfiguration.getExtendedConfiguration(PathResolver.class).iterator().next();
    Server server = platformConfiguration.getExtendedConfiguration(Server.class).iterator().next();

    requireNonNull(nomadRoutingChangeProcessor);
    requireNonNull(topologyService);
    requireNonNull(dynamicConfigEventFiring);

    nomadRoutingChangeProcessor.register(
      NodeAdditionNomadChange.class,
      new MyDummyNomadAdditionChangeProcessor(topologyService, dynamicConfigEventFiring, server));

    nomadRoutingChangeProcessor.register(
      NodeRemovalNomadChange.class,
      new MyDummyNomadRemovalChangeProcessor(topologyService, dynamicConfigEventFiring, parameterSubstitutor, pathResolver, server));

    LOGGER.info("Installing: " + SimulationHandler.class.getName());
    // override the logging handler by hooking into some special properties
    // WARNING:
    // 1. We cannot access the classpath of the service containing LoggerOverrideConfigChangeHandler
    // 2. There is no ordering guarantee when service providers are loaded, so TestServiceProvider can be loaded before or after the DC service provide rregistering the real LoggerOverrideConfigChangeHandler.
    //    So this is a best effort to use the wired one, otherwise we provide teh copy.
    ConfigChangeHandler handler = manager.findConfigChangeHandler(NODE_LOGGER_OVERRIDES).orElseGet(() -> new LoggerOverrideConfigChangeHandler(topologyService));
    SelectingConfigChangeHandler<String> selectingConfigChangeHandler = new SelectingConfigChangeHandler<String>()
      .add("org.terracotta.dynamic-config.simulate", new SimulationHandler(server, topologyService))
      .add("org.terracotta.group-port.simulate", new GroupPortSimulateHandler())
      .fallback(handler)
      .selector(configuration -> {
        String key = configuration.getKey();
        LOGGER.info("Selecting handler for key: {}", key);
        return key;
      });
    // install our new handler
    manager.override(NODE_LOGGER_OVERRIDES, selectingConfigChangeHandler);

    // override the replica handler by hooking into some special properties
    // WARNING:
    // 1. We cannot access the classpath of the service containing ReplicaChangeHandler
    // 2. There is no ordering guarantee when service providers are loaded, so TestServiceProvider can be loaded before or after the DC service provide registering the real ReplicaChangeHandler.
    //    So this is a best effort to use the wired one, otherwise we provide teh copy.
    ConfigChangeHandler replicaHandler = manager.findConfigChangeHandler(REPLICA).orElseGet(() -> new ReplicaChangeHandler(server));
    SelectingConfigChangeHandler<String> selectingHandler = new SelectingConfigChangeHandler<String>()
      .add(SIMULATE_ACTION_TC_PROP, new ReplicaSimulationHandler(server, topologyService))
      .fallback(replicaHandler)
      .selector(configuration -> {
        if (topologyService.getUpcomingNodeContext().getNode().getTcProperties().orDefault().get(SIMULATE_ACTION_TC_PROP) == null) {
          return null;
        } else {
          return SIMULATE_ACTION_TC_PROP;
        }
      });
    manager.override(REPLICA, selectingHandler);
    return true;
  }

  @Override
  public <T> T getService(long l, ServiceConfiguration<T> serviceConfiguration) {
    return null;
  }

  @Override
  public Collection<Class<?>> getProvidedServiceTypes() {
    return Collections.emptyList();
  }

  @Override
  public void prepareForSynchronization() {
  }
}
