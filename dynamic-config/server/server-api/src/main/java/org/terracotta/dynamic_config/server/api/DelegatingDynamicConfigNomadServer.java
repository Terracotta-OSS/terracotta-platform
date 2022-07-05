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

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;

/**
 * @author Mathieu Carbou
 */
public class DelegatingDynamicConfigNomadServer implements DynamicConfigNomadServer {

  private volatile DynamicConfigNomadServer delegate;

  public DelegatingDynamicConfigNomadServer(DynamicConfigNomadServer delegate) {
    this.delegate = delegate;
  }

  public void setDelegate(DynamicConfigNomadServer delegate) {
    this.delegate = delegate;
  }

  @Override
  public boolean hasIncompleteChange() {return delegate.hasIncompleteChange();}

  @Override
  public Optional<ChangeState<NodeContext>> getConfig(UUID changeUUID) throws NomadException {return delegate.getConfig(changeUUID);}

  @Override
  public Optional<NodeContext> getCurrentCommittedConfig() throws NomadException {return delegate.getCurrentCommittedConfig();}

  @Override
  public DiscoverResponse<NodeContext> discover() throws NomadException {return delegate.discover();}

  @Override
  public AcceptRejectResponse prepare(PrepareMessage message) throws NomadException {return delegate.prepare(message);}

  @Override
  public AcceptRejectResponse commit(CommitMessage message) throws NomadException {return delegate.commit(message);}

  @Override
  public AcceptRejectResponse rollback(RollbackMessage message) throws NomadException {return delegate.rollback(message);}

  @Override
  public AcceptRejectResponse takeover(TakeoverMessage message) throws NomadException {return delegate.takeover(message);}

  @Override
  public void reset() throws NomadException {delegate.reset();}

  @Override
  public void close() {delegate.close();}

  @Override
  public void setChangeApplicator(ChangeApplicator<NodeContext> changeApplicator) {delegate.setChangeApplicator(changeApplicator);}

  @Override
  public ChangeApplicator<NodeContext> getChangeApplicator() {return delegate.getChangeApplicator();}

  @Override
  public List<NomadChangeInfo> getChangeHistory() throws NomadException {return delegate.getChangeHistory();}

  @Override
  public void forceSync(Collection<NomadChangeInfo> changes, BiFunction<NodeContext, NomadChange, NodeContext> fn) throws NomadException {delegate.forceSync(changes, fn);}

  @Override
  public String toString() {
    return delegate.toString();
  }
}
