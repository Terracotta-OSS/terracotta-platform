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
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.UID;
import org.terracotta.dynamic_config.api.model.nomad.SettingNomadChange;
import org.terracotta.dynamic_config.api.server.DynamicConfigEventService;
import org.terracotta.dynamic_config.api.server.DynamicConfigListener;
import org.terracotta.dynamic_config.api.server.EventRegistration;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.entity.CommonServerEntity;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.StateDumpCollector;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.notification.ContextualNotification;
import org.terracotta.management.service.monitoring.EntityManagementRegistry;
import org.terracotta.management.service.monitoring.EntityMonitoringService;
import org.terracotta.nomad.messages.AcceptRejectResponse;
import org.terracotta.nomad.messages.CommitMessage;
import org.terracotta.nomad.messages.PrepareMessage;
import org.terracotta.nomad.messages.RollbackMessage;
import org.terracotta.nomad.server.ChangeState;

public class StatsCommonEntity implements CommonServerEntity<EntityMessage, EntityResponse> {

  private static final Logger LOGGER = LoggerFactory.getLogger(StatsCommonEntity.class);

  final EntityManagementRegistry managementRegistry;
  final boolean active;

  private final DynamicConfigEventService dynamicConfigEventService;
  private final TopologyService topologyService;
  private volatile EventRegistration eventRegistration;

  public StatsCommonEntity(EntityManagementRegistry managementRegistry, DynamicConfigEventService dynamicConfigEventService, TopologyService topologyService) {
    // these can be null if management is not wired or if dynamic config is not available
    this.managementRegistry = managementRegistry;
    this.dynamicConfigEventService = dynamicConfigEventService;
    this.topologyService = topologyService;
    this.active = managementRegistry != null && dynamicConfigEventService != null;
  }


  @Override
  public final void createNew() {
    if (active) {
      managementRegistry.entityCreated();
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
      managementRegistry.close();
    }
  }

  @Override
  public void addStateTo(StateDumpCollector stateDumpCollector) {
    stateDumpCollector.addState("active", active);
  }

  final void listen() {
    if (eventRegistration == null) {
      EntityMonitoringService monitoringService = managementRegistry.getMonitoringService();
      Context source = Context.create("consumerId", String.valueOf(monitoringService.getConsumerId())).with("type", "Stats");

      // Start a background thread to collect statistics periodically
      Thread statsCollector = new Thread(() -> {
        try {
          while (!Thread.currentThread().isInterrupted()) {
            try {
              // Collect cache statistics
              Map<String, Object> cacheStats = collectCacheStatistics();
              if (!cacheStats.isEmpty()) {
                Map<String, String> data = new TreeMap<>();
                for (Map.Entry<String, Object> entry : cacheStats.entrySet()) {
                  data.put(entry.getKey(), String.valueOf(entry.getValue()));
                }
                monitoringService.pushNotification(new ContextualNotification(source, "STATS_CACHE", data));
              }

              // Collect dataset statistics
              Map<String, Object> datasetStats = collectDatasetStatistics();
              if (!datasetStats.isEmpty()) {
                Map<String, String> data = new TreeMap<>();
                for (Map.Entry<String, Object> entry : datasetStats.entrySet()) {
                  data.put(entry.getKey(), String.valueOf(entry.getValue()));
                }
                monitoringService.pushNotification(new ContextualNotification(source, "STATS_DATASET", data));
              }

              // Collect server statistics
              Map<String, Object> serverStats = collectServerStatistics();
              if (!serverStats.isEmpty()) {
                Map<String, String> data = new TreeMap<>();
                for (Map.Entry<String, Object> entry : serverStats.entrySet()) {
                  data.put(entry.getKey(), String.valueOf(entry.getValue()));
                }
                monitoringService.pushNotification(new ContextualNotification(source, "STATS_SERVER", data));
              }

              // Sleep for 30 seconds before collecting stats again
              Thread.sleep(30000);
            } catch (Exception e) {
              LOGGER.warn("Error collecting statistics", e);
            }
          }
        } catch (Exception e) {
          LOGGER.error("Stats collection thread terminated unexpectedly", e);
        }
      });

      statsCollector.setName("Stats-Collector");
      statsCollector.setDaemon(true);
      statsCollector.start();

      // Also register for dynamic config events
      eventRegistration = dynamicConfigEventService.register(new DynamicConfigListener() {
        @Override
        public void onSettingChanged(SettingNomadChange change, Cluster updated) {
          // Only capture settings that might affect statistics collection
          if (change.getSetting().toString().contains("STATS") ||
              change.getSetting().toString().contains("MONITORING")) {
            Map<String, String> data = new TreeMap<>();
            data.put("setting", change.getSetting().toString());
            data.put("name", change.getName());
            data.put("value", change.getValue());
            data.put("summary", change.getSummary());
            monitoringService.pushNotification(new ContextualNotification(source, "STATS_CONFIG_CHANGED", data));
          }
        }

        @Override
        public void onNewConfigurationSaved(NodeContext nodeContext, Long version) {
          // Implementation not needed for stats collection
        }

        @Override
        public void onNomadPrepare(PrepareMessage message, AcceptRejectResponse response) {
          // Implementation not needed for stats collection
        }

        @Override
        public void onNomadCommit(CommitMessage message, AcceptRejectResponse response, ChangeState<NodeContext> changeState) {
          // Implementation not needed for stats collection
        }

        @Override
        public void onNomadRollback(RollbackMessage message, AcceptRejectResponse response) {
          // Implementation not needed for stats collection
        }

        @Override
        public void onNodeRemoval(UID stripeUID, Node removedNode) {
          // Track node removal as it affects cluster statistics
          Map<String, String> data = new TreeMap<>();
          data.put("nodeName", removedNode.getName());
          data.put("nodeHostname", removedNode.getHostname());
          monitoringService.pushNotification(new ContextualNotification(source, "STATS_NODE_REMOVED", data));
        }

        @Override
        public void onNodeAddition(UID stripeUID, Node addedNode) {
          // Track node addition as it affects cluster statistics
          Map<String, String> data = new TreeMap<>();
          data.put("nodeName", addedNode.getName());
          data.put("nodeHostname", addedNode.getHostname());
          monitoringService.pushNotification(new ContextualNotification(source, "STATS_NODE_ADDED", data));
        }

        @Override
        public void onStripeAddition(Stripe addedStripe) {
          // Implementation not needed for stats collection
        }

        @Override
        public void onStripeRemoval(Stripe removedStripe) {
          // Implementation not needed for stats collection
        }
      });

      LOGGER.info("Activated statistics collection and monitoring");
    }
  }

