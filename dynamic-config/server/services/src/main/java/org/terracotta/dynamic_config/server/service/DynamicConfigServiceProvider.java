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
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Setting;
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.server.api.ConfigChangeHandler;
import org.terracotta.dynamic_config.server.api.ConfigChangeHandlerManager;
import org.terracotta.dynamic_config.server.api.DynamicConfigEventService;
import org.terracotta.dynamic_config.server.api.SelectingConfigChangeHandler;
import org.terracotta.dynamic_config.server.service.handler.ClientReconnectWindowConfigChangeHandler;
import org.terracotta.dynamic_config.server.service.handler.LoggerOverrideConfigChangeHandler;
import org.terracotta.dynamic_config.server.service.handler.ServerAttributeConfigChangeHandler;
import org.terracotta.entity.PlatformConfiguration;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderConfiguration;
import org.terracotta.nomad.server.NomadServer;

import java.util.Arrays;
import java.util.Collection;

import static org.terracotta.dynamic_config.api.model.Setting.CLIENT_RECONNECT_WINDOW;
import static org.terracotta.dynamic_config.api.model.Setting.FAILOVER_PRIORITY;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_BIND_ADDRESS;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_GROUP_BIND_ADDRESS;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_GROUP_PORT;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_HOSTNAME;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_LOGGER_OVERRIDES;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_LOG_DIR;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_METADATA_DIR;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_NAME;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_PORT;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_PUBLIC_HOSTNAME;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_PUBLIC_PORT;
import static org.terracotta.dynamic_config.api.model.Setting.SECURITY_AUDIT_LOG_DIR;
import static org.terracotta.dynamic_config.api.model.Setting.SECURITY_AUTHC;
import static org.terracotta.dynamic_config.api.model.Setting.SECURITY_DIR;
import static org.terracotta.dynamic_config.api.model.Setting.SECURITY_SSL_TLS;
import static org.terracotta.dynamic_config.api.model.Setting.SECURITY_WHITELIST;
import static org.terracotta.dynamic_config.api.model.Setting.TC_PROPERTIES;
import static org.terracotta.dynamic_config.server.api.ConfigChangeHandler.accept;
import static org.terracotta.dynamic_config.server.api.ConfigChangeHandler.reject;

