/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
package org.terracotta.dynamic_config.server.api;

import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.service.NomadChangeInfo;
import org.terracotta.nomad.client.change.NomadChange;
import org.terracotta.nomad.messages.AcceptRejectResponse;
import org.terracotta.nomad.messages.CommitMessage;
import org.terracotta.nomad.messages.DiscoverResponse;
import org.terracotta.nomad.messages.PrepareMessage;
import org.terracotta.nomad.messages.RollbackMessage;
import org.terracotta.nomad.messages.TakeoverMessage;
import org.terracotta.nomad.server.ChangeApplicator;
import org.terracotta.nomad.server.ChangeState;
import org.terracotta.nomad.server.NomadException;
import org.terracotta.nomad.server.NomadServerMode;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;

/**
 * @author Mathieu Carbou
 */
class EmptyDynamicConfigNomadServer implements DynamicConfigNomadServer {
  @Override
  public boolean hasIncompleteChange() {
    return false;
  }

  @Override
  public Optional<ChangeState<NodeContext>> getConfig(UUID changeUUID) throws NomadException {
    return Optional.empty();
  }

  @Override
  public Optional<NodeContext> getCurrentCommittedConfig() throws NomadException {
    return Optional.empty();
  }

  @Override
  public DiscoverResponse<NodeContext> discover() throws NomadException {
    return new DiscoverResponse<>(NomadServerMode.UNINITIALIZED, 0, null, null, null, 0, 0, null, null);
  }

  @Override
  public AcceptRejectResponse prepare(PrepareMessage message) throws NomadException {
    throw new UnsupportedOperationException();
  }

  @Override
  public AcceptRejectResponse commit(CommitMessage message) throws NomadException {
    throw new UnsupportedOperationException();
  }

  @Override
  public AcceptRejectResponse rollback(RollbackMessage message) throws NomadException {
    throw new UnsupportedOperationException();
  }

  @Override
  public AcceptRejectResponse takeover(TakeoverMessage message) throws NomadException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void reset() throws NomadException {
  }

  @Override
  public void close() {

  }

  @Override
  public void setChangeApplicator(ChangeApplicator<NodeContext> changeApplicator) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ChangeApplicator<NodeContext> getChangeApplicator() {
    return null;
  }

  @Override
  public List<NomadChangeInfo> getChangeHistory() throws NomadException {
    return Collections.<NomadChangeInfo>emptyList();
  }

  @Override
  public void forceSync(Collection<NomadChangeInfo> changes, BiFunction<NodeContext, NomadChange, NodeContext> fn) throws NomadException {
  }
}
