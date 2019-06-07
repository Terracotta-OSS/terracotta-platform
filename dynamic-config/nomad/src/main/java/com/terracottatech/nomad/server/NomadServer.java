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

public interface NomadServer {
  DiscoverResponse discover() throws NomadException;

  AcceptRejectResponse prepare(PrepareMessage message) throws NomadException;

  AcceptRejectResponse commit(CommitMessage message) throws NomadException;

  AcceptRejectResponse rollback(RollbackMessage message) throws NomadException;

  AcceptRejectResponse takeover(TakeoverMessage message) throws NomadException;
}
