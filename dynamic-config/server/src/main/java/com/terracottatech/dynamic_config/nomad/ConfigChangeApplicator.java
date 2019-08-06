/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.nomad;

import com.terracottatech.dynamic_config.nomad.processor.NomadChangeProcessor;
import com.terracottatech.nomad.client.change.MultipleNomadChanges;
import com.terracottatech.nomad.client.change.NomadChange;
import com.terracottatech.nomad.server.ChangeApplicator;
import com.terracottatech.nomad.server.NomadException;
import com.terracottatech.nomad.server.PotentialApplicationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

import static com.terracottatech.nomad.server.PotentialApplicationResult.allow;
import static com.terracottatech.nomad.server.PotentialApplicationResult.reject;

public class ConfigChangeApplicator implements ChangeApplicator<String> {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigChangeApplicator.class);

  private final NomadChangeProcessor<NomadChange> commandProcessor;

  public ConfigChangeApplicator(NomadChangeProcessor<NomadChange> commandProcessor) {
    this.commandProcessor = commandProcessor;
  }

  @Override
  public PotentialApplicationResult<String> tryApply(String baseConfig, NomadChange change) {
    // supports multiple changes
    List<NomadChange> changes = change instanceof MultipleNomadChanges ? ((MultipleNomadChanges) change).getChanges() : Collections.singletonList(change);

    for (NomadChange c : changes) {
      try {
        baseConfig = commandProcessor.tryApply(baseConfig, c);
      } catch (NomadException e) {
        LOGGER.warn(e.getMessage(), e);
        return reject(e.getMessage());
      }
    }

    return allow(baseConfig);
  }

  @Override
  public void apply(NomadChange change) throws NomadException {
    // supports multiple changes
    List<NomadChange> changes = change instanceof MultipleNomadChanges ? ((MultipleNomadChanges) change).getChanges() : Collections.singletonList(change);

    for (NomadChange c : changes) {
      commandProcessor.apply(c);
    }
  }

}
