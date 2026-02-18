/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2026
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
import org.terracotta.dynamic_config.api.model.DisasterRecoveryMode;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.UID;
import org.terracotta.dynamic_config.api.model.nomad.ClusterActivationNomadChange;
import org.terracotta.dynamic_config.api.server.NomadChangeProcessor;
import org.terracotta.nomad.server.NomadException;

import java.util.List;

import static java.util.Objects.requireNonNull;

public class ClusterActivationNomadChangeProcessor implements NomadChangeProcessor<ClusterActivationNomadChange> {
  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterActivationNomadChangeProcessor.class);

  private final UID nodeUID;

  public ClusterActivationNomadChangeProcessor(UID nodeUID) {
    this.nodeUID = requireNonNull(nodeUID);
  }

  @Override
  public void validate(NodeContext baseConfig, ClusterActivationNomadChange change) throws NomadException {
    LOGGER.info("Validating change: {}", change.getSummary());
    if (baseConfig != null) {
      throw new NomadException("Found an existing configuration: " + baseConfig);
    }

    Cluster cluster = change.getCluster();
    if (!cluster.containsNode(nodeUID)) {
      throw new NomadException("Node: " + nodeUID + " not found in cluster: " + cluster);
    }

    List<String> replicaNodes = cluster.getNodes().stream()
      .filter(node -> DisasterRecoveryMode.fromNode(node) == DisasterRecoveryMode.REPLICA)
      .map(Node::getName).sorted().toList();

    if (!replicaNodes.isEmpty()) {
      throw new NomadException("Nodes with names: " + replicaNodes + " have replica-mode enabled. " +
        "A cluster cannot be activated if replica-mode is enabled on any node.");
    }
  }

  @Override
  public void apply(ClusterActivationNomadChange change) {
    // no-op
  }
}
