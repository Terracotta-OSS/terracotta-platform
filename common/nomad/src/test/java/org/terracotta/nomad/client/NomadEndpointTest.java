/*
 * Copyright Terracotta, Inc.
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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.terracotta.inet.HostPort;
import org.terracotta.nomad.messages.AcceptRejectResponse;
import org.terracotta.nomad.messages.CommitMessage;
import org.terracotta.nomad.messages.DiscoverResponse;
import org.terracotta.nomad.messages.PrepareMessage;
import org.terracotta.nomad.messages.RollbackMessage;
import org.terracotta.nomad.messages.TakeoverMessage;
import org.terracotta.nomad.server.NomadServer;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NomadEndpointTest {
  @Mock
  private NomadServer<String> server;

  @Mock
  private DiscoverResponse<String> discovery;

  @Mock
  private AcceptRejectResponse acceptRejectResponse;

  @Mock
  private PrepareMessage prepareMessage;

  @Mock
  private TakeoverMessage takeoverMessage;

  @Mock
  private CommitMessage commitMessage;

  @Mock
  private RollbackMessage rollbackMessage;

  protected HostPort name = HostPort.create("localhost", 9410);

  @Test
  public void getName() {
    NomadEndpoint<String> namedServer = new NomadEndpoint<>(name, server);
    assertEquals(name, namedServer.getHostPort());
  }

  @Test
  public void delegateDiscover() throws Exception {
    when(server.discover()).thenReturn(discovery);

    NomadEndpoint<String> namedServer = new NomadEndpoint<>(name, server);
    assertEquals(discovery, namedServer.discover());
  }

  @Test
  public void delegatePrepare() throws Exception {
    when(server.prepare(prepareMessage)).thenReturn(acceptRejectResponse);

    NomadEndpoint<String> namedServer = new NomadEndpoint<>(name, server);
    assertEquals(acceptRejectResponse, namedServer.prepare(prepareMessage));
    verify(server).prepare(prepareMessage);
  }

  @Test
  public void delegateTakeover() throws Exception {
    when(server.takeover(takeoverMessage)).thenReturn(acceptRejectResponse);

    NomadEndpoint<String> namedServer = new NomadEndpoint<>(name, server);
    assertEquals(acceptRejectResponse, namedServer.takeover(takeoverMessage));
    verify(server).takeover(takeoverMessage);
  }

  @Test
  public void delegateCommit() throws Exception {
    when(server.commit(commitMessage)).thenReturn(acceptRejectResponse);

    NomadEndpoint<String> namedServer = new NomadEndpoint<>(name, server);
    assertEquals(acceptRejectResponse, namedServer.commit(commitMessage));
    verify(server).commit(commitMessage);
  }

  @Test
  public void delegateRollback() throws Exception {
    when(server.rollback(rollbackMessage)).thenReturn(acceptRejectResponse);

    NomadEndpoint<String> namedServer = new NomadEndpoint<>(name, server);
    assertEquals(acceptRejectResponse, namedServer.rollback(rollbackMessage));
    verify(server).rollback(rollbackMessage);
  }
}
