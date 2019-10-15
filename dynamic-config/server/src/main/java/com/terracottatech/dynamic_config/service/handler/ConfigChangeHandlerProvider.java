/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.service.handler;

import com.tc.classloader.BuiltinService;
import com.terracottatech.config.data_roots.DataDirectoriesConfig;
import com.terracottatech.diagnostic.server.DiagnosticServices;
import com.terracottatech.dynamic_config.handler.ConfigChangeHandler;
import com.terracottatech.dynamic_config.handler.ConfigChangeHandlerManager;
import com.terracottatech.dynamic_config.handler.SelectingConfigChangeHandler;
import com.terracottatech.dynamic_config.nomad.SettingNomadChange;
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

import static com.terracottatech.dynamic_config.handler.ConfigChangeHandler.reject;
import static com.terracottatech.dynamic_config.model.Setting.CLIENT_LEASE_DURATION;
import static com.terracottatech.dynamic_config.model.Setting.DATA_DIRS;
import static com.terracottatech.dynamic_config.model.Setting.OFFHEAP_RESOURCES;
import static com.terracottatech.dynamic_config.model.Setting.TC_PROPERTIES;

@BuiltinService
public class ConfigChangeHandlerProvider implements ServiceProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigChangeHandlerProvider.class);

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
        ConfigChangeHandler configChangeHandler = new DataRootConfigChangeHandler(dataDirectoriesConfigs.iterator().next(), substitutor);
        if (!manager.add(DATA_DIRS, configChangeHandler)) {
          throw new AssertionError("Duplicate " + ConfigChangeHandler.class.getSimpleName() + " for " + DATA_DIRS);
        } else {
          LOGGER.info("Registered dynamic configuration change handler: {} for setting: {}", configChangeHandler.getClass().getName(), DATA_DIRS);
        }
      }

      // offheap
      Collection<OffHeapResources> offHeapResources = platformConfiguration.getExtendedConfiguration(OffHeapResources.class);
      if (offHeapResources.size() > 1) {
        throw new UnsupportedOperationException("Multiple " + OffHeapResources.class.getSimpleName() + " not supported");
      }
      if (!offHeapResources.isEmpty()) {
        ConfigChangeHandler configChangeHandler = new OffheapConfigChangeHandler(offHeapResources.iterator().next(), substitutor);
        if (!manager.add(OFFHEAP_RESOURCES, configChangeHandler)) {
          throw new AssertionError("Duplicate " + ConfigChangeHandler.class.getSimpleName() + " for " + OFFHEAP_RESOURCES);
        } else {
          LOGGER.info("Registered dynamic configuration change handler: {} for setting: {}", configChangeHandler.getClass().getName(), OFFHEAP_RESOURCES);
        }
      }

      // lease
      manager.add(CLIENT_LEASE_DURATION, new LeaseConfigChangeHandler());

      // tc-properties
      // TODO [DYNAMIC-CONFIG]: TDB-4710: IMPLEMENT TC-PROPERTIES CHANGE
      manager.add(TC_PROPERTIES, new SelectingConfigChangeHandler<>()
          .selector(SettingNomadChange::getName)
          .add("server.entity.processor.threads", new ProcessorThreadsConfigChangeHandler())
          .add("foo.bar", new FooBarConfigChangeHandler())
          .fallback(reject()));
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
    throw new UnsupportedOperationException(configuration.getServiceType().getName());
  }

  @Override
  public Collection<Class<?>> getProvidedServiceTypes() {
    return Arrays.asList(IParameterSubstitutor.class, ConfigChangeHandlerManager.class);
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
}
