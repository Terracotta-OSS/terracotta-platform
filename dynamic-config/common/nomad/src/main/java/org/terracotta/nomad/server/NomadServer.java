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

public interface NomadServer<T> extends AutoCloseable {
  DiscoverResponse<T> discover() throws NomadException;

  AcceptRejectResponse prepare(PrepareMessage message) throws NomadException;

  AcceptRejectResponse commit(CommitMessage message) throws NomadException;

  AcceptRejectResponse rollback(RollbackMessage message) throws NomadException;

  AcceptRejectResponse takeover(TakeoverMessage message) throws NomadException;

  @Override
  void close();
}
