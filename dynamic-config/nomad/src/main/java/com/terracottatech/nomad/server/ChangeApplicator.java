/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.server;

import com.terracottatech.nomad.client.change.NomadChange;

public interface ChangeApplicator<T> {
  PotentialApplicationResult<T> canApply(T existing, NomadChange change);

  void apply(NomadChange change) throws NomadException;
}