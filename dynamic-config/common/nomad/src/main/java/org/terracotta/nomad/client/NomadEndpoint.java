/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.nomad.client;

import org.terracotta.nomad.messages.AcceptRejectResponse;
import org.terracotta.nomad.messages.CommitMessage;
import org.terracotta.nomad.messages.DiscoverResponse;
import org.terracotta.nomad.messages.PrepareMessage;
import org.terracotta.nomad.messages.RollbackMessage;
import org.terracotta.nomad.messages.TakeoverMessage;
import org.terracotta.nomad.server.NomadException;
import org.terracotta.nomad.server.NomadServer;

import java.net.InetSocketAddress;

import static java.util.Objects.requireNonNull;

public class NomadEndpoint<T> implements NomadServer<T> {
  private final InetSocketAddress address;
  private final NomadServer<T> server;

  public NomadEndpoint(InetSocketAddress address, NomadServer<T> server) {
    this.address = requireNonNull(address);
    this.server = requireNonNull(server);
  }

  public InetSocketAddress getAddress() {
    return address;
  }

  @Override
  public DiscoverResponse<T> discover() throws NomadException {
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

  @Override
  public void close() {server.close();}

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("NomadEndpoint{");
    sb.append("address=").append(address);
    sb.append('}');
    return sb.toString();
  }
}
