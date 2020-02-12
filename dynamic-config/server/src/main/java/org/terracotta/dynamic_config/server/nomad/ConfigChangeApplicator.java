/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.server.nomad;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.server.nomad.processor.NomadChangeProcessor;
import org.terracotta.nomad.client.change.MultiNomadChange;
import org.terracotta.nomad.client.change.NomadChange;
import org.terracotta.nomad.server.ChangeApplicator;
import org.terracotta.nomad.server.NomadException;
import org.terracotta.nomad.server.PotentialApplicationResult;

import java.util.Collections;
import java.util.List;

import static org.terracotta.nomad.server.PotentialApplicationResult.allow;
import static org.terracotta.nomad.server.PotentialApplicationResult.reject;

public class ConfigChangeApplicator implements ChangeApplicator<NodeContext> {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigChangeApplicator.class);

  private final NomadChangeProcessor<NomadChange> commandProcessor;

  public ConfigChangeApplicator(NomadChangeProcessor<NomadChange> commandProcessor) {
    this.commandProcessor = commandProcessor;
  }

  @Override
  public PotentialApplicationResult<NodeContext> tryApply(NodeContext baseConfig, NomadChange change) {
    // supports multiple changes
    List<NomadChange> changes = getChanges(change);

    for (NomadChange c : changes) {
      try {
        baseConfig = commandProcessor.tryApply(baseConfig, c);
        // If one handler rejects the chanche by returning null, immediately reject!
        // If we were continuing the loop, then the "null" baseConfig would be pass to the next handlers
        // which would create a NPE
        if (baseConfig == null) {
          return reject("Change rejected: " + change);
        }
      } catch (NomadException e) {
        LOGGER.warn("Error:", e);
        return reject(e.getMessage());
      }
    }

    return allow(baseConfig);
  }

  @Override
  public void apply(NomadChange change) throws NomadException {
    // supports multiple changes
    List<NomadChange> changes = getChanges(change);

    for (NomadChange c : changes) {
      commandProcessor.apply(c);
    }
  }

  @SuppressWarnings("unchecked")
  private static List<NomadChange> getChanges(NomadChange change) {
    return change instanceof MultiNomadChange ? ((MultiNomadChange<NomadChange>) change).getChanges() : Collections.singletonList(change);
  }
}
