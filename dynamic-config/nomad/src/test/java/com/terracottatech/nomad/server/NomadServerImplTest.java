/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.server;

import org.junit.Test;

import com.terracottatech.nomad.server.state.NomadServerState;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.Assert.fail;

public class NomadServerImplTest {

  @Test
  public void testSetChangeApplicatorAlreadySet() throws Exception {
    NomadServerState serverState = mock(NomadServerState.class);
    when(serverState.isInitialized()).thenReturn(true);
    NomadServerImpl nomadServer = new NomadServerImpl(serverState);
    ChangeApplicator changeApplicator = mock(ChangeApplicator.class);
    nomadServer.setChangeApplicator(changeApplicator);
    try {
      nomadServer.setChangeApplicator(changeApplicator);
      fail("Should have got IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      //Indirectly tests the base case of setting also
      //If set correctly sets in first call, then only we get this exception
    }
  }

  @Test(expected = NullPointerException.class)
  public void testSetNullChangeApplicator() throws Exception {
    NomadServerState serverState = mock(NomadServerState.class);
    when(serverState.isInitialized()).thenReturn(true);
    NomadServerImpl nomadServer = new NomadServerImpl(serverState);
    nomadServer.setChangeApplicator(null);
  }
}
