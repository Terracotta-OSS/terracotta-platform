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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.entity.StateDumpCollector;
import org.terracotta.entity.StateDumpable;
import org.terracotta.management.service.monitoring.EntityManagementRegistry;
import org.terracotta.management.service.monitoring.ManageableServerComponent;
import org.terracotta.offheapresource.config.MemoryUnit;
import org.terracotta.offheapresource.config.OffheapResourcesType;
import org.terracotta.offheapresource.config.ResourceType;
import org.terracotta.offheapresource.management.OffHeapResourceBinding;
import org.terracotta.offheapresource.management.OffHeapResourceSettingsManagementProvider;
import org.terracotta.offheapresource.management.OffHeapResourceStatisticsManagementProvider;
import org.terracotta.statistics.StatisticsManager;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A provider of {@link OffHeapResource} instances.
 * <p>
 * This service allows for the configuration of a multitude of virtual offheap
 * resource pools from which participating entities can reserve space.  This
 * allows for the partitioning and control of memory usage by entities
 * consuming this service.
 */
public class OffHeapResourcesProvider implements OffHeapResources, ManageableServerComponent, StateDumpable {

  private static final Logger LOGGER = LoggerFactory.getLogger(OffHeapResourcesProvider.class);

  private final Map<OffHeapResourceIdentifier, OffHeapResourceImpl> resources = new HashMap<>();
  private final Collection<EntityManagementRegistry> registries = new CopyOnWriteArrayList<>();

  public OffHeapResourcesProvider(OffheapResourcesType configuration) {
    long totalSize = 0;
    for (ResourceType r : configuration.getResource()) {
      long size = longValueExact(convert(r.getValue(), r.getUnit()));
      totalSize += size;
      OffHeapResourceIdentifier identifier = OffHeapResourceIdentifier.identifier(r.getName());
      OffHeapResourceImpl offHeapResource = new OffHeapResourceImpl(identifier.getName(), size, (res, update) -> {
        for (EntityManagementRegistry registry : registries) {
          Map<String, String> attrs = new HashMap<>();
          attrs.put("oldThreshold", String.valueOf(update.old));
          attrs.put("threshold", String.valueOf(update.now));
          attrs.put("capacity", String.valueOf(res.capacity()));
          attrs.put("available", String.valueOf(res.available()));
          registry.pushServerEntityNotification(res.getManagementBinding(), "OFFHEAP_RESOURCE_THRESHOLD_REACHED", attrs);
        }
      });
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
  }

  @Override
  public Set<OffHeapResourceIdentifier> getAllIdentifiers() {
    return Collections.unmodifiableSet(resources.keySet());
  }

  @Override
  public OffHeapResourceImpl getOffHeapResource(OffHeapResourceIdentifier identifier) {
    return resources.get(identifier);
  }

  @Override
  public void onManagementRegistryCreated(EntityManagementRegistry registry) {
    LOGGER.trace("[{}] onManagementRegistryCreated()", registry.getMonitoringService().getConsumerId());

    registries.add(registry);

    registry.addManagementProvider(new OffHeapResourceSettingsManagementProvider());
    registry.addManagementProvider(new OffHeapResourceStatisticsManagementProvider());

    for (OffHeapResourceIdentifier identifier : getAllIdentifiers()) {
      LOGGER.trace("[{}] onManagementRegistryCreated() - Exposing OffHeapResource:{}", registry.getMonitoringService().getConsumerId(), identifier.getName());
      OffHeapResourceBinding managementBinding = getOffHeapResource(identifier).getManagementBinding();
      registry.register(managementBinding);
    }
  }

  @Override
  public void onManagementRegistryClose(EntityManagementRegistry registry) {
    registries.remove(registry);
  }

  @Override
  public void addStateTo(StateDumpCollector dump) {
    for (Map.Entry<OffHeapResourceIdentifier, OffHeapResourceImpl> entry : resources.entrySet()) {
      OffHeapResourceIdentifier identifier = entry.getKey();
      OffHeapResource resource = entry.getValue();
      StateDumpCollector offHeapDump = dump.subStateDumpCollector(identifier.getName());
      offHeapDump.addState("capacity", String.valueOf(resource.capacity()));
      offHeapDump.addState("available", String.valueOf(resource.available()));
    }
  }

  static BigInteger convert(BigInteger value, MemoryUnit unit) {
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

  static long longValueExact(BigInteger value) {
    if (value.compareTo(MAX_LONG_PLUS_ONE) < 0) {
      return value.longValue();
    } else {
      throw new ArithmeticException("BigInteger out of long range");
    }
  }
}