@BuiltinService
public class DynamicConfigServiceProvider implements ServiceProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(DynamicConfigServiceProvider.class);

  private volatile IParameterSubstitutor parameterSubstitutor;
  private volatile ConfigChangeHandlerManager configChangeHandlerManager;
  private volatile DynamicConfigEventService dynamicConfigEventService;
  private volatile TopologyService topologyService;
  private volatile NomadServer<NodeContext> nomadServer;

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Override
  public boolean initialize(ServiceProviderConfiguration configuration, PlatformConfiguration platformConfiguration) {
    parameterSubstitutor = find(platformConfiguration, IParameterSubstitutor.class);
    configChangeHandlerManager = find(platformConfiguration, ConfigChangeHandlerManager.class);
    dynamicConfigEventService = find(platformConfiguration, DynamicConfigEventService.class);
    topologyService = find(platformConfiguration, TopologyService.class);
    nomadServer = find(platformConfiguration, (Class<NomadServer<NodeContext>>) (Class) NomadServer.class);

    // If the server is started without the startup manager, with the old script but not with not start-node.sh, then the diagnostic services won't be there.
    if (configChangeHandlerManager != null) {

      // client-reconnect-window
      ConfigChangeHandler clientReconnectWindowHandler = new ClientReconnectWindowConfigChangeHandler();
      addToManager(configChangeHandlerManager, clientReconnectWindowHandler, CLIENT_RECONNECT_WINDOW);

      // server attributes
      ConfigChangeHandler serverAttributeConfigChangeHandler = new ServerAttributeConfigChangeHandler();
      addToManager(configChangeHandlerManager, serverAttributeConfigChangeHandler, NODE_LOG_DIR);
      addToManager(configChangeHandlerManager, serverAttributeConfigChangeHandler, NODE_BIND_ADDRESS);
      addToManager(configChangeHandlerManager, serverAttributeConfigChangeHandler, NODE_GROUP_BIND_ADDRESS);

      // settings applied directly without any config handler but which require a restart
      addToManager(configChangeHandlerManager, accept(), FAILOVER_PRIORITY);
      addToManager(configChangeHandlerManager, accept(), NODE_GROUP_PORT);
      addToManager(configChangeHandlerManager, accept(), SECURITY_DIR);
      addToManager(configChangeHandlerManager, accept(), SECURITY_AUDIT_LOG_DIR);
      addToManager(configChangeHandlerManager, accept(), SECURITY_AUTHC);
      addToManager(configChangeHandlerManager, accept(), SECURITY_SSL_TLS);
      addToManager(configChangeHandlerManager, accept(), SECURITY_WHITELIST);

      // public hostname/port
      addToManager(configChangeHandlerManager, accept(), NODE_PUBLIC_HOSTNAME);
      addToManager(configChangeHandlerManager, accept(), NODE_PUBLIC_PORT);

      // tc-logging
      LoggerOverrideConfigChangeHandler loggerOverrideConfigChangeHandler = new LoggerOverrideConfigChangeHandler(topologyService);
      addToManager(configChangeHandlerManager, loggerOverrideConfigChangeHandler, NODE_LOGGER_OVERRIDES);

      // tc-properties
      configChangeHandlerManager.add(TC_PROPERTIES, new SelectingConfigChangeHandler<String>()
          .selector(Configuration::getKey)
          .fallback(accept()));

      // ensure to reject these changes
      addToManager(configChangeHandlerManager, reject(), NODE_METADATA_DIR);
      addToManager(configChangeHandlerManager, reject(), NODE_NAME);
      addToManager(configChangeHandlerManager, reject(), NODE_HOSTNAME);
      addToManager(configChangeHandlerManager, reject(), NODE_PORT);

      // initialize the config handlers that need do to something at startup
      loggerOverrideConfigChangeHandler.init();
    }
    return true;
  }

  @Override
  public <T> T getService(long consumerID, ServiceConfiguration<T> configuration) {
    if (configuration.getServiceType() == IParameterSubstitutor.class) {
      return configuration.getServiceType().cast(parameterSubstitutor);
    }
    if (configuration.getServiceType() == ConfigChangeHandlerManager.class) {
      return configuration.getServiceType().cast(configChangeHandlerManager);
    }
    if (configuration.getServiceType() == DynamicConfigEventService.class) {
      return configuration.getServiceType().cast(dynamicConfigEventService);
    }
    if (configuration.getServiceType() == TopologyService.class) {
      return configuration.getServiceType().cast(topologyService);
    }
    if (configuration.getServiceType() == NomadServer.class) {
      return configuration.getServiceType().cast(nomadServer);
    }
    throw new UnsupportedOperationException(configuration.getServiceType().getName());
  }

  @Override
  public Collection<Class<?>> getProvidedServiceTypes() {
    return Arrays.asList(
        IParameterSubstitutor.class,
        ConfigChangeHandlerManager.class,
        DynamicConfigEventService.class,
        TopologyService.class,
        NomadServer.class
    );
  }

  @Override
  public void prepareForSynchronization() {
    // no-op
  }

  private void addToManager(ConfigChangeHandlerManager manager, ConfigChangeHandler configChangeHandler, Setting setting) {
    if (!manager.add(setting, configChangeHandler)) {
      throw new AssertionError("Duplicate " + ConfigChangeHandler.class.getSimpleName() + " for " + setting);
    }
    LOGGER.debug("Registered dynamic configuration change handler for setting {}: {}", setting, configChangeHandler);
  }

  private <T> T find(PlatformConfiguration platformConfiguration, Class<T> type) {
    final Collection<T> services = platformConfiguration.getExtendedConfiguration(type);
    if (services.isEmpty()) {
      return null;
    }
    if (services.size() == 1) {
      return services.iterator().next();
    }
    throw new IllegalStateException("Multiple instance of service " + type + " found");
  }
}
