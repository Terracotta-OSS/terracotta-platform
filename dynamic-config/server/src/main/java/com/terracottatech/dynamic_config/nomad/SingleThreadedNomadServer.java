/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.nomad;

import com.terracottatech.nomad.messages.AcceptRejectResponse;
import com.terracottatech.nomad.messages.CommitMessage;
import com.terracottatech.nomad.messages.DiscoverResponse;
import com.terracottatech.nomad.messages.PrepareMessage;
import com.terracottatech.nomad.messages.RollbackMessage;
import com.terracottatech.nomad.messages.TakeoverMessage;
import com.terracottatech.nomad.server.ChangeApplicator;
import com.terracottatech.nomad.server.NomadException;
import com.terracottatech.nomad.server.UpgradableNomadServer;

import java.util.concurrent.locks.ReentrantLock;

public class SingleThreadedNomadServer implements UpgradableNomadServer {
  private final UpgradableNomadServer underlying;
  private final ReentrantLock lock = new ReentrantLock(true);

  public SingleThreadedNomadServer(UpgradableNomadServer underlying) {
    this.underlying = underlying;
  }

  @Override
  public DiscoverResponse discover() throws NomadException {
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
  public void setChangeApplicator(ChangeApplicator changeApplicator) {
    lock.lock();
    try {
      underlying.setChangeApplicator(changeApplicator);
    } finally {
      lock.unlock();
    }
  }
}
