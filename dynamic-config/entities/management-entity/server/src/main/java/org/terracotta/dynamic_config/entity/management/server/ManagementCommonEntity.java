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
package org.terracotta.dynamic_config.entity.management.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.UID;
import org.terracotta.dynamic_config.api.model.nomad.SettingNomadChange;
import org.terracotta.dynamic_config.api.service.Props;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.server.api.DynamicConfigEventService;
import org.terracotta.dynamic_config.server.api.DynamicConfigListener;
import org.terracotta.dynamic_config.server.api.EventRegistration;
import org.terracotta.entity.CommonServerEntity;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.StateDumpCollector;
import org.terracotta.inet.HostPort;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.notification.ContextualNotification;
import org.terracotta.management.service.monitoring.EntityManagementRegistry;
import org.terracotta.management.service.monitoring.EntityMonitoringService;
import org.terracotta.nomad.messages.AcceptRejectResponse;
import org.terracotta.nomad.messages.CommitMessage;
import org.terracotta.nomad.messages.PrepareMessage;
import org.terracotta.nomad.messages.RollbackMessage;
import org.terracotta.nomad.server.ChangeState;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class ManagementCommonEntity implements CommonServerEntity<EntityMessage, EntityResponse> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ManagementCommonEntity.class);

  final EntityManagementRegistry managementRegistry;
  final boolean active;

  private final DynamicConfigEventService dynamicConfigEventService;
  private final TopologyService topologyService;
  private volatile EventRegistration eventRegistration;

  public ManagementCommonEntity(EntityManagementRegistry managementRegistry, DynamicConfigEventService dynamicConfigEventService, TopologyService topologyService) {
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

      Context source = Context.create("consumerId", String.valueOf(monitoringService.getConsumerId())).with("type", "DynamicConfig");

      eventRegistration = dynamicConfigEventService.register(new DynamicConfigListener() {
        @Override
        public void onSettingChanged(SettingNomadChange change, Cluster updated) {
          NodeContext nodeContext = topologyService.getRuntimeNodeContext();
          boolean restartRequired = !change.canUpdateRuntimeTopology(nodeContext);
          Map<String, String> data = new TreeMap<>();
          data.put("change", change.toString());
          data.put("result", Props.toString(updated.toProperties(false, false, true)));
          data.put("operation", change.getOperation().name().toLowerCase());
          data.put("setting", change.getSetting().toString());
          data.put("name", change.getName());
          data.put("value", change.getValue());
          data.put("scope", change.getApplicability().getLevel().name().toLowerCase());
          data.put("summary", change.getSummary());
          switch (change.getApplicability().getLevel()) {
            case CLUSTER:
              break;
            case STRIPE:
              data.put("stripeName", change.getApplicability().getStripe(updated).get().getName());
              break;
            case NODE:
              data.put("stripeName", change.getApplicability().getStripe(updated).get().getName());
              data.put("nodeName", change.getApplicability().getNode(updated).get().getName());
              break;

          }
          data.put("appliedAtRuntime", String.valueOf(!restartRequired));
          data.put("restartRequired", String.valueOf(restartRequired));
          String type = "DYNAMIC_CONFIG_" + change.getOperation();
          monitoringService.pushNotification(new ContextualNotification(source, type, data));
        }

        @Override
        public void onNewConfigurationSaved(NodeContext nodeContext, Long version) {
          Map<String, String> data = new TreeMap<>();
          data.put("version", String.valueOf(version));
          data.put("upcomingConfig", Props.toString(nodeContext.getCluster().toProperties(false, false, true)));
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
        public void onNomadCommit(CommitMessage message, AcceptRejectResponse response, ChangeState<NodeContext> changeState) {
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
        public void onNodeRemoval(UID stripeUID, Node removedNode) {
          Map<String, String> data = new TreeMap<>();
          data.put("stripeUID", stripeUID.toString());
          data.put("stripeName", topologyService.getRuntimeNodeContext().getCluster().getStripe(stripeUID).get().getName());
          data.put("nodeName", removedNode.getName());
          data.put("nodeHostname", removedNode.getHostname());
          data.put("nodeAddress", removedNode.getInternalHostPort().toString());
          data.put("nodeInternalAddress", removedNode.getInternalHostPort().toString());
          removedNode.getPublicHostPort().ifPresent(addr -> data.put("nodePublicAddress", addr.toString()));
          monitoringService.pushNotification(new ContextualNotification(source, "DYNAMIC_CONFIG_NODE_REMOVED", data));
        }

        @Override
        public void onNodeAddition(UID stripeUID, Node addedNode) {
          Map<String, String> data = new TreeMap<>();
          data.put("stripeUID", stripeUID.toString());
          data.put("stripeName", topologyService.getRuntimeNodeContext().getCluster().getStripe(stripeUID).get().getName());
          data.put("nodeName", addedNode.getName());
          data.put("nodeHostname", addedNode.getHostname());
          data.put("nodeAddress", addedNode.getInternalHostPort().toString());
          data.put("nodeInternalAddress", addedNode.getInternalHostPort().toString());
          addedNode.getPublicHostPort().ifPresent(addr -> data.put("nodePublicAddress", addr.toString()));
          monitoringService.pushNotification(new ContextualNotification(source, "DYNAMIC_CONFIG_NODE_ADDED", data));
        }

        @Override
        public void onStripeAddition(Stripe addedStripe) {
          Map<String, String> data = new TreeMap<>();
          data.put("stripeUID", addedStripe.getUID().toString());
          data.put("stripeName", addedStripe.getName());
          data.put("nodeUIDs", addedStripe.getNodes().stream().map(Node::getUID).map(UID::toString).collect(Collectors.joining(",")));
          data.put("nodeNames", addedStripe.getNodes().stream().map(Node::getName).collect(Collectors.joining(",")));
          data.put("nodeAddresses", addedStripe.getNodes().stream().map(Node::getInternalHostPort).map(HostPort::toString).collect(Collectors.joining(",")));
          monitoringService.pushNotification(new ContextualNotification(source, "DYNAMIC_CONFIG_STRIPE_ADDED", data));
        }

        @Override
        public void onStripeRemoval(Stripe removedStripe) {
          Map<String, String> data = new TreeMap<>();
          data.put("stripeUID", removedStripe.getUID().toString());
          data.put("stripeName", removedStripe.getName());
          data.put("nodeUIDs", removedStripe.getNodes().stream().map(Node::getUID).map(UID::toString).collect(Collectors.joining(",")));
          data.put("nodeNames", removedStripe.getNodes().stream().map(Node::getName).collect(Collectors.joining(",")));
          data.put("nodeAddresses", removedStripe.getNodes().stream().map(Node::getInternalHostPort).map(HostPort::toString).collect(Collectors.joining(",")));
          monitoringService.pushNotification(new ContextualNotification(source, "DYNAMIC_CONFIG_STRIPE_REMOVED", data));
        }
      });

      LOGGER.info("Activated management and monitoring for dynamic configuration");
    }
  }
}