  /**
   * Collect cache statistics such as hit/miss rates, size, etc.
   */
  private Map<String, Object> collectCacheStatistics() {
    Map<String, Object> stats = new HashMap<>();
    try {
      // This would typically integrate with a cache monitoring system
      // For demonstration, we'll add some sample metrics
      stats.put("cacheHitCount", 1000);
      stats.put("cacheMissCount", 250);
      stats.put("cacheHitRatio", 0.8);
      stats.put("cacheSize", 10240);
      stats.put("cacheEvictionCount", 50);

      // In a real implementation, you would collect actual cache metrics
      // from the Terracotta cache management system
    } catch (Exception e) {
      LOGGER.warn("Error collecting cache statistics", e);
    }
    return stats;
  }

  /**
   * Collect dataset statistics such as storage usage, operations, etc.
   */
  private Map<String, Object> collectDatasetStatistics() {
    Map<String, Object> stats = new HashMap<>();
    try {
      // This would typically integrate with a dataset monitoring system
      // For demonstration, we'll add some sample metrics
      stats.put("datasetSize", 1048576);
      stats.put("datasetRecordCount", 5000);
      stats.put("datasetReadOperations", 15000);
      stats.put("datasetWriteOperations", 3000);
      stats.put("datasetAvgReadLatency", 5);
      stats.put("datasetAvgWriteLatency", 15);

      // In a real implementation, you would collect actual dataset metrics
      // from the Terracotta storage system
    } catch (Exception e) {
      LOGGER.warn("Error collecting dataset statistics", e);
    }
    return stats;
  }

  /**
   * Collect server statistics such as CPU, memory, connections, etc.
   */
  private Map<String, Object> collectServerStatistics() {
    Map<String, Object> stats = new HashMap<>();
    try {
      // Collect JVM and system metrics
      Runtime runtime = Runtime.getRuntime();
      OperatingSystemMXBean osMXBean = ManagementFactory.getOperatingSystemMXBean();
      MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

      // Memory statistics
      long maxMemory = runtime.maxMemory();
      long allocatedMemory = runtime.totalMemory();
      long freeMemory = runtime.freeMemory();
      long usedMemory = allocatedMemory - freeMemory;

      stats.put("maxMemory", maxMemory);
      stats.put("allocatedMemory", allocatedMemory);
      stats.put("freeMemory", freeMemory);
      stats.put("usedMemory", usedMemory);
      stats.put("memoryUtilization", (double) usedMemory / maxMemory);

      // CPU statistics
      stats.put("availableProcessors", runtime.availableProcessors());
      stats.put("systemLoadAverage", osMXBean.getSystemLoadAverage());

      // Heap memory details
      stats.put("heapMemoryUsed", memoryMXBean.getHeapMemoryUsage().getUsed());
      stats.put("heapMemoryMax", memoryMXBean.getHeapMemoryUsage().getMax());
      stats.put("nonHeapMemoryUsed", memoryMXBean.getNonHeapMemoryUsage().getUsed());

      // In a real implementation, you would also collect:
      // - Connection counts
      // - Thread counts
      // - GC statistics
      // - Network I/O
      // - Disk I/O

    } catch (Exception e) {
      LOGGER.warn("Error collecting server statistics", e);
    }
    return stats;
  }
}
