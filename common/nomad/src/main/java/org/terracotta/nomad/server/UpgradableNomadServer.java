/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.nomad.server;

import org.terracotta.nomad.client.change.NomadChange;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;

public interface UpgradableNomadServer<T> extends NomadServer<T> {
  void setChangeApplicator(ChangeApplicator<T> changeApplicator);

  ChangeApplicator<T> getChangeApplicator();

  Optional<NomadChangeInfo> getNomadChangeInfo(UUID uuid) throws NomadException;

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
