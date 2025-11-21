/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
package org.terracotta.stats.entity.server;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.server.EventRegistration;
import org.terracotta.entity.CommonServerEntity;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.StateDumpCollector;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.registry.ExposedObject;
import org.terracotta.management.registry.ManagementProvider;
import org.terracotta.management.registry.ManagementProviderAdapter;
import org.terracotta.management.registry.collect.StatisticProvider;
import org.terracotta.management.service.monitoring.EntityManagementRegistry;


public class StatsCommonEntity implements CommonServerEntity<EntityMessage, EntityResponse> {

  private static final Logger LOGGER = LoggerFactory.getLogger(StatsCommonEntity.class);
  private static final String CAPABILITY_NAME = "StatsCapability";

  private final ManagementProvider<?> capabilityManagementSupport;
  private static final long STATS_COLLECTION_INTERVAL_MINUTE = 5;
  private static final String THREAD_NAME = "Stats-Collector";
  private ScheduledFuture<?> statsHandle = null;

  final EntityManagementRegistry managementRegistry;
  final boolean active;

  private volatile EventRegistration eventRegistration;
  private volatile ScheduledExecutorService statsExecutor;

  public StatsCommonEntity(EntityManagementRegistry managementRegistry, ScheduledExecutorService statsExecutor) {
    this.managementRegistry = managementRegistry;
    this.statsExecutor = statsExecutor;
    this.active = managementRegistry != null;

    // Initialize the management provider
    if (managementRegistry != null) {
      this.capabilityManagementSupport = new ManagementProviderAdapter<>(CAPABILITY_NAME, Object.class);
    } else {
      this.capabilityManagementSupport = null;
    }
  }

  @Override
  public final void createNew() {
    if (active) {
      managementRegistry.entityCreated();

      // Register the management provider
      managementRegistry.addManagementProvider(capabilityManagementSupport);

      managementRegistry.refresh();
      listen();
    }
  }

