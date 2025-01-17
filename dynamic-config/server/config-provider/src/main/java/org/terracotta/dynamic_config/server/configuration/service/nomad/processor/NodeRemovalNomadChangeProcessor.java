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
package org.terracotta.dynamic_config.server.configuration.service.nomad.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.ClusterState;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.nomad.NodeRemovalNomadChange;
import org.terracotta.dynamic_config.api.service.ClusterValidator;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.api.server.DynamicConfigEventFiring;
import org.terracotta.dynamic_config.api.server.NomadChangeProcessor;
import org.terracotta.nomad.server.NomadException;

import static java.util.Objects.requireNonNull;

/**
 * @author Mathieu Carbou
 */
public class NodeRemovalNomadChangeProcessor implements NomadChangeProcessor<NodeRemovalNomadChange> {
  private static final Logger LOGGER = LoggerFactory.getLogger(NodeRemovalNomadChangeProcessor.class);

  private final TopologyService topologyService;
  private final DynamicConfigEventFiring dynamicConfigEventFiring;

  public NodeRemovalNomadChangeProcessor(TopologyService topologyService, DynamicConfigEventFiring dynamicConfigEventFiring) {
    this.topologyService = requireNonNull(topologyService);
    this.dynamicConfigEventFiring = requireNonNull(dynamicConfigEventFiring);
  }

  @Override
  public void validate(NodeContext baseConfig, NodeRemovalNomadChange change) throws NomadException {
    try {
      Cluster updated = change.apply(baseConfig.getCluster());
      new ClusterValidator(updated).validate(ClusterState.ACTIVATED);
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

    dynamicConfigEventFiring.onNodeRemoval(change.getStripeUID(), node);
  }
}
