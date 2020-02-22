/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.api.model.nomad;

import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.nomad.client.change.NomadChange;

/**
 * @author Mathieu Carbou
 */
public interface DynamicConfigNomadChange extends NomadChange {

  /**
   * Returns the updated cluster to use for the next configuration
   *
   * @param original Original cluster on the node. Might be null;
   * @return updated cluster, must not be null
   */
  Cluster apply(Cluster original);

  boolean canApplyAtRuntime();
}
