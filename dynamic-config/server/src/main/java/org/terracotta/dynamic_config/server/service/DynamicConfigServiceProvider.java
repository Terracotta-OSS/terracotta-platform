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
import org.terracotta.config.data_roots.DataDirectoriesConfig;
import org.terracotta.diagnostic.server.DiagnosticServices;
import org.terracotta.dynamic_config.api.model.Configuration;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Setting;
import org.terracotta.dynamic_config.api.service.ConfigChangeHandler;
import org.terracotta.dynamic_config.api.service.ConfigChangeHandlerManager;
import org.terracotta.dynamic_config.api.service.DynamicConfigEventService;
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;
import org.terracotta.dynamic_config.api.service.SelectingConfigChangeHandler;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.server.service.handler.ClientReconnectWindowConfigChangeHandler;
import org.terracotta.dynamic_config.server.service.handler.DataDirectoryConfigChangeHandler;
import org.terracotta.dynamic_config.server.service.handler.LoggerOverrideConfigChangeHandler;
import org.terracotta.dynamic_config.server.service.handler.OffheapResourceConfigChangeHandler;
import org.terracotta.dynamic_config.server.service.handler.ServerAttributeConfigChangeHandler;
import org.terracotta.entity.PlatformConfiguration;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderConfiguration;
import org.terracotta.nomad.server.NomadServer;
import org.terracotta.offheapresource.OffHeapResources;

import java.util.Arrays;
import java.util.Collection;

import static org.terracotta.dynamic_config.api.model.Setting.CLIENT_RECONNECT_WINDOW;
import static org.terracotta.dynamic_config.api.model.Setting.DATA_DIRS;
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
import static org.terracotta.dynamic_config.api.model.Setting.OFFHEAP_RESOURCES;
import static org.terracotta.dynamic_config.api.model.Setting.SECURITY_AUDIT_LOG_DIR;
import static org.terracotta.dynamic_config.api.model.Setting.SECURITY_AUTHC;
import static org.terracotta.dynamic_config.api.model.Setting.SECURITY_DIR;
import static org.terracotta.dynamic_config.api.model.Setting.SECURITY_SSL_TLS;
import static org.terracotta.dynamic_config.api.model.Setting.SECURITY_WHITELIST;
import static org.terracotta.dynamic_config.api.model.Setting.TC_PROPERTIES;
import static org.terracotta.dynamic_config.api.service.ConfigChangeHandler.accept;
import static org.terracotta.dynamic_config.api.service.ConfigChangeHandler.reject;

