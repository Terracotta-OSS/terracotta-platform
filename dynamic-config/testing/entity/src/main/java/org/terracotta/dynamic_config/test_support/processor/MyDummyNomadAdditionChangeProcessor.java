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
package org.terracotta.dynamic_config.test_support.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.ClusterState;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.nomad.NodeAdditionNomadChange;
import org.terracotta.dynamic_config.api.service.ClusterValidator;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.server.api.DynamicConfigEventFiring;
import org.terracotta.dynamic_config.server.api.NomadChangeProcessor;
import org.terracotta.nomad.server.NomadException;

import javax.management.MBeanServer;

import static java.util.Objects.requireNonNull;
import static org.terracotta.dynamic_config.test_support.processor.ServerCrasher.crash;

public class MyDummyNomadAdditionChangeProcessor implements NomadChangeProcessor<NodeAdditionNomadChange> {
  private static final Logger LOGGER = LoggerFactory.getLogger(MyDummyNomadAdditionChangeProcessor.class);
  private static final String PLATFORM_MBEAN_OPERATION_NAME = "addPassive";
  private static final String failAtPrepare = "prepareAddition-failure";
  private static final String killAtPrepare = "killAddition-prepare";
  private static final String killAtCommit = "killAddition-commit";
  private static final String failoverKey = "failoverAddition";
  private static final String attachStatusKey = "attachStatus";
  private final TopologyService topologyService;
  private final DynamicConfigEventFiring dynamicConfigEventFiring;
  private final MBeanServer mbeanServer;

  public MyDummyNomadAdditionChangeProcessor(TopologyService topologyService, DynamicConfigEventFiring dynamicConfigEventFiring, MBeanServer mbeanServer) {
    this.mbeanServer = mbeanServer;
    this.topologyService = requireNonNull(topologyService);
    this.dynamicConfigEventFiring = requireNonNull(dynamicConfigEventFiring);
  }

  @Override
  public void validate(NodeContext baseConfig, NodeAdditionNomadChange change) throws NomadException {
    if (failAtPrepare.equals(topologyService.getUpcomingNodeContext().getNode().getTcProperties().orDefault().get(attachStatusKey))) {
      throw new NomadException("Invalid addition fail at prepare");
    }

    LOGGER.info("Validating change: {}", change.getSummary());
    if (baseConfig == null) {
      throw new NomadException("Existing config must not be null");
    }
    try {
      Cluster updated = change.apply(baseConfig.getCluster());
      new ClusterValidator(updated).validate(ClusterState.ACTIVATED);
    } catch (RuntimeException e) {
      throw new NomadException("Error when trying to apply: '" + change.getSummary() + "': " + e.getMessage(), e);
    }

    // cause failure when in prepare phase
    if (killAtPrepare.equals(topologyService.getUpcomingNodeContext().getNode().getTcProperties().orDefault().get(failoverKey))) {
      crash();
    }
  }

  @Override
  public void apply(NodeAdditionNomadChange change) throws NomadException {
    Cluster runtime = topologyService.getRuntimeNodeContext().getCluster();
    if (runtime.containsNode(change.getNode().getUID())) {
      return;
    }

    // cause failover when in commit phase
    if (killAtCommit.equals(topologyService.getUpcomingNodeContext().getNode().getTcProperties().orDefault().get(failoverKey))) {
      crash();
    }

    try {
      Node node = change.getNode();
      LOGGER.info("Adding node: {} to stripe UID: {}", node.getName(), change.getStripeUID());
      dynamicConfigEventFiring.onNodeAddition(change.getStripeUID(), node);
    } catch (RuntimeException e) {
      throw new NomadException("Error when applying: '" + change.getSummary() + "': " + e.getMessage(), e);
    }
  }
}
