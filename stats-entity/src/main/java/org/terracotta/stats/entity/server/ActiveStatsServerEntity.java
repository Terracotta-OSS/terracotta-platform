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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.entity.StateDumpCollector;
import org.terracotta.management.registry.CombiningCapabilityManagementSupport;
import org.terracotta.management.service.monitoring.EntityManagementRegistry;
import org.terracotta.management.service.monitoring.SharedEntityManagementRegistry;
import org.terracotta.stats.entity.common.Stats;
import org.terracotta.stats.entity.common.StatsConfig;
import org.terracotta.voltron.proxy.server.ActiveProxiedServerEntity;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;


class ActiveStatsServerEntity extends ActiveProxiedServerEntity<Void, Void, StatsCallback> implements Stats {

  private static final Logger LOGGER = LoggerFactory.getLogger(ActiveStatsServerEntity.class);

  private final StatsConfig config;
  private final CombiningCapabilityManagementSupport capabilityManagementSupport;

  ActiveStatsServerEntity(StatsConfig config, EntityManagementRegistry entityManagementRegistry, SharedEntityManagementRegistry sharedEntityManagementRegistry, TopologyService topologyService) {
    this.config = config;
    this.capabilityManagementSupport = new CombiningCapabilityManagementSupport(sharedEntityManagementRegistry, entityManagementRegistry);
  }

  @Override
  public void destroy() {
    super.destroy();
  }

  @Override
  public void createNew() {
    super.createNew();
    LOGGER.trace("createNew()");
  }

  @Override
  public void loadExisting() {
    super.loadExisting();
    LOGGER.trace("loadExisting()");
  }

  @Override
  protected void dumpState(StateDumpCollector dump) {
    // No state to dump
  }

  @Override
  public Future<Map<String, Object>> collectCacheStatistics() {
    Map<String, Object> stats = new HashMap<>();
    try {
      // Query all statistics from all capabilities that support cache statistics
      capabilityManagementSupport.getCapabilities().forEach(capability -> {
        try {
          capabilityManagementSupport.withCapability(capability.getName())
              .queryAllStatistics()
              .build()
              .execute()
              .forEach(contextualStats -> {
                contextualStats.getStatistics().forEach((key, value) -> {
                  if (key.toLowerCase().contains("cache") || key.toLowerCase().contains("hit") || key.toLowerCase().contains("miss")) {
                    stats.put(key, value.getLatestSampleValue().orElse(null));
                  }
                });
              });
        } catch (Exception e) {
          LOGGER.warn("Failed to collect statistics from capability {}: {}", capability.getName(), e.getMessage());
        }
      });
    } catch (Exception e) {
      LOGGER.error("Error collecting cache statistics", e);
    }

    if (stats.isEmpty()) {
      // Fallback to basic server stats if no cache stats available
      stats.put("cacheHitRate", 0.0);
      stats.put("cacheMissRate", 0.0);
      stats.put("cacheSize", 0L);
      stats.put("cacheEvictions", 0L);
    }

    LOGGER.info("Collected cache statistics: {}", stats);
    return CompletableFuture.completedFuture(stats);
  }

  @Override
  public Future<Map<String, Object>> collectDatasetStatistics() {
    Map<String, Object> stats = new HashMap<>();
    try {
      // Query all statistics from all capabilities that support dataset statistics
      capabilityManagementSupport.getCapabilities().forEach(capability -> {
        try {
          capabilityManagementSupport.withCapability(capability.getName())
              .queryAllStatistics()
              .build()
              .execute()
              .forEach(contextualStats -> {
                contextualStats.getStatistics().forEach((key, value) -> {
                  if (key.toLowerCase().contains("dataset") || key.toLowerCase().contains("storage") ||
                      key.toLowerCase().contains("operation") || key.toLowerCase().contains("size")) {
                    stats.put(key, value.getLatestSampleValue().orElse(null));
                  }
                });
              });
        } catch (Exception e) {
          LOGGER.warn("Failed to collect statistics from capability {}: {}", capability.getName(), e.getMessage());
        }
      });
    } catch (Exception e) {
      LOGGER.error("Error collecting dataset statistics", e);
    }

    if (stats.isEmpty()) {
      // Fallback to basic dataset stats if no dataset stats available
      stats.put("datasetStorageUsage", 0L);
      stats.put("datasetOperations", 0L);
      stats.put("datasetSize", 0L);
      stats.put("datasetReadOperations", 0L);
      stats.put("datasetWriteOperations", 0L);
    }

    LOGGER.info("Collected dataset statistics: {}", stats);
    return CompletableFuture.completedFuture(stats);
  }

  @Override
  public Future<Map<String, Object>> collectServerStatistics() {
    Map<String, Object> stats = new HashMap<>();

    // Collect CPU usage
    OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
    double cpuLoad = osBean.getSystemLoadAverage();
    if (cpuLoad < 0) {
      // Fallback for systems that don't support load average
      cpuLoad = 0.0;
    }
    stats.put("cpuUsage", cpuLoad);

    // Collect memory usage
    MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    long usedHeap = memoryBean.getHeapMemoryUsage().getUsed();
    long maxHeap = memoryBean.getHeapMemoryUsage().getMax();
    stats.put("heapMemoryUsed", usedHeap);
    stats.put("heapMemoryMax", maxHeap);

    // Collect number of connections (simulated - in real implementation would track actual connections)
    int connectionCount = getClients().size();
    stats.put("activeConnections", connectionCount);

    // Additional server stats
    stats.put("uptime", ManagementFactory.getRuntimeMXBean().getUptime());
    stats.put("threadCount", ManagementFactory.getThreadMXBean().getThreadCount());

    LOGGER.info("Collected server statistics: {}", stats);
    return CompletableFuture.completedFuture(stats);
  }

}
