/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.server.config_provider;

import com.terracotta.config.Configuration;
import org.terracotta.config.TcConfig;
import org.terracotta.config.TcConfiguration;
import org.terracotta.entity.ServiceProviderConfiguration;

import java.util.List;

class TcConfigurationWrapper implements Configuration {
  private final TcConfiguration tcConfiguration;
  private final boolean partialConfig;

  TcConfigurationWrapper(TcConfiguration tcConfiguration, boolean partialConfig) {
    this.tcConfiguration = tcConfiguration;
    this.partialConfig = partialConfig;
  }

  @Override
  public TcConfig getPlatformConfiguration() {
    return tcConfiguration.getPlatformConfiguration();
  }

  @Override
  public List<ServiceProviderConfiguration> getServiceConfigurations() {
    return tcConfiguration.getServiceConfigurations();
  }

  @Override
  public <T> List<T> getExtendedConfiguration(Class<T> type) {
    return tcConfiguration.getExtendedConfiguration(type);
  }

  @Override
  public String getRawConfiguration() {
    return tcConfiguration.toString();
  }

  @Override
  public boolean isPartialConfiguration() {
    return partialConfig;
  }
}
