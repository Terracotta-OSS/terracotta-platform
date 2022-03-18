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
package org.terracotta.dynamic_config.server.service;

import com.tc.classloader.BuiltinService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.model.Configuration;
import org.terracotta.dynamic_config.api.model.Setting;
import org.terracotta.dynamic_config.api.service.DynamicConfigService;
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.server.api.ConfigChangeHandler;
import org.terracotta.dynamic_config.server.api.ConfigChangeHandlerManager;
import org.terracotta.dynamic_config.server.api.DynamicConfigEventFiring;
import org.terracotta.dynamic_config.server.api.DynamicConfigEventService;
import org.terracotta.dynamic_config.server.api.LicenseService;
import org.terracotta.dynamic_config.server.api.NomadPermissionChangeProcessor;
import org.terracotta.dynamic_config.server.api.NomadRoutingChangeProcessor;
import org.terracotta.dynamic_config.server.api.PathResolver;
import org.terracotta.dynamic_config.server.api.SelectingConfigChangeHandler;
import org.terracotta.dynamic_config.server.service.handler.ClientReconnectWindowConfigChangeHandler;
import org.terracotta.dynamic_config.server.service.handler.LoggerOverrideConfigChangeHandler;
import org.terracotta.dynamic_config.server.service.handler.NodeLogDirChangeHandler;
import org.terracotta.entity.PlatformConfiguration;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderConfiguration;
import org.terracotta.nomad.server.NomadServer;
import org.terracotta.nomad.server.UpgradableNomadServer;

import java.util.Arrays;
import java.util.Collection;

import static org.terracotta.dynamic_config.api.model.Setting.CLIENT_RECONNECT_WINDOW;
import static org.terracotta.dynamic_config.api.model.Setting.CLUSTER_NAME;
import static org.terracotta.dynamic_config.api.model.Setting.LOCK_CONTEXT;
import static org.terracotta.dynamic_config.api.model.Setting.FAILOVER_PRIORITY;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_LOGGER_OVERRIDES;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_LOG_DIR;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_PUBLIC_HOSTNAME;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_PUBLIC_PORT;
import static org.terracotta.dynamic_config.api.model.Setting.TC_PROPERTIES;
import static org.terracotta.dynamic_config.server.api.ConfigChangeHandler.accept;

@BuiltinService
public class DynamicConfigServiceProvider implements ServiceProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(DynamicConfigServiceProvider.class);

  private volatile PlatformConfiguration platformConfiguration;

  @Override
  public boolean initialize(ServiceProviderConfiguration configuration, PlatformConfiguration platformConfiguration) {
    this.platformConfiguration = platformConfiguration;

    IParameterSubstitutor parameterSubstitutor = findService(platformConfiguration, IParameterSubstitutor.class);
    PathResolver pathResolver = findService(platformConfiguration, PathResolver.class);
    ConfigChangeHandlerManager configChangeHandlerManager = findService(platformConfiguration, ConfigChangeHandlerManager.class);
    TopologyService topologyService = findService(platformConfiguration, TopologyService.class);

    // client-reconnect-window
    ConfigChangeHandler clientReconnectWindowHandler = new ClientReconnectWindowConfigChangeHandler();
    addToManager(configChangeHandlerManager, clientReconnectWindowHandler, CLIENT_RECONNECT_WINDOW);

    // log-dir
    ConfigChangeHandler nodeLogDirChangeHandler = new NodeLogDirChangeHandler(parameterSubstitutor, pathResolver);
    addToManager(configChangeHandlerManager, nodeLogDirChangeHandler, NODE_LOG_DIR);

    // settings applied directly without any config handler but which require a restart
    addToManager(configChangeHandlerManager, accept(), FAILOVER_PRIORITY);

    // public hostname/port
    addToManager(configChangeHandlerManager, accept(), NODE_PUBLIC_HOSTNAME);
    addToManager(configChangeHandlerManager, accept(), NODE_PUBLIC_PORT);

    // cluster name
    addToManager(configChangeHandlerManager, accept(), CLUSTER_NAME);

    // lock context
    addToManager(configChangeHandlerManager, accept(), LOCK_CONTEXT);

    // tc-logging
    LoggerOverrideConfigChangeHandler loggerOverrideConfigChangeHandler = new LoggerOverrideConfigChangeHandler(topologyService);
    addToManager(configChangeHandlerManager, loggerOverrideConfigChangeHandler, NODE_LOGGER_OVERRIDES);

    // tc-properties
    configChangeHandlerManager.set(TC_PROPERTIES, new SelectingConfigChangeHandler<String>()
        .selector(Configuration::getKey)
        .fallback(accept()));

    // initialize the config handlers that need do to something at startup
    loggerOverrideConfigChangeHandler.init();

    NomadPermissionChangeProcessor permissions = findService(platformConfiguration, NomadPermissionChangeProcessor.class);
    permissions.addCheck(new DisallowSettingChanges());
    permissions.addCheck(new ServerStateCheck());

    return true;
  }

  @Override
  public <T> T getService(long consumerID, ServiceConfiguration<T> configuration) {
    return findService(platformConfiguration, configuration.getServiceType());
  }

  @Override
  public Collection<Class<?>> getProvidedServiceTypes() {
    return Arrays.asList(
        IParameterSubstitutor.class,
        ConfigChangeHandlerManager.class,
        DynamicConfigEventService.class,
        TopologyService.class,
        DynamicConfigService.class,
        DynamicConfigEventFiring.class,
        NomadServer.class,
        UpgradableNomadServer.class,
        NomadRoutingChangeProcessor.class,
        NomadPermissionChangeProcessor.class,
        LicenseService.class,
        PathResolver.class
    );
  }

  @Override
  public void prepareForSynchronization() {
    // no-op
  }

  private void addToManager(ConfigChangeHandlerManager manager, ConfigChangeHandler configChangeHandler, Setting setting) {
    ConfigChangeHandler old = manager.set(setting, configChangeHandler);
    if (old != null) {
      LOGGER.warn("Default dynamic configuration change handler {} for setting {} has been override by {}", configChangeHandler, setting, old);
    }
  }

  private <T> T findService(PlatformConfiguration platformConfiguration, Class<T> type) {
    if (!getProvidedServiceTypes().contains(type)) {
      throw new IllegalArgumentException(String.valueOf(type));
    }
    final Collection<T> services = platformConfiguration.getExtendedConfiguration(type);
    if (services.isEmpty()) {
      throw new IllegalArgumentException("No instance of service " + type + " found");
    }

    if (services.size() == 1) {
      T instance = services.iterator().next();
      if (instance == null) {
        throw new IllegalArgumentException("Instance of service " + type + " found to be null");
      }
      return instance;
    }
    throw new IllegalArgumentException("Multiple instances of service " + type + " found");
  }
}
