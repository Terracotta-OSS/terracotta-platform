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
package org.terracotta.dynamic_config.server.configuration.service.nomad.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.nomad.NodeRemovalNomadChange;
import org.terracotta.dynamic_config.api.service.ClusterValidator;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.server.api.DynamicConfigEventFiring;
import org.terracotta.dynamic_config.server.api.NomadChangeProcessor;
import org.terracotta.nomad.server.NomadException;
import org.terracotta.server.ServerEnv;
import org.terracotta.server.ServerMBean;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/**
 * @author Mathieu Carbou
 */
public class NodeRemovalNomadChangeProcessor implements NomadChangeProcessor<NodeRemovalNomadChange> {
  private static final Logger LOGGER = LoggerFactory.getLogger(NodeRemovalNomadChangeProcessor.class);
  private static final String PLATFORM_MBEAN_OPERATION_NAME = "removePassive";

  private final TopologyService topologyService;
  private final DynamicConfigEventFiring dynamicConfigEventFiring;
  private final MBeanServer mbeanServer = ServerEnv.getServer().getManagement().getMBeanServer();

  public NodeRemovalNomadChangeProcessor(TopologyService topologyService, DynamicConfigEventFiring dynamicConfigEventFiring) {
    this.topologyService = requireNonNull(topologyService);
    this.dynamicConfigEventFiring = requireNonNull(dynamicConfigEventFiring);
  }

  @Override
  public void validate(NodeContext baseConfig, NodeRemovalNomadChange change) throws NomadException {
    try {
      checkMBeanOperation();
      Cluster updated = change.apply(baseConfig.getCluster());
      new ClusterValidator(updated).validate();
    } catch (RuntimeException e) {
      throw new NomadException("Error when trying to apply: '" + change.getSummary() + "': " + e.getMessage(), e);
    }
  }

  @Override
  public final void apply(NodeRemovalNomadChange change) throws NomadException {
    Cluster runtime = topologyService.getRuntimeNodeContext().getCluster();
    Node node = change.getNode();
    if (!runtime.containsNode(node.getUID())) {
      return;
    }

    try {
      ObjectName objectName = ServerMBean.createMBeanName("TopologyMBean");
      LOGGER.info("Removing node: {} from stripe ID: {}", node.getName(), runtime.getStripe(change.getStripeUID()).get().getName());
      LOGGER.debug("Calling mBean {}#{}", objectName, PLATFORM_MBEAN_OPERATION_NAME);
      mbeanServer.invoke(
          objectName,
          PLATFORM_MBEAN_OPERATION_NAME,
          new Object[]{node.getHostname(), node.getPort().orDefault(), node.getGroupPort().orDefault()},
          new String[]{String.class.getName(), int.class.getName(), int.class.getName()}
      );

      dynamicConfigEventFiring.onNodeRemoval(change.getStripeUID(), node);
    } catch (RuntimeException | JMException e) {
      throw new NomadException("Error when applying: '" + change.getSummary() + "': " + e.getMessage(), e);
    }
  }

  private void checkMBeanOperation() {
    boolean canCall;
    try {
      canCall = Stream
          .of(mbeanServer.getMBeanInfo(ServerMBean.createMBeanName("TopologyMBean")).getOperations())
          .anyMatch(attr -> PLATFORM_MBEAN_OPERATION_NAME.equals(attr.getName()));
    } catch (JMException e) {
      LOGGER.error("MBeanServer::getMBeanInfo resulted in:", e);
      canCall = false;
    }
    if (!canCall) {
      throw new IllegalStateException("Unable to invoke MBean operation to detach a node");
    }
  }
}
