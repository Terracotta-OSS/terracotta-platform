/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.nomad.entity.server;

import org.terracotta.entity.ConcurrencyStrategy;
import org.terracotta.nomad.entity.common.NomadEntityMessage;

import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.singletonList;

/**
 * @author Mathieu Carbou
 */
class NoConcurrency implements ConcurrencyStrategy<NomadEntityMessage> {
  private static final int SLOT = 42;

  @Override
  public int concurrencyKey(NomadEntityMessage message) {
    return SLOT;
  }

  @Override
  public Set<Integer> getKeysForSynchronization() {
    return new HashSet<>(singletonList(SLOT));
  }
}
