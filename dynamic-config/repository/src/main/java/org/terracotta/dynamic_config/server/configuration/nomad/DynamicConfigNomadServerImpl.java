/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2026
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
import org.terracotta.dynamic_config.api.model.nomad.ClusterActivationNomadChange;
import org.terracotta.dynamic_config.api.service.NomadChangeInfo;
import org.terracotta.dynamic_config.api.server.DynamicConfigNomadServer;
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
import org.terracotta.nomad.server.NomadServerImpl;
import org.terracotta.nomad.server.state.NomadServerState;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiFunction;

/**
 * @author Mathieu Carbou
 */
public class DynamicConfigNomadServerImpl extends NomadServerImpl<NodeContext> implements DynamicConfigNomadServer {
  private final NomadServerState<NodeContext> state;
  private final ReadWriteLock lock = new ReentrantReadWriteLock(true);

  public DynamicConfigNomadServerImpl(NomadServerState<NodeContext> state) throws NomadException {
    super(state);
    this.state = state;
  }

  // Dynamic Config does not allow more than 1 thread to access the Nomad system at a time (in W mode)

  @Override
  public void reset() throws NomadException {
    lock.writeLock().lock();
    try {
      super.reset();
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public void close() {
    lock.writeLock().lock();
    try {
      super.close();
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public DiscoverResponse<NodeContext> discover() throws NomadException {
    lock.readLock().lock();
    try {
      return super.discover();
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public AcceptRejectResponse prepare(PrepareMessage message) throws NomadException {
    lock.writeLock().lock();
    try {
      return super.prepare(message);
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public AcceptRejectResponse commit(CommitMessage message) throws NomadException {
    lock.writeLock().lock();
    try {
      return super.commit(message);
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public AcceptRejectResponse rollback(RollbackMessage message) throws NomadException {
    lock.writeLock().lock();
    try {
      return super.rollback(message);
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public AcceptRejectResponse takeover(TakeoverMessage message) throws NomadException {
    lock.writeLock().lock();
    try {
      return super.takeover(message);
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public boolean hasIncompleteChange() {
    lock.readLock().lock();
    try {
      return super.hasIncompleteChange();
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public Optional<ChangeState<NodeContext>> getConfig(UUID changeUUID) throws NomadException {
    lock.readLock().lock();
    try {
      return super.getConfig(changeUUID);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public Optional<NodeContext> getCurrentCommittedConfig() throws NomadException {
    lock.readLock().lock();
    try {
      return super.getCurrentCommittedConfig();
    } finally {
      lock.readLock().unlock();
    }
  }

  // This implementation

  @Override
  public void reserve() {
    lock.writeLock().lock();
  }

  @Override
  public void release() {
    lock.writeLock().unlock();
  }

  @Override
  public void setChangeApplicator(ChangeApplicator<NodeContext> changeApplicator) {
    lock.writeLock().lock();
    try {
      if (getChangeApplicator() != null && changeApplicator != null) {
        throw new IllegalArgumentException("Variable changeApplicator is already set");
      }
      super.setChangeApplicator(changeApplicator);
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public ChangeApplicator<NodeContext> getChangeApplicator() {
    lock.readLock().lock();
    try {
      return super.getChangeApplicator();
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public void applyChanges(Collection<NomadChangeInfo> changes, BiFunction<NodeContext, NomadChange, NodeContext> fn) throws NomadException {
    lock.writeLock().lock();
    try {
      final ChangeApplicator<NodeContext> backup = super.getChangeApplicator();
      try {
        super.setChangeApplicator(ChangeApplicator.allow(fn));

        for (NomadChangeInfo change : changes) {
          switch (change.getChangeRequestState()) {
            case PREPARED: {
              long mutativeMessageCount = state.getMutativeMessageCount();
              AcceptRejectResponse response = super.prepare(change.toPrepareMessage(mutativeMessageCount));
              if (!response.isAccepted()) {
                throw new NomadException("Prepare failure. " +
                  "Reason: " + response + ". " +
                  "Change:" + change.getNomadChange().getSummary());
              }
              break;
            }
            case COMMITTED: {
              long mutativeMessageCount = state.getMutativeMessageCount();
              AcceptRejectResponse response = super.prepare(change.toPrepareMessage(mutativeMessageCount));
              if (!response.isAccepted()) {
                throw new NomadException("Prepare failure. " +
                  "Reason: " + response + ". " +
                  "Change:" + change.getNomadChange().getSummary());
              }
              response = super.commit(change.toCommitMessage(mutativeMessageCount + 1));
              if (!response.isAccepted()) {
                throw new NomadException("Unexpected commit failure. " +
                  "Reason: " + response + ". " +
                  "Change:" + change.getNomadChange().getSummary());
              }
              break;
            }
            case ROLLED_BACK: {
              long mutativeMessageCount = state.getMutativeMessageCount();
              AcceptRejectResponse response = super.prepare(change.toPrepareMessage(mutativeMessageCount));
              if (!response.isAccepted()) {
                throw new NomadException("Prepare failure. " +
                  "Reason: " + response + ". " +
                  "Change:" + change.getNomadChange().getSummary());
              }
              response = super.rollback(change.toRollbackMessage(mutativeMessageCount + 1));
              if (!response.isAccepted()) {
                throw new NomadException("Unexpected rollback failure. " +
                  "Reason: " + response + ". " +
                  "Change:" + change.getNomadChange().getSummary());
              }
              break;
            }
            default:
              throw new AssertionError(change.getChangeRequestState());
          }
        }
      } finally {
        super.setChangeApplicator(backup);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public List<NomadChangeInfo> getChangeHistory() throws NomadException {
    lock.readLock().lock();
    try {
      LinkedList<NomadChangeInfo> output = new LinkedList<>();
      UUID changeUuid = state.getLatestChangeUuid();
      while (changeUuid != null) {
        ChangeState<NodeContext> changeState = state.getChangeState(changeUuid);
        output.addFirst(
          new NomadChangeInfo(
            changeUuid,
            changeState.getChange(),
            changeState.getState(),
            changeState.getVersion(),
            changeState.getCreationHost(),
            changeState.getCreationUser(),
            changeState.getCreationTimestamp(),
            changeState.getChangeResultHash()
          )
        );
        // We have arrived at a starting point of a sync when we reach the first ClusterActivationNomadChange
        // or FormatUpgradeNomadChange or when there is no more changes
        changeUuid = changeState.getChange() instanceof ClusterActivationNomadChange ? null : changeState.getPrevChangeId();
      }
      return output;
    } finally {
      lock.readLock().unlock();
    }
  }
}
