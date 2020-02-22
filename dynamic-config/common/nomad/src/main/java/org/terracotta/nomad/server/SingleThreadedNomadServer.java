/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
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
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;

public class SingleThreadedNomadServer<T> implements UpgradableNomadServer<T> {
  private final UpgradableNomadServer<T> underlying;
  private final ReentrantLock lock = new ReentrantLock(true);

  public SingleThreadedNomadServer(UpgradableNomadServer<T> underlying) {
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
  public void forceSync(Iterable<NomadChangeInfo> changes, BiFunction<T, NomadChange, T> fn) throws NomadException {
    lock.lock();
    try {
      underlying.forceSync(changes, fn);
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
  public DiscoverResponse<T> discover() throws NomadException {
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
  public void setChangeApplicator(ChangeApplicator<T> changeApplicator) {
    lock.lock();
    try {
      underlying.setChangeApplicator(changeApplicator);
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
  public Optional<T> getCurrentCommittedChangeResult() throws NomadException {
    lock.lock();
    try {
      return underlying.getCurrentCommittedChangeResult();
    } finally {
      lock.unlock();
    }
  }
}
