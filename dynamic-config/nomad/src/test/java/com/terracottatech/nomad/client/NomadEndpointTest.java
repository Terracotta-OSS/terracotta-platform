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
import com.terracottatech.nomad.server.NomadServer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.InetSocketAddress;

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

  protected InetSocketAddress name = InetSocketAddress.createUnresolved("localhost", 9410);

  @Test
  public void getName() {
    NomadEndpoint<String> namedServer = new NomadEndpoint<>(name, server);
    assertEquals(name, namedServer.getAddress());
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
