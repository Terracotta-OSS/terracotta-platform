/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package org.terracotta.platform;

import com.tc.classloader.BuiltinService;
import org.terracotta.entity.PlatformConfiguration;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderConfiguration;
import org.terracotta.entity.StateDumpCollector;

import java.util.Collection;
import java.util.Collections;

@BuiltinService
public class ServerInfoProvider implements ServiceProvider {
  private volatile ServerInfo serverInfo;

  @Override
  public boolean initialize(ServiceProviderConfiguration serviceProviderConfiguration, PlatformConfiguration platformConfiguration) {
    String serverName = platformConfiguration.getServerName();
    serverInfo = new ServerInfo(serverName);
    return true;
  }

  @Override
  public <T> T getService(long l, ServiceConfiguration<T> serviceConfiguration) {
    Class<T> serviceType = serviceConfiguration.getServiceType();

    if (!serviceType.equals(ServerInfo.class)) {
      return null;
    }

    return serviceType.cast(serverInfo);
  }

  @Override
  public Collection<Class<?>> getProvidedServiceTypes() {
    return Collections.singletonList(ServerInfo.class);
  }

  @Override
  public void prepareForSynchronization() {
  }

  @Override
  public void addStateTo(StateDumpCollector stateDumpCollector) {
    stateDumpCollector.addState("serverName", serverInfo.getName());
  }
}
