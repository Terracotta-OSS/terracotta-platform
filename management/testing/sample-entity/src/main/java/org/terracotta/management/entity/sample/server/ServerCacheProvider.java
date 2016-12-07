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
package org.terracotta.management.entity.sample.server;

import com.tc.classloader.BuiltinService;
import org.terracotta.entity.PlatformConfiguration;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderCleanupException;
import org.terracotta.entity.ServiceProviderConfiguration;

import java.io.Closeable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Mathieu Carbou
 */
@BuiltinService
public class ServerCacheProvider implements ServiceProvider, Closeable {

  private final Map<String, ServerCache> caches = new ConcurrentHashMap<>();

  @Override
  public Collection<Class<?>> getProvidedServiceTypes() {
    return Arrays.asList(ServerCache.class);
  }

  @Override
  public boolean initialize(ServiceProviderConfiguration configuration, PlatformConfiguration platformConfiguration) {
    return true;
  }

  @Override
  public void prepareForSynchronization() throws ServiceProviderCleanupException {
  }

  @Override
  public void close() {
    caches.clear();
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T getService(long consumerID, ServiceConfiguration<T> configuration) {
    Class<T> serviceType = configuration.getServiceType();

    if (ServerCache.class == serviceType) {
      if (configuration instanceof ServerCacheConfiguration) {
        ServerCacheConfiguration serverCacheConfiguration = (ServerCacheConfiguration) configuration;
        return serviceType.cast(caches.computeIfAbsent(serverCacheConfiguration.getName(), ServerCache::new));
      } else {
        throw new IllegalArgumentException("Missing configuration " + ServerCacheConfiguration.class.getSimpleName() + " when requesting service " + serviceType.getName());
      }
    }

    throw new IllegalStateException("Unable to provide service " + serviceType.getName() + " to consumerID: " + consumerID);
  }

}
