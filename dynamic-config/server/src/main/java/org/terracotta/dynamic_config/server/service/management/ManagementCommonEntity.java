/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.server.service.management;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Configuration;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Props;
import org.terracotta.dynamic_config.api.service.DynamicConfigEventService;
import org.terracotta.dynamic_config.api.service.DynamicConfigListener;
import org.terracotta.dynamic_config.api.service.EventRegistration;
import org.terracotta.entity.CommonServerEntity;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.notification.ContextualNotification;
import org.terracotta.management.service.monitoring.EntityManagementRegistry;
import org.terracotta.management.service.monitoring.EntityMonitoringService;
import org.terracotta.nomad.messages.AcceptRejectResponse;
import org.terracotta.nomad.messages.CommitMessage;
import org.terracotta.nomad.messages.PrepareMessage;
import org.terracotta.nomad.messages.RollbackMessage;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

public class ManagementCommonEntity implements CommonServerEntity<EntityMessage, EntityResponse> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ManagementCommonEntity.class);

  final EntityManagementRegistry managementRegistry;
  final boolean active;

  private final DynamicConfigEventService dynamicConfigEventService;
  private volatile EventRegistration eventRegistration;

  public ManagementCommonEntity(EntityManagementRegistry managementRegistry, DynamicConfigEventService dynamicConfigEventService) {
    // these can be null if management is not wired or if dynamic config is not available
    this.managementRegistry = managementRegistry;
    this.dynamicConfigEventService = dynamicConfigEventService;
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

  final void listen() {
    if (eventRegistration == null) {
      EntityMonitoringService monitoringService = managementRegistry.getMonitoringService();

      Context source = Context.create("consumerId", String.valueOf(monitoringService.getConsumerId())).with("type", "DynamicConfig");

      eventRegistration = dynamicConfigEventService.register(new DynamicConfigListener() {
        @Override
        public void onNewConfigurationAppliedAtRuntime(NodeContext nodeContext, Configuration configuration) {
          Map<String, String> data = new TreeMap<>();
          data.put("change", configuration.toString());
          data.put("runtimeConfig", topologyToConfig(nodeContext.getCluster()));
          data.put("appliedAtRuntime", "true");
          data.put("restartRequired", "false");
          String type = configuration.getValue() == null ? "DYNAMIC_CONFIG_UNSET" : "DYNAMIC_CONFIG_SET";
          monitoringService.pushNotification(new ContextualNotification(source, type, data));
        }

        @Override
        public void onNewConfigurationPendingRestart(NodeContext nodeContext, Configuration configuration) {
          Map<String, String> data = new TreeMap<>();
          data.put("change", configuration.toString());
          data.put("upcomingConfig", topologyToConfig(nodeContext.getCluster()));
          data.put("appliedAtRuntime", "false");
          data.put("restartRequired", "true");
          String type = configuration.getValue() == null ? "DYNAMIC_CONFIG_UNSET" : "DYNAMIC_CONFIG_SET";
          monitoringService.pushNotification(new ContextualNotification(source, type, data));
        }

        @Override
        public void onNewConfigurationSaved(NodeContext nodeContext, Long version) {
          Map<String, String> data = new TreeMap<>();
          data.put("version", String.valueOf(version));
          data.put("upcomingConfig", topologyToConfig(nodeContext.getCluster()));
          monitoringService.pushNotification(new ContextualNotification(source, "DYNAMIC_CONFIG_SAVED", data));
        }

        @Override
        public void onNomadPrepare(PrepareMessage message, AcceptRejectResponse response) {
          Map<String, String> data = new TreeMap<>();
          data.put("changeSummary", message.getChange().getSummary());
          data.put("changeUuid", message.getChangeUuid().toString());
          data.put("version", String.valueOf(message.getVersionNumber()));
          data.put("host", String.valueOf(message.getMutationHost()));
          data.put("user", String.valueOf(message.getMutationUser()));
          data.put("accepted", String.valueOf(response.isAccepted()));
          if (!response.isAccepted()) {
            data.put("reason", response.getRejectionReason().toString());
            data.put("error", response.getRejectionMessage());
          }
          monitoringService.pushNotification(new ContextualNotification(source, "DYNAMIC_CONFIG_NOMAD_PREPARE", data));
        }

        @Override
        public void onNomadCommit(CommitMessage message, AcceptRejectResponse response) {
          Map<String, String> data = new TreeMap<>();
          data.put("changeUuid", message.getChangeUuid().toString());
          data.put("host", String.valueOf(message.getMutationHost()));
          data.put("user", String.valueOf(message.getMutationUser()));
          data.put("accepted", String.valueOf(response.isAccepted()));
          if (!response.isAccepted()) {
            data.put("reason", response.getRejectionReason().toString());
            data.put("error", response.getRejectionMessage());
          }
          monitoringService.pushNotification(new ContextualNotification(source, "DYNAMIC_CONFIG_NOMAD_COMMIT", data));
        }

        @Override
        public void onNomadRollback(RollbackMessage message, AcceptRejectResponse response) {
          Map<String, String> data = new TreeMap<>();
          data.put("changeUuid", message.getChangeUuid().toString());
          data.put("host", String.valueOf(message.getMutationHost()));
          data.put("user", String.valueOf(message.getMutationUser()));
          data.put("accepted", String.valueOf(response.isAccepted()));
          if (!response.isAccepted()) {
            data.put("reason", response.getRejectionReason().toString());
            data.put("error", response.getRejectionMessage());
          }
          monitoringService.pushNotification(new ContextualNotification(source, "DYNAMIC_CONFIG_NOMAD_ROLLBACK", data));
        }

        @Override
        public void onNodeRemoval(int stripeId, Node removedNode) {
          Map<String, String> data = new TreeMap<>();
          data.put("stripeId", String.valueOf(stripeId));
          data.put("nodeName", removedNode.getNodeName());
          data.put("nodeHostname", removedNode.getNodeHostname());
          data.put("nodeAddress", removedNode.getNodeAddress().toString());
          data.put("nodeInternalAddress", removedNode.getNodeInternalAddress().toString());
          data.put("nodePublicAddress", removedNode.getNodePublicAddress().toString());
          monitoringService.pushNotification(new ContextualNotification(source, "DYNAMIC_CONFIG_NODE_REMOVED", data));
        }

        @Override
        public void onNodeAddition(int stripeId, Node addedNode) {
          Map<String, String> data = new TreeMap<>();
          data.put("stripeId", String.valueOf(stripeId));
          data.put("nodeName", addedNode.getNodeName());
          data.put("nodeHostname", addedNode.getNodeHostname());
          data.put("nodeAddress", addedNode.getNodeAddress().toString());
          data.put("nodeInternalAddress", addedNode.getNodeInternalAddress().toString());
          data.put("nodePublicAddress", addedNode.getNodePublicAddress().toString());
          monitoringService.pushNotification(new ContextualNotification(source, "DYNAMIC_CONFIG_NODE_ADDED", data));
        }
      });

      LOGGER.info("Activated management and monitoring for dynamic configuration");
    }
  }

  private static String topologyToConfig(Cluster cluster) {
    Properties properties = cluster.toProperties(false, true);
    try (StringWriter out = new StringWriter()) {
      Props.store(out, properties, "Configurations:");
      return out.toString();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
