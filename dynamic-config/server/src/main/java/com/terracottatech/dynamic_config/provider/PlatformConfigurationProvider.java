/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.provider;

import org.terracotta.entity.PlatformConfiguration;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderConfiguration;

import com.tc.classloader.BuiltinService;
import com.terracottatech.dynamic_config.nomad.processor.SettingNomadChangeProcessor;

import java.util.Collection;
import java.util.Collections;

@BuiltinService
public class PlatformConfigurationProvider implements ServiceProvider {
  @Override
  public boolean initialize(ServiceProviderConfiguration configuration, PlatformConfiguration platformConfiguration) {
    SettingNomadChangeProcessor.get().setPlatformConfiguration(platformConfiguration);
    return true;
  }

  @Override
  public <T> T getService(long consumerID, ServiceConfiguration<T> configuration) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<Class<?>> getProvidedServiceTypes() {
    return Collections.emptyList();
  }

  @Override
  public void prepareForSynchronization() {
    // no-op
  }
}
