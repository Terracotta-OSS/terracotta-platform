/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.nomad.server;

import org.terracotta.nomad.messages.AcceptRejectResponse;
import org.terracotta.nomad.messages.CommitMessage;
import org.terracotta.nomad.messages.DiscoverResponse;
import org.terracotta.nomad.messages.PrepareMessage;
import org.terracotta.nomad.messages.RollbackMessage;
import org.terracotta.nomad.messages.TakeoverMessage;

import java.util.List;
import java.util.Optional;

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
  public List<NomadChangeInfo<T>> getAllNomadChanges() throws NomadException {return delegate.getAllNomadChanges();}

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
  public Optional<T> getCurrentCommittedChangeResult() throws NomadException {return delegate.getCurrentCommittedChangeResult();}

  @Override
  public void reset() throws NomadException {
    delegate.reset();
  }
}
