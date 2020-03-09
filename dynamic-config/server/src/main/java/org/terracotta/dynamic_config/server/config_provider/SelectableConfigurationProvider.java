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
package org.terracotta.dynamic_config.server.config_provider;

import com.tc.classloader.OverrideService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.config.provider.DefaultConfigurationProvider;
import org.terracotta.configuration.Configuration;
import org.terracotta.configuration.ConfigurationException;
import org.terracotta.configuration.ConfigurationProvider;
import org.terracotta.dynamic_config.server.nomad.NomadBootstrapper;

import java.util.List;

@OverrideService("org.terracotta.config.provider.DefaultConfigurationProvider")
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