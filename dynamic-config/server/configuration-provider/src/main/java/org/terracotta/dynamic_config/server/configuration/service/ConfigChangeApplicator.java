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
package org.terracotta.dynamic_config.server.configuration.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.ClusterState;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.UID;
import org.terracotta.dynamic_config.api.model.nomad.DynamicConfigNomadChange;
import org.terracotta.dynamic_config.api.service.ClusterValidator;
import org.terracotta.dynamic_config.server.api.NomadChangeProcessor;
import org.terracotta.nomad.client.change.NomadChange;
import org.terracotta.nomad.server.ChangeApplicator;
import org.terracotta.nomad.server.NomadException;
import org.terracotta.nomad.server.PotentialApplicationResult;

import static java.util.Objects.requireNonNull;
import static org.terracotta.nomad.server.PotentialApplicationResult.reject;

public class ConfigChangeApplicator implements ChangeApplicator<NodeContext> {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigChangeApplicator.class);

  private final UID nodeUID;
  private final NomadChangeProcessor<DynamicConfigNomadChange> processor;

  public ConfigChangeApplicator(UID nodeUID, NomadChangeProcessor<DynamicConfigNomadChange> processor) {
    this.nodeUID = nodeUID;
    this.processor = processor;
  }

  @Override
  public PotentialApplicationResult<NodeContext> tryApply(NodeContext baseConfig, NomadChange change) {
    if (!(change instanceof DynamicConfigNomadChange)) {
      return reject(baseConfig, "Not a " + DynamicConfigNomadChange.class.getSimpleName() + ": " + change.getClass().getName());
    }

    DynamicConfigNomadChange dynamicConfigNomadChange = (DynamicConfigNomadChange) change;

    // if the change is valid, we apply it on the topology, for all the nodes,
    // to generate a configuration directory that is the same everywhere
    Cluster original = baseConfig == null ? null : baseConfig.getCluster();
    Cluster updated = dynamicConfigNomadChange.apply(original);
    if (updated == null) {
      throw new AssertionError();
    }

    NodeContext newConfiguration = newConfiguration(baseConfig, updated);

    try {
      new ClusterValidator(updated).validate(ClusterState.ACTIVATED);
      // validate the change thanks to external processors
      processor.validate(baseConfig, dynamicConfigNomadChange);
      return PotentialApplicationResult.allow(newConfiguration);
    } catch (RuntimeException | NomadException e) {
      LOGGER.warn("Nomad change: {} rejected with error: {}", change.getSummary(), e.getMessage(), e);
      return reject(newConfiguration, e.getMessage());
    }
  }

  @Override
  public void apply(NomadChange change) throws NomadException {
    if (!(change instanceof DynamicConfigNomadChange)) {
      throw new NomadException("Not a " + DynamicConfigNomadChange.class.getSimpleName() + ": " + change);
    }
    DynamicConfigNomadChange dynamicConfigNomadChange = (DynamicConfigNomadChange) change;
    processor.apply(dynamicConfigNomadChange);
  }

  private NodeContext newConfiguration(NodeContext baseConfig, Cluster updated) {
    requireNonNull(updated);
    // - If we are activating this node, there is not yet any existing configuration, so we create one.
    // - If we have updated the topology and our current node is still there, then return a context to be written on disk for the node.
    // - If the updated topology does not contain the node anymore (removal ?) and a base config was there (topology change) then we isolate the node in its own cluster
    return baseConfig == null ? new NodeContext(updated, nodeUID) : baseConfig.withCluster(updated).orElseGet(baseConfig::alone);
  }
}
