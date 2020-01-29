/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.server;

import com.terracottatech.nomad.messages.AcceptRejectResponse;
import com.terracottatech.nomad.messages.CommitMessage;
import com.terracottatech.nomad.messages.DiscoverResponse;
import com.terracottatech.nomad.messages.PrepareMessage;
import com.terracottatech.nomad.messages.RollbackMessage;
import com.terracottatech.nomad.messages.TakeoverMessage;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

public class SingleThreadedNomadServer<T> implements UpgradableNomadServer<T> {
  private final UpgradableNomadServer<T> underlying;
  private final ReentrantLock lock = new ReentrantLock(true);

  public SingleThreadedNomadServer(UpgradableNomadServer<T> underlying) {
    this.underlying = underlying;
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
