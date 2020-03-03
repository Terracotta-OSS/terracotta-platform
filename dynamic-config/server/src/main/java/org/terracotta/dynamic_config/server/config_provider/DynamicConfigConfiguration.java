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

import com.terracotta.config.Configuration;
import org.terracotta.config.TcConfig;
import org.terracotta.config.TcConfiguration;
import org.terracotta.entity.ServiceProviderConfiguration;

import java.util.List;

class DynamicConfigConfiguration implements Configuration {
  private final TcConfiguration tcConfiguration;
  private final boolean partialConfig;

  DynamicConfigConfiguration(TcConfiguration tcConfiguration, boolean partialConfig) {
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
