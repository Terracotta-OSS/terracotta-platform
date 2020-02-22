/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.server.nomad;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.nomad.DynamicConfigNomadChange;
import org.terracotta.dynamic_config.server.nomad.processor.NomadChangeProcessor;
import org.terracotta.nomad.client.change.NomadChange;
import org.terracotta.nomad.server.ChangeApplicator;
import org.terracotta.nomad.server.NomadException;
import org.terracotta.nomad.server.PotentialApplicationResult;

import static java.util.Objects.requireNonNull;
import static org.terracotta.nomad.server.PotentialApplicationResult.allow;
import static org.terracotta.nomad.server.PotentialApplicationResult.reject;

public class ConfigChangeApplicator implements ChangeApplicator<NodeContext> {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigChangeApplicator.class);

  private final int stripeId;
  private final String nodeName;
  private final NomadChangeProcessor<DynamicConfigNomadChange> processor;

  public ConfigChangeApplicator(int stripeId, String nodeName, NomadChangeProcessor<DynamicConfigNomadChange> processor) {
    this.stripeId = stripeId;
    this.nodeName = nodeName;
    this.processor = processor;
  }

  @Override
  public PotentialApplicationResult<NodeContext> tryApply(NodeContext baseConfig, NomadChange change) {
    if (!(change instanceof DynamicConfigNomadChange)) {
      return reject("Not a " + DynamicConfigNomadChange.class.getSimpleName() + ": " + change.getClass().getName());
    }

    DynamicConfigNomadChange dynamicConfigNomadChange = (DynamicConfigNomadChange) change;

    try {
      // validate the change thanks to external processors
      processor.validate(baseConfig, dynamicConfigNomadChange);

      // if the change is valid, we apply it on the topology, for all the nodes,
      // to generate a config repository that is the same everywhere
      Cluster original = baseConfig == null ? null : baseConfig.getCluster();
      Cluster updated = dynamicConfigNomadChange.apply(original);
      if (updated == null) {
        throw new AssertionError();
      }

      return allow(newConfiguration(baseConfig, updated));
    } catch (RuntimeException | NomadException e) {
      LOGGER.warn("Nomad change: {} rejected with error: {}", change.getSummary(), e.getMessage(), e);
      return reject(e.getMessage());
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
    return baseConfig == null ? new NodeContext(updated, stripeId, nodeName) : baseConfig.withCluster(updated);
  }
}
