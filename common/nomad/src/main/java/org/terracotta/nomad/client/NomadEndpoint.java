/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
package org.terracotta.nomad.client;

import org.terracotta.inet.HostPort;
import org.terracotta.nomad.messages.AcceptRejectResponse;
import org.terracotta.nomad.messages.CommitMessage;
import org.terracotta.nomad.messages.DiscoverResponse;
import org.terracotta.nomad.messages.PrepareMessage;
import org.terracotta.nomad.messages.RollbackMessage;
import org.terracotta.nomad.messages.TakeoverMessage;
import org.terracotta.nomad.server.ChangeState;
import org.terracotta.nomad.server.NomadException;
import org.terracotta.nomad.server.NomadServer;

import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public class NomadEndpoint<T> implements NomadServer<T> {
  private final HostPort hostPort;
  private final NomadServer<T> server;

  public NomadEndpoint(HostPort hostPort, NomadServer<T> server) {
    this.hostPort = requireNonNull(hostPort);
    this.server = requireNonNull(server);
  }

  public HostPort getHostPort() {
    return hostPort;
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
  public boolean hasIncompleteChange() {return server.hasIncompleteChange();}

  @Override
  public Optional<ChangeState<T>> getConfig(UUID changeUUID) throws NomadException {
    return server.getConfig(changeUUID);
  }

  @Override
  public Optional<T> getCurrentCommittedConfig() throws NomadException {return server.getCurrentCommittedConfig();}

  @Override
  public void reset() throws NomadException {server.reset();}

  @Override
  public String toString() {
    return hostPort.toString();
  }
}
