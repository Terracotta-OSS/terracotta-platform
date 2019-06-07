/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.client;

import com.terracottatech.nomad.messages.AcceptRejectResponse;
import com.terracottatech.nomad.messages.CommitMessage;
import com.terracottatech.nomad.messages.DiscoverResponse;
import com.terracottatech.nomad.messages.PrepareMessage;
import com.terracottatech.nomad.messages.RollbackMessage;
import com.terracottatech.nomad.messages.TakeoverMessage;
import com.terracottatech.nomad.server.NomadException;
import com.terracottatech.nomad.server.NomadServer;

public class NamedNomadServer implements NomadServer {
  private final String name;
  private final NomadServer server;

  public NamedNomadServer(String name, NomadServer server) {
    this.name = name;
    this.server = server;
  }

  public String getName() {
    return name;
  }

  @Override
  public DiscoverResponse discover() throws NomadException {
    return server.discover();
  }

  @Override
  public AcceptRejectResponse prepare(PrepareMessage message) throws NomadException {
    return server.prepare(message);
  }

  @Override
  public AcceptRejectResponse commit(CommitMessage message) throws NomadException {
    return server.commit(message);
  }

  @Override
  public AcceptRejectResponse rollback(RollbackMessage message) throws NomadException {
    return server.rollback(message);
  }

  @Override
  public AcceptRejectResponse takeover(TakeoverMessage message) throws NomadException {
    return server.takeover(message);
  }
}
