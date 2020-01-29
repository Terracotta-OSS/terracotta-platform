/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.server.nomad.processor;

import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.nomad.client.change.NomadChange;
import org.terracotta.nomad.server.NomadException;

public interface NomadChangeProcessor<T extends NomadChange> {
  NodeContext tryApply(NodeContext baseConfig, T change) throws NomadException;

  void apply(T change) throws NomadException;
}
