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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.entity.PlatformConfiguration;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderCleanupException;
import org.terracotta.entity.ServiceProviderConfiguration;
import org.terracotta.entity.StateDumpCollector;
import org.terracotta.offheapresource.OffHeapResource;
import org.terracotta.offheapresource.OffHeapResourceIdentifier;
import org.terracotta.offheapresource.OffHeapResources;

import java.io.Closeable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Mathieu Carbou
 */
@BuiltinService
public class MapProvider implements ServiceProvider, Closeable {

  private static final Logger LOGGER = LoggerFactory.getLogger(MapProvider.class);

  private final Map<String, Map<String, String>> caches = new ConcurrentHashMap<>();

  private OffHeapResource heapResource;

  @Override
  public Collection<Class<?>> getProvidedServiceTypes() {
    return Arrays.asList(Map.class);
  }

  @Override
  public boolean initialize(ServiceProviderConfiguration configuration, PlatformConfiguration platformConfiguration) {
    OffHeapResources offHeapResources = platformConfiguration.getExtendedConfiguration(OffHeapResources.class).iterator().next();
    heapResource = offHeapResources.getOffHeapResource(OffHeapResourceIdentifier.identifier("primary-server-resource"));
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

    if (Map.class == serviceType) {
      if (configuration instanceof MapConfiguration) {
        MapConfiguration mapConfiguration = (MapConfiguration) configuration;
        LOGGER.trace("getService({}, {})", consumerID, configuration);
        return serviceType.cast(caches.computeIfAbsent(mapConfiguration.getName(), s -> {
          // just to mimic some allocation
          heapResource.reserve(12 * 1024 * 1024);
          return new ConcurrentHashMap<>();
        }));
      
      } else if (configuration instanceof MapRelease) {
        heapResource.release(12 * 1024 * 1024);
        return null;
      
      } else {
        throw new IllegalArgumentException("Missing configuration " + MapConfiguration.class.getSimpleName() + " when requesting service " + serviceType.getName());
      }
    }

    throw new IllegalStateException("Unable to provide service " + serviceType.getName() + " to consumerID: " + consumerID);
  }

  @Override
  public void addStateTo(StateDumpCollector stateDumper) {
    stateDumper.addState("caches", this.caches.keySet());
  }
}
