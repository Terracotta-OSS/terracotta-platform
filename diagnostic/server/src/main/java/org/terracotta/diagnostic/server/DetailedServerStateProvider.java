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
package org.terracotta.diagnostic.server;

import com.tc.classloader.BuiltinService;
import org.terracotta.dynamic_config.server.api.DetailedServerState;
import org.terracotta.entity.PlatformConfiguration;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderConfiguration;

import java.util.Collection;
import java.util.Collections;

@BuiltinService
public class DetailedServerStateProvider implements ServiceProvider {
  private volatile DetailedServerStateImpl detailedServerState;

  @Override
  public boolean initialize(ServiceProviderConfiguration serviceProviderConfiguration, PlatformConfiguration platformConfiguration) {
    detailedServerState = DetailedServerStateImpl.init();
    return true;
  }

  @Override
  public <T> T getService(long l, ServiceConfiguration<T> serviceConfiguration) {
    Class<T> serviceType = serviceConfiguration.getServiceType();
    return !serviceType.equals(DetailedServerState.class) ? null : serviceType.cast(detailedServerState);
  }

  @Override
  public Collection<Class<?>> getProvidedServiceTypes() {
    return Collections.singletonList(DetailedServerState.class);
  }

  @Override
  public void prepareForSynchronization() {
  }
}
