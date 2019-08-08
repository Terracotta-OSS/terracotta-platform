/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.nomad.processor;

import com.terracottatech.dynamic_config.model.NodeContext;
import com.terracottatech.nomad.client.change.NomadChange;
import com.terracottatech.nomad.server.NomadException;

import java.util.HashMap;
import java.util.Map;

/**
 * Routes the incoming Nomad change to the right processor based on the Nomad change type
 */
public class RoutingNomadChangeProcessor implements NomadChangeProcessor<NomadChange> {

  private final Map<Class<? extends NomadChange>, NomadChangeProcessor<NomadChange>> processors = new HashMap<>();

  @SuppressWarnings("unchecked")
  public <T extends NomadChange> RoutingNomadChangeProcessor register(Class<? extends T> changeType, NomadChangeProcessor<T> processor) {
    this.processors.put(changeType, (NomadChangeProcessor<NomadChange>) processor);
    return this;
  }

  @Override
  public NodeContext tryApply(NodeContext baseConfig, NomadChange change) throws NomadException {
    NomadChangeProcessor<NomadChange> processor = getProcessor(change);
    return processor.tryApply(baseConfig, change);
  }

  @Override
  public void apply(NomadChange change) throws NomadException {
    NomadChangeProcessor<NomadChange> processor = getProcessor(change);
    processor.apply(change);
  }

  private NomadChangeProcessor<NomadChange> getProcessor(NomadChange change) throws NomadException {
    NomadChangeProcessor<NomadChange> processor = processors.get(change.getClass());

    if (processor == null) {
      throw new NomadException("Unknown change: " + change.getClass().getName());
    }

    return processor;
  }
}
