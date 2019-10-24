/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.server;

import com.terracottatech.nomad.client.change.SimpleNomadChange;
import com.terracottatech.nomad.messages.AcceptRejectResponse;
import com.terracottatech.nomad.messages.CommitMessage;
import com.terracottatech.nomad.messages.DiscoverResponse;
import com.terracottatech.nomad.messages.PrepareMessage;
import com.terracottatech.nomad.server.state.MemoryNomadServerState;
import com.terracottatech.nomad.server.state.NomadServerState;
import org.junit.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

public class NomadServerImplTest {

  @SuppressWarnings("unchecked")
  @Test
  public void testSetChangeApplicatorAlreadySet() throws Exception {
    NomadServerState<String> serverState = mock(NomadServerState.class);
    when(serverState.isInitialized()).thenReturn(true);
    when(serverState.isLatestChangeCommittedOrRolledBack()).thenReturn(true);
    NomadServerImpl<String> nomadServer = new NomadServerImpl<>(serverState);
    ChangeApplicator<String> changeApplicator = mock(ChangeApplicator.class);
    nomadServer.setChangeApplicator(changeApplicator);
    try {
      nomadServer.setChangeApplicator(changeApplicator);
      fail("Should have got IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      //Indirectly tests the base case of setting also
      //If set correctly sets in first call, then only we get this exception
    }
  }

  @SuppressWarnings("unchecked")
  @Test(expected = NullPointerException.class)
  public void testSetNullChangeApplicator() throws Exception {
    NomadServerState<String> serverState = mock(NomadServerState.class);
    when(serverState.isInitialized()).thenReturn(true);
    when(serverState.isLatestChangeCommittedOrRolledBack()).thenReturn(true);
    NomadServerImpl<String> nomadServer = new NomadServerImpl<>(serverState);
    nomadServer.setChangeApplicator(null);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testListOfNomadChanges() throws Exception {
    ChangeApplicator<String> changeApplicator = mock(ChangeApplicator.class);
    NomadServerState<String> state = new MemoryNomadServerState<>();
    UpgradableNomadServer<String> server = new NomadServerImpl<>(state, changeApplicator);
    when(changeApplicator.tryApply(null, new SimpleNomadChange("change", "summary"))).thenReturn(PotentialApplicationResult.allow("change-applied"));

    DiscoverResponse<String> discoverResponse = server.discover();

    UUID firstChangeUuid = UUID.randomUUID();

    server.prepare(new PrepareMessage(
        discoverResponse.getMutativeMessageCount(),
        "testhost1",
        "testuser1",
        firstChangeUuid,
        discoverResponse.getHighestVersion() + 1,
        new SimpleNomadChange("change", "summary")
    ));

    AcceptRejectResponse response = server.commit(new CommitMessage(
        discoverResponse.getMutativeMessageCount() + 1,
        "testhost2",
        "testuser2",
        firstChangeUuid
    ));

    assertTrue(response.isAccepted());
    verify(changeApplicator).tryApply(null, new SimpleNomadChange("change", "summary"));
    verify(changeApplicator).apply(new SimpleNomadChange("change", "summary"));

    // Apply more changes
    when(changeApplicator.tryApply("change-applied", new SimpleNomadChange("change1", "summary1"))).thenReturn(PotentialApplicationResult.allow("change-applied1"));
    discoverResponse = server.discover();
    UUID nextChangeUuid = UUID.randomUUID();

    server.prepare(new PrepareMessage(
        discoverResponse.getMutativeMessageCount(),
        "testhost1",
        "testuser1",
        nextChangeUuid,
        discoverResponse.getHighestVersion() + 1,
        new SimpleNomadChange("change1", "summary1")
    ));

    response = server.commit(new CommitMessage(
        discoverResponse.getMutativeMessageCount() + 1,
        "testhost2",
        "testuser2",
        nextChangeUuid
    ));

    assertTrue(response.isAccepted());
    verify(changeApplicator).tryApply("change-applied", new SimpleNomadChange("change1", "summary1"));
    verify(changeApplicator).apply(new SimpleNomadChange("change1", "summary1"));

    //Verifying changes are as it happened
    List<NomadChangeHolder> getAllChanges = server.getAllNomadChanges();
    assertEquals(getAllChanges.size(), 2);
    assertEquals(getAllChanges.get(0).getChangeUuid(), firstChangeUuid);
    assertEquals(getAllChanges.get(0).getNomadChange(), new SimpleNomadChange("change", "summary"));
    assertEquals(getAllChanges.get(1).getChangeUuid(), nextChangeUuid);
    assertEquals(getAllChanges.get(1).getNomadChange(), new SimpleNomadChange("change1", "summary1"));
  }
}
