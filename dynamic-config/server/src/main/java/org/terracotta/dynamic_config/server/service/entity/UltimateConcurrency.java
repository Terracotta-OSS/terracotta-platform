/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.server.service.entity;

import org.terracotta.dynamic_config.entity.common.DynamicTopologyEntityMessage;
import org.terracotta.entity.ConcurrencyStrategy;

import java.util.Collections;
import java.util.Set;

/**
 * @author Mathieu Carbou
 */
class UltimateConcurrency implements ConcurrencyStrategy<DynamicTopologyEntityMessage> {
  public int concurrencyKey(DynamicTopologyEntityMessage message) {
    return ConcurrencyStrategy.UNIVERSAL_KEY;
  }

  @Override
  public Set<Integer> getKeysForSynchronization() {
    return Collections.emptySet();
  }
}
