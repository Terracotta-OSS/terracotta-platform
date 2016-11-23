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
package org.terracotta.offheapresource;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.terracotta.entity.PlatformConfiguration;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderConfiguration;
import org.terracotta.offheapresource.config.MemoryUnit;
import org.terracotta.offheapresource.config.ResourceType;
import org.terracotta.statistics.StatisticsManager;

/**
 * A provider of {@link OffHeapResource} instances.
 * <p>
 * This service allows for the configuration of a multitude of virtual offheap
 * resource pools from which participating entities can reserve space.  This
 * allows for the partitioning and control of memory usage by entities
 * consuming this service.
 */
public class OffHeapResourcesProvider implements ServiceProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(OffHeapResourcesProvider.class);
          
  private final Map<OffHeapResourceIdentifier, OffHeapResource> resources = new HashMap<OffHeapResourceIdentifier, OffHeapResource>();

  @Override
  public synchronized boolean initialize(ServiceProviderConfiguration unknownConfig, PlatformConfiguration platformConfiguration) {
    if (unknownConfig instanceof OffHeapResourcesConfiguration) {
      OffHeapResourcesConfiguration configuration = (OffHeapResourcesConfiguration) unknownConfig;
      if (resources.isEmpty()) {
        long totalSize = 0;
        for (ResourceType r : configuration.getResources()) {
          long size = longValueExact(convert(r.getValue(), r.getUnit()));
          totalSize += size;
          OffHeapResourceImpl offHeapResource = new OffHeapResourceImpl(size);
          OffHeapResourceIdentifier identifier = OffHeapResourceIdentifier.identifier(r.getName());
          resources.put(identifier, offHeapResource);

          Map<String, Object> properties = new HashMap<>();
          properties.put("discriminator", "OffHeapResource");
          properties.put("offHeapResourceIdentifier", identifier.getName());
          StatisticsManager.createPassThroughStatistic(
              offHeapResource,
              "allocatedMemory",
              new HashSet<>(Arrays.asList("OffHeapResource", "tier")),
              properties,
              (Callable<Number>) () -> offHeapResource.capacity() - offHeapResource.available());
        }
        Long physicalMemory = PhysicalMemory.totalPhysicalMemory();
        if (physicalMemory != null && totalSize > physicalMemory) {
          LOGGER.warn("More offheap configured than there is physical memory [{} > {}]", totalSize, physicalMemory);
        }
        return true;
      } else {
        throw new IllegalStateException("Resources already initialized");
      }
    } else {
      return false;
    }
  }

  @Override
  public <T> T getService(long consumerID, ServiceConfiguration<T> unknownConfiguration) {
    if (unknownConfiguration instanceof OffHeapResourceIdentifier) {
      OffHeapResourceIdentifier identifier = (OffHeapResourceIdentifier) unknownConfiguration;
      return (T) identifier.getServiceType().cast(resources.get(identifier));
    } else if (OffHeapResources.class.isAssignableFrom(unknownConfiguration.getServiceType())) {
      return (T) new OffHeapResources() {
        @Override
        public Set<String> getAllIdentifiers() {
          Set<String> names = new HashSet<String>();
          for (OffHeapResourceIdentifier identifier: resources.keySet()) {
            names.add(identifier.getName());
          }

          return Collections.unmodifiableSet(names);
        }
      };
    } else {
      throw new IllegalArgumentException("Unexpected configuration type " + unknownConfiguration.getClass());
    }
  }

  @Override
  public Collection<Class<?>> getProvidedServiceTypes() {
    return Arrays.asList(OffHeapResource.class, OffHeapResources.class);
  }

  @Override
  public void prepareForSynchronization () {
    resources.clear();
  }

  private static BigInteger convert(BigInteger value, MemoryUnit unit) {
    switch (unit) {
      case B: return value.shiftLeft(0);
      case K_B: return value.shiftLeft(10);
      case MB: return value.shiftLeft(20);
      case GB: return value.shiftLeft(30);
      case TB: return value.shiftLeft(40);
      case PB: return value.shiftLeft(50);
    }
    throw new IllegalArgumentException("Unknown unit " + unit);
  }

  private static final BigInteger MAX_LONG_PLUS_ONE = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE);
  private static long longValueExact(BigInteger value) {
    if (value.compareTo(MAX_LONG_PLUS_ONE) < 0) {
      return value.longValue();
    } else {
      throw new ArithmeticException("BigInteger out of long range");
    }
  }
}
