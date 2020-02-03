/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.server.config_provider;

import com.tc.classloader.OverrideService;
import com.tc.config.DefaultConfigurationProvider;
import com.terracotta.config.Configuration;
import com.terracotta.config.ConfigurationException;
import com.terracotta.config.ConfigurationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.server.nomad.NomadBootstrapper;

import java.util.List;

@OverrideService("com.tc.config.DefaultConfigurationProvider")
public class SelectableConfigurationProvider implements ConfigurationProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(SelectableConfigurationProvider.class);

  private volatile ConfigurationProvider delegate;

  @Override
  public void initialize(List<String> args) throws ConfigurationException {
    getDelegate().initialize(args);
  }

  @Override
  public Configuration getConfiguration() {
    return getDelegate().getConfiguration();
  }

  @Override
  public String getConfigurationParamsDescription() {
    return getDelegate().getConfigurationParamsDescription();
  }

  @Override
  public byte[] getSyncData() {
    return getDelegate().getSyncData();
  }

  @Override
  public void sync(byte[] configuration) {
    getDelegate().sync(configuration);
  }

  @Override
  public void close() {
    getDelegate().close();
  }

  private ConfigurationProvider getDelegate() {
    if (delegate == null) {
      boolean nodeStartedWithNewScript = NomadBootstrapper.getNomadServerManager() == null;
      delegate = nodeStartedWithNewScript ? new DefaultConfigurationProvider() : new DynamicConfigConfigurationProvider();
      LOGGER.info("Selected " + ConfigurationProvider.class.getSimpleName() + ": " + delegate.getClass().getName());
    }
    return delegate;
  }
}