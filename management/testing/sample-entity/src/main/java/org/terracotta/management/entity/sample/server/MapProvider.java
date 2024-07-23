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
package org.terracotta.management.entity.sample.server;

import com.tc.classloader.BuiltinService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.entity.PlatformConfiguration;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderConfiguration;
import org.terracotta.entity.StateDumpCollector;
import org.terracotta.offheapresource.OffHeapResource;
import org.terracotta.offheapresource.OffHeapResourceIdentifier;
import org.terracotta.offheapresource.OffHeapResources;

import java.io.Closeable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Mathieu Carbou
 */
@BuiltinService
public class MapProvider implements ServiceProvider, Closeable {

  private static final Logger LOGGER = LoggerFactory.getLogger(MapProvider.class);

  private final Map<String, Map<String, String>> caches = new ConcurrentHashMap<>();
  private OffHeapResource offHeapResource;

  @Override
  public Collection<Class<?>> getProvidedServiceTypes() {
    return Collections.singletonList(Map.class);
  }

  @Override
  public boolean initialize(ServiceProviderConfiguration configuration, PlatformConfiguration platformConfiguration) {
    OffHeapResources offHeapResources = findService(platformConfiguration, OffHeapResources.class);
    offHeapResource = offHeapResources.getOffHeapResource(OffHeapResourceIdentifier.identifier("primary-server-resource"));
    return true;
  }

  @Override
  public void prepareForSynchronization() {
  }

  @Override
  public void close() {
    caches.clear();
  }

  @Override
  public <T> T getService(long consumerID, ServiceConfiguration<T> configuration) {
    Class<T> serviceType = configuration.getServiceType();

    if (Map.class == serviceType) {
      if (configuration instanceof MapConfiguration) {
        MapConfiguration mapConfiguration = (MapConfiguration) configuration;
        LOGGER.trace("getService({}, {})", consumerID, configuration);
        return serviceType.cast(caches.computeIfAbsent(mapConfiguration.getName(), s -> {
          // just to mimic some allocation
          offHeapResource.reserve(12 * 1024 * 1024);
          return new ConcurrentHashMap<>();
        }));
      
      } else if (configuration instanceof MapRelease) {
        offHeapResource.release(12 * 1024 * 1024);
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

  private <T> T findService(PlatformConfiguration platformConfiguration, Class<T> type) {
    final Collection<T> services = platformConfiguration.getExtendedConfiguration(type);
    if (services.isEmpty()) {
      throw new AssertionError("No instance of service " + type + " found");
    }

    if (services.size() == 1) {
      T instance = services.iterator().next();
      if (instance == null) {
        throw new AssertionError("Instance of service " + type + " found to be null");
      }
      return instance;
    }
    throw new AssertionError("Multiple instances of service " + type + " found");
  }
}
