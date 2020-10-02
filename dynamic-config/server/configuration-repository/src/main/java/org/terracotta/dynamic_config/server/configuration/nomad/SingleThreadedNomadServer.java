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
package org.terracotta.dynamic_config.server.configuration.nomad;

import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.service.NomadChangeInfo;
import org.terracotta.dynamic_config.server.api.DynamicConfigNomadServer;
import org.terracotta.nomad.client.change.NomadChange;
import org.terracotta.nomad.messages.AcceptRejectResponse;
import org.terracotta.nomad.messages.CommitMessage;
import org.terracotta.nomad.messages.DiscoverResponse;
import org.terracotta.nomad.messages.PrepareMessage;
import org.terracotta.nomad.messages.RollbackMessage;
import org.terracotta.nomad.messages.TakeoverMessage;
import org.terracotta.nomad.server.ChangeApplicator;
import org.terracotta.nomad.server.NomadException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;

public class SingleThreadedNomadServer implements DynamicConfigNomadServer {
  private final DynamicConfigNomadServer underlying;
  protected final ReentrantLock lock = new ReentrantLock(true);

  public SingleThreadedNomadServer(DynamicConfigNomadServer underlying) {
    this.underlying = underlying;
  }

  @Override
  public void reset() throws NomadException {
    lock.lock();
    try {
      underlying.reset();
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void close() {
    lock.lock();
    try {
      underlying.close();
    } finally {
      lock.unlock();
    }
  }

  @Override
  public DiscoverResponse<NodeContext> discover() throws NomadException {
    lock.lock();
    try {
      return underlying.discover();
    } finally {
      lock.unlock();
    }
  }

  @Override
  public AcceptRejectResponse prepare(PrepareMessage message) throws NomadException {
    lock.lock();
    try {
      return underlying.prepare(message);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public AcceptRejectResponse commit(CommitMessage message) throws NomadException {
    lock.lock();
    try {
      return underlying.commit(message);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public AcceptRejectResponse rollback(RollbackMessage message) throws NomadException {
    lock.lock();
    try {
      return underlying.rollback(message);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public AcceptRejectResponse takeover(TakeoverMessage message) throws NomadException {
    lock.lock();
    try {
      return underlying.takeover(message);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public boolean hasIncompleteChange() {
    lock.lock();
    try {
      return underlying.hasIncompleteChange();
    } finally {
      lock.unlock();
    }
  }

  @Override
  public Optional<NodeContext> getCurrentCommittedConfig() throws NomadException {
    lock.lock();
    try {
      return underlying.getCurrentCommittedConfig();
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void forceSync(Iterable<NomadChangeInfo> changes, BiFunction<NodeContext, NomadChange, NodeContext> fn) throws NomadException {
    lock.lock();
    try {
      underlying.forceSync(changes, fn);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void setChangeApplicator(ChangeApplicator<NodeContext> changeApplicator) {
    lock.lock();
    try {
      underlying.setChangeApplicator(changeApplicator);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public ChangeApplicator<NodeContext> getChangeApplicator() {
    lock.lock();
    try {
      return underlying.getChangeApplicator();
    } finally {
      lock.unlock();
    }
  }

  @Override
  public List<NomadChangeInfo> getAllNomadChanges() throws NomadException {
    lock.lock();
    try {
      return underlying.getAllNomadChanges();
    } finally {
      lock.unlock();
    }
  }

  @Override
  public Optional<NomadChangeInfo> getNomadChange(UUID uuid) throws NomadException {
    lock.lock();
    try {
      return underlying.getNomadChange(uuid);
    } finally {
      lock.unlock();
    }
  }
}
