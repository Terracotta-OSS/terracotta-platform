/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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

import org.terracotta.nomad.messages.AcceptRejectResponse;
import org.terracotta.nomad.messages.CommitMessage;
import org.terracotta.nomad.messages.DiscoverResponse;
import org.terracotta.nomad.messages.PrepareMessage;
import org.terracotta.nomad.messages.RollbackMessage;
import org.terracotta.nomad.messages.TakeoverMessage;

import java.util.Optional;
import java.util.UUID;

public interface NomadServer<T> extends AutoCloseable {
  /**
   * Last change has not been committed or rolled back yet.
   * Nomad is in PREPARED mode and won't accept further changes.
   */
  boolean hasIncompleteChange();

  Optional<ChangeState<T>> getConfig(UUID changeUUID) throws NomadException;

  Optional<T> getCurrentCommittedConfig() throws NomadException;

  DiscoverResponse<T> discover() throws NomadException;

  AcceptRejectResponse prepare(PrepareMessage message) throws NomadException;

  AcceptRejectResponse commit(CommitMessage message) throws NomadException;

  AcceptRejectResponse rollback(RollbackMessage message) throws NomadException;

  AcceptRejectResponse takeover(TakeoverMessage message) throws NomadException;

  void reset() throws NomadException;

  @Override
  void close();
}