@BuiltinService
public class DynamicConfigServiceProvider implements ServiceProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(DynamicConfigServiceProvider.class);

  @Override
  public boolean initialize(ServiceProviderConfiguration configuration, PlatformConfiguration platformConfiguration) {
    // If the server is started without the startup manager, with the old script but not with not start-node.sh, then the diagnostic services won't be there.
    ConfigChangeHandlerManager manager = getManager();

    if (manager != null) {
      IParameterSubstitutor substitutor = getSubstitutor();
      TopologyService topologyService = getTopologyService();

      // data-dirs
      Collection<DataDirectoriesConfig> dataDirectoriesConfigs = platformConfiguration.getExtendedConfiguration(DataDirectoriesConfig.class);
      if (dataDirectoriesConfigs.size() > 1) {
        throw new UnsupportedOperationException("Multiple " + DataDirectoriesConfig.class.getSimpleName() + " not supported");
      }
      if (!dataDirectoriesConfigs.isEmpty()) {
        ConfigChangeHandler configChangeHandler = new DataDirectoryConfigChangeHandler(dataDirectoriesConfigs.iterator().next(), substitutor);
        addToManager(manager, configChangeHandler, DATA_DIRS);
      }

      // offheap-resources
      Collection<OffHeapResources> offHeapResources = platformConfiguration.getExtendedConfiguration(OffHeapResources.class);
      if (offHeapResources.size() > 1) {
        throw new UnsupportedOperationException("Multiple " + OffHeapResources.class.getSimpleName() + " not supported");
      }
      if (!offHeapResources.isEmpty()) {
        ConfigChangeHandler configChangeHandler = new OffheapResourceConfigChangeHandler(topologyService, offHeapResources.iterator().next());
        addToManager(manager, configChangeHandler, OFFHEAP_RESOURCES);
      }

      // client-reconnect-window
      ConfigChangeHandler clientReconnectWindowHandler = new ClientReconnectWindowConfigChangeHandler();
      addToManager(manager, clientReconnectWindowHandler, CLIENT_RECONNECT_WINDOW);

      // server attributes
      ConfigChangeHandler serverAttributeConfigChangeHandler = new ServerAttributeConfigChangeHandler();
      addToManager(manager, serverAttributeConfigChangeHandler, NODE_LOG_DIR);
      addToManager(manager, serverAttributeConfigChangeHandler, NODE_BIND_ADDRESS);
      addToManager(manager, serverAttributeConfigChangeHandler, NODE_GROUP_BIND_ADDRESS);

      // settings applied directly without any config handler but which require a restart
      addToManager(manager, accept(), FAILOVER_PRIORITY);
      addToManager(manager, accept(), NODE_GROUP_PORT);
      addToManager(manager, accept(), SECURITY_DIR);
      addToManager(manager, accept(), SECURITY_AUDIT_LOG_DIR);
      addToManager(manager, accept(), SECURITY_AUTHC);
      addToManager(manager, accept(), SECURITY_SSL_TLS);
      addToManager(manager, accept(), SECURITY_WHITELIST);

      // public hostname/port
      addToManager(manager, accept(), NODE_PUBLIC_HOSTNAME);
      addToManager(manager, accept(), NODE_PUBLIC_PORT);

      // tc-logging
      LoggerOverrideConfigChangeHandler loggerOverrideConfigChangeHandler = new LoggerOverrideConfigChangeHandler(topologyService);
      addToManager(manager, loggerOverrideConfigChangeHandler, NODE_LOGGER_OVERRIDES);

      // tc-properties
      manager.add(TC_PROPERTIES, new SelectingConfigChangeHandler<String>()
          .selector(Configuration::getKey)
          .fallback(accept()));

      // ensure to reject these changes
      addToManager(manager, reject(), NODE_METADATA_DIR);
      addToManager(manager, reject(), NODE_NAME);
      addToManager(manager, reject(), NODE_HOSTNAME);
      addToManager(manager, reject(), NODE_PORT);

      // initialize the config handlers that need do to something at startup
      loggerOverrideConfigChangeHandler.init();
    }
    return true;
  }

  @Override
  public <T> T getService(long consumerID, ServiceConfiguration<T> configuration) {
    if (configuration.getServiceType() == IParameterSubstitutor.class) {
      return configuration.getServiceType().cast(getSubstitutor());
    }
    if (configuration.getServiceType() == ConfigChangeHandlerManager.class) {
      return configuration.getServiceType().cast(getManager());
    }
    if (configuration.getServiceType() == DynamicConfigEventService.class) {
      return configuration.getServiceType().cast(getEventingSupport());
    }
    if (configuration.getServiceType() == TopologyService.class) {
      return configuration.getServiceType().cast(getTopologyService());
    }
    if (configuration.getServiceType() == NomadServer.class) {
      return configuration.getServiceType().cast(getNomadServer());
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

  // note: if any of the get() calls below fail, it means the servers have not been started with dynamic config code

  private IParameterSubstitutor getSubstitutor() {
    return DiagnosticServices.findService(IParameterSubstitutor.class).get();
  }

  private ConfigChangeHandlerManager getManager() {
    return DiagnosticServices.findService(ConfigChangeHandlerManager.class).get();
  }

  private DynamicConfigEventService getEventingSupport() {
    return DiagnosticServices.findService(DynamicConfigEventService.class).get();
  }

  private TopologyService getTopologyService() {
    return DiagnosticServices.findService(TopologyService.class).get();
  }

  @SuppressWarnings("unchecked")
  private NomadServer<NodeContext> getNomadServer() {
    return DiagnosticServices.findService(NomadServer.class).get();
  }

  private void addToManager(ConfigChangeHandlerManager manager, ConfigChangeHandler configChangeHandler, Setting setting) {
    if (!manager.add(setting, configChangeHandler)) {
      throw new AssertionError("Duplicate " + ConfigChangeHandler.class.getSimpleName() + " for " + setting);
    }
    LOGGER.debug("Registered dynamic configuration change handler for setting {}: {}", setting, configChangeHandler);
  }
}