  @Override
  public final void destroy() {
    if (active) {
      if (eventRegistration != null) {
        eventRegistration.unregister();
        eventRegistration = null;
      }

      // Shutdown the stats executor if it exists
      if (statsExecutor != null) {
        statsExecutor.shutdown();
        try {
          if (!statsExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
            LOGGER.warn("Stats collector thread did not terminate in time");
            statsExecutor.shutdownNow();
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          LOGGER.warn("Interrupted while waiting for stats collector shutdown", e);
        }
        statsExecutor = null;
      }

      // Remove the management provider
      managementRegistry.removeManagementProvider(capabilityManagementSupport);

      if (statsHandle != null) {
        statsHandle.cancel(true);
      }
      managementRegistry.close();
    }
  }

  @Override
  public void addStateTo(StateDumpCollector stateDumpCollector) {
    stateDumpCollector.addState("active", active);
  }

  final void listen() {
    if (eventRegistration == null) {

      // Schedule periodic statistics collection
       statsHandle = statsExecutor.scheduleWithFixedDelay(() -> {
          try {
              collectAndPublishStatistics();
          } catch (Exception e) {
              LOGGER.warn("Error collecting statistics", e);
          }
      }, 0, STATS_COLLECTION_INTERVAL_MINUTE, TimeUnit.MINUTES);

      LOGGER.info("Activated statistics collection and logging");
    }
  }

  /**
   * Collects and publishes all available server statistics
   */
  private void collectAndPublishStatistics() {
      try {
        // Collect all available server statistics
        Map<String, Object> allStats = collectServerStatisticsAsync().get();

        if (!allStats.isEmpty()) {
          LOGGER.info("SERVER_WORKLOAD|STATISTICS {}", allStats);
        } else {
          LOGGER.debug("No statistics available to publish");
        }
      } catch (Exception e) {
        LOGGER.warn("Failed to collect and publish statistics", e);
      }
  }

  /**
   * Collect all available server statistics from the Management Service.
   * This includes:
   * - SovereignDataset statistics (RecordCount, AllocatedMemory, OccupiedStorage, etc.)
   * - SovereignIndex statistics (OccupiedStorage, AccessCount, IndexedRecordCount)
   * - Pool statistics (AllocatedSize)
   * - Store statistics (DataSize, AllocatedMemory, Entries)
   * - OffHeapResource statistics (AllocatedMemory)
   * - DataRoot statistics (TotalDiskUsage)
   * - RestartStore statistics (TotalUsage)
   * - Sequence statistics (SequenceValues)
   *
   * Structure follows DefaultStatisticCollector pattern
   */
  public Future<Map<String, Object>> collectServerStatisticsAsync() {
    Map<String, Object> stats = new HashMap<>();
    try {
      if (managementRegistry != null) {
        for (String capabilityName : managementRegistry.getCapabilityNames()) {

          Set<Context> allContexts = new LinkedHashSet<>();

          for (ManagementProvider<?> managementProvider : managementRegistry.getManagementProvidersByCapability(capabilityName)) {
            if (managementProvider.getClass().isAnnotationPresent(StatisticProvider.class)) {
              for (ExposedObject<?> exposedObject : managementProvider.getExposedObjects()) {
                allContexts.add(exposedObject.getContext());
              }
            }
          }

          if (!allContexts.isEmpty()) {
            try {
              managementRegistry.withCapability(capabilityName)
                  .queryAllStatistics()
                  .on(allContexts)
                  .build()
                  .execute()
                  .forEach(contextualStats -> {
                    contextualStats.getStatistics().forEach((statName, statistic) -> {
                      Object value = statistic.getLatestSampleValue().orElse(null);
                      if (value != null) {
                        stats.put(statName, value);
                      }
                    });
                  });
            } catch (RuntimeException e) {
              LOGGER.warn("Failed to collect statistics from capability {}: {}", capabilityName, e.getMessage(), e);
            }
          }
        }
      }
    } catch (RuntimeException e) {
      LOGGER.error("Error collecting server statistics: " + e.getMessage(), e);
    }

    LOGGER.debug("Collected {} statistics from management registry", stats.size());
    return CompletableFuture.completedFuture(stats);
  }

  /**
   * Collect JVM resource statistics (CPU, memory, etc.)
   * These are system-level metrics from MXBeans
   */
  private Map<String, Object> collectJVMResourceStatistics() {
    Map<String, Object> stats = new HashMap<>();
    try {
      Runtime runtime = Runtime.getRuntime();
      OperatingSystemMXBean osMXBean = ManagementFactory.getOperatingSystemMXBean();
      MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

      // Memory statistics
      long maxMemory = runtime.maxMemory();
      long allocatedMemory = runtime.totalMemory();
      long freeMemory = runtime.freeMemory();
      long usedMemory = allocatedMemory - freeMemory;

      stats.put("jvm_maxMemory", maxMemory);
      stats.put("jvm_allocatedMemory", allocatedMemory);
      stats.put("jvm_freeMemory", freeMemory);
      stats.put("jvm_usedMemory", usedMemory);
      stats.put("jvm_memoryUtilization", (double) usedMemory / maxMemory);

      // CPU statistics
      stats.put("jvm_availableProcessors", runtime.availableProcessors());
      stats.put("jvm_systemLoadAverage", osMXBean.getSystemLoadAverage());

      // Heap memory details
      stats.put("jvm_heapMemoryUsed", memoryMXBean.getHeapMemoryUsage().getUsed());
      stats.put("jvm_heapMemoryMax", memoryMXBean.getHeapMemoryUsage().getMax());
      stats.put("jvm_nonHeapMemoryUsed", memoryMXBean.getNonHeapMemoryUsage().getUsed());

    } catch (Exception e) {
      LOGGER.warn("Error collecting JVM resource statistics", e);
    }
    return stats;
  }
}
