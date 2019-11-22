/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.service;

import com.tc.classloader.BuiltinService;
import com.terracottatech.config.data_roots.DataDirectoriesConfig;
import com.terracottatech.diagnostic.server.DiagnosticServices;
import com.terracottatech.dynamic_config.diagnostic.TopologyService;
import com.terracottatech.dynamic_config.handler.ConfigChangeHandler;
import com.terracottatech.dynamic_config.handler.ConfigChangeHandlerManager;
import com.terracottatech.dynamic_config.handler.SelectingConfigChangeHandler;
import com.terracottatech.dynamic_config.model.Configuration;
import com.terracottatech.dynamic_config.model.Setting;
import com.terracottatech.dynamic_config.service.handler.ClientReconnectWindowConfigChangeHandler;
import com.terracottatech.dynamic_config.service.handler.DataDirectoryConfigChangeHandler;
import com.terracottatech.dynamic_config.service.handler.OffheapResourceConfigChangeHandler;
import com.terracottatech.dynamic_config.service.handler.ServerAttributeConfigChangeHandler;
import com.terracottatech.dynamic_config.service.handler.SimulationHandler;
import com.terracottatech.dynamic_config.util.IParameterSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.entity.PlatformConfiguration;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderConfiguration;
import org.terracotta.offheapresource.OffHeapResources;

import java.util.Arrays;
import java.util.Collection;

import static com.terracottatech.dynamic_config.handler.ConfigChangeHandler.applyAfterRestart;
import static com.terracottatech.dynamic_config.model.Setting.CLIENT_RECONNECT_WINDOW;
import static com.terracottatech.dynamic_config.model.Setting.DATA_DIRS;
import static com.terracottatech.dynamic_config.model.Setting.FAILOVER_PRIORITY;
import static com.terracottatech.dynamic_config.model.Setting.NODE_BIND_ADDRESS;
import static com.terracottatech.dynamic_config.model.Setting.NODE_GROUP_BIND_ADDRESS;
import static com.terracottatech.dynamic_config.model.Setting.NODE_LOG_DIR;
import static com.terracottatech.dynamic_config.model.Setting.OFFHEAP_RESOURCES;
import static com.terracottatech.dynamic_config.model.Setting.TC_PROPERTIES;

@BuiltinService
public class DynamicConfigServiceProvider implements ServiceProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(DynamicConfigServiceProvider.class);

  @Override
  public boolean initialize(ServiceProviderConfiguration configuration, PlatformConfiguration platformConfiguration) {
    // If the server is started without the startup manager, with the old script but not with not start-node.sh, then the diagnostic services won't be there.
    ConfigChangeHandlerManager manager = getManager();
    if (manager != null) {
      IParameterSubstitutor substitutor = getSubstitutor();

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
        ConfigChangeHandler configChangeHandler = new OffheapResourceConfigChangeHandler(offHeapResources.iterator().next(), substitutor);
        addToManager(manager, configChangeHandler, OFFHEAP_RESOURCES);
      }

      {
        // failover-priority
        addToManager(manager, applyAfterRestart(substitutor), FAILOVER_PRIORITY);
      }

      {
        // client-reconnect-window
        ConfigChangeHandler configChangeHandler = new ClientReconnectWindowConfigChangeHandler(substitutor);
        addToManager(manager, configChangeHandler, CLIENT_RECONNECT_WINDOW);
      }

      {
        // server attributes
        ConfigChangeHandler configChangeHandler = new ServerAttributeConfigChangeHandler(substitutor);
        addToManager(manager, configChangeHandler, NODE_LOG_DIR);
        addToManager(manager, configChangeHandler, NODE_BIND_ADDRESS);
        addToManager(manager, configChangeHandler, NODE_GROUP_BIND_ADDRESS);
      }

      // tc-properties
      manager.add(TC_PROPERTIES, new SelectingConfigChangeHandler<>()
          .selector(Configuration::getKey)
          .add("com.terracottatech.dynamic-config.simulate", new SimulationHandler(substitutor))
          .fallback(applyAfterRestart(substitutor)));
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
    if (configuration.getServiceType() == DynamicConfigEventing.class) {
      return configuration.getServiceType().cast(getEventingSupport());
    }
    if (configuration.getServiceType() == TopologyService.class) {
      return configuration.getServiceType().cast(getTopologyService());
    }
    throw new UnsupportedOperationException(configuration.getServiceType().getName());
  }

  @Override
  public Collection<Class<?>> getProvidedServiceTypes() {
    return Arrays.asList(IParameterSubstitutor.class, ConfigChangeHandlerManager.class, DynamicConfigEventing.class, TopologyService.class);
  }

  @Override
  public void prepareForSynchronization() {
    // no-op
  }

  private IParameterSubstitutor getSubstitutor() {
    return DiagnosticServices.findService(IParameterSubstitutor.class).orElse(null);
  }

  private ConfigChangeHandlerManager getManager() {
    return DiagnosticServices.findService(ConfigChangeHandlerManager.class).orElse(null);
  }

  private DynamicConfigEventing getEventingSupport() {
    return DiagnosticServices.findService(DynamicConfigEventing.class).orElse(null);
  }

  private TopologyService getTopologyService() {
    return DiagnosticServices.findService(TopologyService.class).orElse(null);
  }

  private void addToManager(ConfigChangeHandlerManager manager, ConfigChangeHandler configChangeHandler, Setting setting) {
    if (!manager.add(setting, configChangeHandler)) {
      throw new AssertionError("Duplicate " + ConfigChangeHandler.class.getSimpleName() + " for " + setting);
    } else {
      LOGGER.info("Registered dynamic configuration change handler: {} for setting: {}", configChangeHandler.getClass().getName(), setting);
    }
  }
}
