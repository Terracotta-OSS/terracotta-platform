/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.service.management;

import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.service.DynamicConfigEventing;
import com.terracottatech.dynamic_config.service.EventRegistration;
import com.terracottatech.dynamic_config.util.PropertiesWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.entity.CommonServerEntity;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.notification.ContextualNotification;
import org.terracotta.management.service.monitoring.EntityManagementRegistry;
import org.terracotta.management.service.monitoring.EntityMonitoringService;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

public class DynamicConfigCommonEntity implements CommonServerEntity<EntityMessage, EntityResponse> {

  private static final Logger LOGGER = LoggerFactory.getLogger(DynamicConfigCommonEntity.class);

  final EntityManagementRegistry managementRegistry;
  final boolean active;

  private final DynamicConfigEventing dynamicConfigEventing;
  private final List<EventRegistration> eventRegistrations = new ArrayList<>(3);

  private volatile boolean listening;

  public DynamicConfigCommonEntity(EntityManagementRegistry managementRegistry, DynamicConfigEventing dynamicConfigEventing) {
    // these can be null if management is not wired or if dynamic config is not available
    this.managementRegistry = managementRegistry;
    this.dynamicConfigEventing = dynamicConfigEventing;
    this.active = managementRegistry != null && dynamicConfigEventing != null;
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
      if (listening) {
        listening = false;
        eventRegistrations.forEach(EventRegistration::unregister);
        eventRegistrations.clear();
      }
      managementRegistry.close();
    }
  }

  final void listen() {
    if (!listening) {
      listening = true;

      EntityMonitoringService monitoringService = managementRegistry.getMonitoringService();

      Context source = Context.create("consumerId", String.valueOf(monitoringService.getConsumerId())).with("type", "DynamicConfig");

      eventRegistrations.add(dynamicConfigEventing.onNewRuntimeConfiguration((nodeContext, configuration) -> {
        Map<String, String> data = new TreeMap<>();
        data.put("change", configuration.toString());
        data.put("runtimeConfig", topologyToConfig(nodeContext.getCluster()));
        data.put("appliedAtRuntime", "true");
        data.put("restartRequired", "false");
        String type = configuration.getValue() == null ? "DYNAMIC_CONFIG_UNSET" : "DYNAMIC_CONFIG_SET";
        monitoringService.pushNotification(new ContextualNotification(source, type, data));
      }));

      eventRegistrations.add(dynamicConfigEventing.onNewUpcomingConfiguration((nodeContext, configuration) -> {
        Map<String, String> data = new TreeMap<>();
        data.put("change", configuration.toString());
        data.put("upcomingConfig", topologyToConfig(nodeContext.getCluster()));
        data.put("appliedAtRuntime", "false");
        data.put("restartRequired", "true");
        String type = configuration.getValue() == null ? "DYNAMIC_CONFIG_UNSET" : "DYNAMIC_CONFIG_SET";
        monitoringService.pushNotification(new ContextualNotification(source, type, data));
      }));

      eventRegistrations.add(dynamicConfigEventing.onNewTopologyCommitted((version, nodeContext) -> {
        Map<String, String> data = new TreeMap<>();
        data.put("version", String.valueOf(version));
        data.put("upcomingConfig", topologyToConfig(nodeContext.getCluster()));
        monitoringService.pushNotification(new ContextualNotification(source, "DYNAMIC_CONFIG_COMMITTED", data));
      }));

      LOGGER.info("Activated management and monitoring for dynamic configuration");
    }
  }

  private static String topologyToConfig(Cluster cluster) {
    Properties properties = cluster.toProperties(false, true);
    try (StringWriter out = new StringWriter()) {
      PropertiesWriter.store(out, properties, "Configurations:");
      return out.toString();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
