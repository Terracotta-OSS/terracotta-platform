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
import org.terracotta.nomad.messages.AcceptRejectResponse;
import org.terracotta.nomad.messages.CommitMessage;
import org.terracotta.nomad.messages.DiscoverResponse;
import org.terracotta.nomad.messages.PrepareMessage;
import org.terracotta.nomad.messages.RollbackMessage;
import org.terracotta.nomad.messages.TakeoverMessage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;

/**
 * @author Mathieu Carbou
 */
public class UpgradableNomadServerAdapter<T> implements UpgradableNomadServer<T> {

  private final UpgradableNomadServer<T> delegate;

  public UpgradableNomadServerAdapter(UpgradableNomadServer<T> delegate) {
    this.delegate = delegate;
  }

  @Override
  public void setChangeApplicator(ChangeApplicator<T> changeApplicator) {delegate.setChangeApplicator(changeApplicator);}

  @Override
  public ChangeApplicator<T> getChangeApplicator() {
    return delegate.getChangeApplicator();
  }

  @Override
  public Optional<NomadChangeInfo> getNomadChangeInfo(UUID uuid) throws NomadException {
    return delegate.getNomadChangeInfo(uuid);
  }

  @Override
  public List<NomadChangeInfo> getAllNomadChanges() throws NomadException {return delegate.getAllNomadChanges();}

  @Override
  public Optional<NomadChangeInfo> getNomadChange(UUID uuid) throws NomadException {return delegate.getNomadChange(uuid);}

  @Override
  public DiscoverResponse<T> discover() throws NomadException {return delegate.discover();}

  @Override
  public AcceptRejectResponse prepare(PrepareMessage message) throws NomadException {return delegate.prepare(message);}

  @Override
  public AcceptRejectResponse commit(CommitMessage message) throws NomadException {return delegate.commit(message);}

  @Override
  public AcceptRejectResponse rollback(RollbackMessage message) throws NomadException {return delegate.rollback(message);}

  @Override
  public AcceptRejectResponse takeover(TakeoverMessage message) throws NomadException {return delegate.takeover(message);}

  @Override
  public boolean hasIncompleteChange() {
    return delegate.hasIncompleteChange();
  }

  @Override
  public Optional<T> getCurrentCommittedConfig() throws NomadException {return delegate.getCurrentCommittedConfig();}

  @Override
  public void reset() throws NomadException {
    delegate.reset();
  }

  @Override
  public void forceSync(Iterable<NomadChangeInfo> changes, BiFunction<T, NomadChange, T> fn) throws NomadException {
    delegate.forceSync(changes, fn);
  }

  @Override
  public void close() {delegate.close();}
}
