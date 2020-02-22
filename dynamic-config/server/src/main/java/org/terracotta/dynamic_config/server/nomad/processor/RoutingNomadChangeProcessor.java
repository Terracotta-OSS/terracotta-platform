/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.server.nomad.processor;

import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.nomad.DynamicConfigNomadChange;
import org.terracotta.nomad.client.change.NomadChange;
import org.terracotta.nomad.server.NomadException;

import java.util.HashMap;
import java.util.Map;

/**
 * Routes the incoming Nomad change to the right processor based on the Nomad change type
 */
public class RoutingNomadChangeProcessor implements NomadChangeProcessor<DynamicConfigNomadChange> {

  private final Map<Class<? extends NomadChange>, NomadChangeProcessor<DynamicConfigNomadChange>> processors = new HashMap<>();

  @SuppressWarnings("unchecked")
  public <T extends DynamicConfigNomadChange> RoutingNomadChangeProcessor register(Class<? extends T> changeType, NomadChangeProcessor<T> processor) {
    this.processors.put(changeType, (NomadChangeProcessor<DynamicConfigNomadChange>) processor);
    return this;
  }

  @Override
  public void validate(NodeContext baseConfig, DynamicConfigNomadChange change) throws NomadException {
    NomadChangeProcessor<DynamicConfigNomadChange> processor = getProcessor(change);
    processor.validate(baseConfig, change);
  }

  @Override
  public void apply(DynamicConfigNomadChange change) throws NomadException {
    NomadChangeProcessor<DynamicConfigNomadChange> processor = getProcessor(change);
    processor.apply(change);
  }

  private NomadChangeProcessor<DynamicConfigNomadChange> getProcessor(NomadChange change) throws NomadException {
    NomadChangeProcessor<DynamicConfigNomadChange> processor = processors.get(change.getClass());

    if (processor == null) {
      throw new NomadException("Unknown change: " + change.getClass().getName());
    }

    return processor;
  }
}
