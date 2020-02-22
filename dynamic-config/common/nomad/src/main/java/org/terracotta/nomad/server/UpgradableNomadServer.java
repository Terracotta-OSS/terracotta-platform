/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.nomad.server;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UpgradableNomadServer<T> extends NomadServer<T> {
  void setChangeApplicator(ChangeApplicator<T> changeApplicator);

  List<NomadChangeInfo> getAllNomadChanges() throws NomadException;

  Optional<NomadChangeInfo> getNomadChange(UUID uuid) throws NomadException;

  /**
   * Last change has not been committed or rolled back yet.
   * Nomad is in PREPARED mode and won't accept further changes.
   */
  boolean hasIncompleteChange();

  Optional<T> getCurrentCommittedChangeResult() throws NomadException;

  void reset() throws NomadException;
}
