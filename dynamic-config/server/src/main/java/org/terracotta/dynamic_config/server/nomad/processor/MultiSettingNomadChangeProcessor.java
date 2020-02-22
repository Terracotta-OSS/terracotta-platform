/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.server.nomad.processor;

import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.nomad.DynamicConfigNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.FilteredNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.MultiSettingNomadChange;
import org.terracotta.nomad.server.NomadException;

import java.util.List;

/**
 * Filters Nomad changes of type {@link FilteredNomadChange} based on their applicability
 */
public class MultiSettingNomadChangeProcessor implements NomadChangeProcessor<DynamicConfigNomadChange> {
  private final NomadChangeProcessor<DynamicConfigNomadChange> next;

  public MultiSettingNomadChangeProcessor(NomadChangeProcessor<DynamicConfigNomadChange> next) {
    this.next = next;
  }

  @Override
  public void validate(NodeContext baseConfig, DynamicConfigNomadChange change) throws NomadException {
    List<? extends DynamicConfigNomadChange> changes = MultiSettingNomadChange.extractChanges(change);
    for (DynamicConfigNomadChange dynamicConfigNomadChange : changes) {
      next.validate(baseConfig, dynamicConfigNomadChange);
      if (baseConfig != null) {
        baseConfig = baseConfig.withCluster(dynamicConfigNomadChange.apply(baseConfig.getCluster()));
      }
    }
  }

  @Override
  public void apply(DynamicConfigNomadChange change) throws NomadException {
    List<? extends DynamicConfigNomadChange> changes = MultiSettingNomadChange.extractChanges(change);
    for (DynamicConfigNomadChange dynamicConfigNomadChange : changes) {
      next.apply(dynamicConfigNomadChange);
    }
  }
}
