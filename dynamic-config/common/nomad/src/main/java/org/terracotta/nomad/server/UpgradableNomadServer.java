/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.nomad.server;

import org.terracotta.nomad.client.change.NomadChange;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;

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

  /**
   * Forces the sync of a stream of changes in a node's append log.
   * <p>
   * To construct the configurations to write, a function must be passed,
   * which will return a new configuration from 2 parameters: the change and
   * the previous configuration, which might be null at the beginning.
   */
  void forceSync(Iterable<NomadChangeInfo> changes, BiFunction<T, NomadChange, T> fn) throws NomadException;
}
