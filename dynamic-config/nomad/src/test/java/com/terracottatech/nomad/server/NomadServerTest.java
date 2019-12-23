/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.server;

import com.terracottatech.nomad.client.change.SimpleNomadChange;
import com.terracottatech.nomad.messages.AcceptRejectResponse;
import com.terracottatech.nomad.messages.ChangeDetails;
import com.terracottatech.nomad.messages.CommitMessage;
import com.terracottatech.nomad.messages.DiscoverResponse;
import com.terracottatech.nomad.messages.PrepareMessage;
import com.terracottatech.nomad.messages.RejectionReason;
import com.terracottatech.nomad.messages.RollbackMessage;
import com.terracottatech.nomad.messages.TakeoverMessage;
import com.terracottatech.nomad.server.state.MemoryNomadServerState;
import com.terracottatech.nomad.server.state.NomadServerState;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.UUID;

import static com.terracottatech.nomad.messages.RejectionReason.DEAD;
import static com.terracottatech.nomad.messages.RejectionReason.UNACCEPTABLE;
import static com.terracottatech.nomad.server.NomadServerMode.ACCEPTING;
import static com.terracottatech.nomad.server.NomadServerMode.PREPARED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NomadServerTest {
  @Mock
  private ChangeApplicator<String> changeApplicator;

  private NomadServerState<String> state;
  private NomadServer<String> server;

  @Before
  public void before() throws Exception {
    state = new MemoryNomadServerState<>();
    assertFalse(state.isInitialized());
    server = new NomadServerImpl<>(state, changeApplicator);
  }

  @After
  public void after() {
    verifyNoMoreInteractions(changeApplicator);
  }

  @Test
  public void initializesState() throws Exception {
    assertState(ACCEPTING, 1L, null, null, null, 0L, 0L, null, null, null, null, null, null, null, null);
  }

  @Test
  public void doubleInitialization() throws Exception {
    new NomadServerImpl<>(state, changeApplicator);
    assertState(ACCEPTING, 1L, null, null, null, 0L, 0L, null, null, null, null, null, null, null, null);
  }

  @Test
  public void prepare() throws Exception {
    when(changeApplicator.tryApply(null, new SimpleNomadChange("change", "summary"))).thenReturn(PotentialApplicationResult.allow("change-applied"));

    DiscoverResponse<String> discoverResponse = server.discover();

    UUID uuid = UUID.randomUUID();

    AcceptRejectResponse response = server.prepare(new PrepareMessage(
        discoverResponse.getMutativeMessageCount(),
        "testhost",
        "testuser",
        uuid,
        discoverResponse.getHighestVersion() + 1,
        new SimpleNomadChange("change", "summary")
    ));

    assertTrue(response.isAccepted());
    assertState(PREPARED, 2L, "testhost", "testuser", uuid, 0L, 1L, ChangeRequestState.PREPARED, 1L, null, "change", "change-applied", "testhost", "testuser", "summary");
    verify(changeApplicator).tryApply(null, new SimpleNomadChange("change", "summary"));
  }

  @Test
  public void commit() throws Exception {
    when(changeApplicator.tryApply(null, new SimpleNomadChange("change", "summary"))).thenReturn(PotentialApplicationResult.allow("change-applied"));

    DiscoverResponse<String> discoverResponse = server.discover();

    UUID uuid = UUID.randomUUID();

    server.prepare(new PrepareMessage(
        discoverResponse.getMutativeMessageCount(),
        "testhost1",
        "testuser1",
        uuid,
        discoverResponse.getHighestVersion() + 1,
        new SimpleNomadChange("change", "summary")
    ));

    AcceptRejectResponse response = server.commit(new CommitMessage(
        discoverResponse.getMutativeMessageCount() + 1,
        "testhost2",
        "testuser2",
        uuid
    ));

    assertTrue(response.isAccepted());
    assertState(ACCEPTING, 3L, "testhost2", "testuser2", uuid, 1L, 1L, ChangeRequestState.COMMITTED, 1L, null, "change", "change-applied", "testhost1", "testuser1", "summary");
    verify(changeApplicator).tryApply(null, new SimpleNomadChange("change", "summary"));
    verify(changeApplicator).apply(new SimpleNomadChange("change", "summary"));
  }

  @Test
  public void rollback() throws Exception {
    when(changeApplicator.tryApply(null, new SimpleNomadChange("change", "summary"))).thenReturn(PotentialApplicationResult.allow("change-applied"));

    DiscoverResponse<String> discoverResponse = server.discover();

    UUID uuid = UUID.randomUUID();

    server.prepare(new PrepareMessage(
        discoverResponse.getMutativeMessageCount(),
        "testhost1",
        "testuser1",
        uuid,
        discoverResponse.getHighestVersion() + 1,
        new SimpleNomadChange("change", "summary")
    ));

    AcceptRejectResponse response = server.rollback(new RollbackMessage(
        discoverResponse.getMutativeMessageCount() + 1,
        "testhost2",
        "testuser2",
        uuid
    ));

    assertTrue(response.isAccepted());
    assertState(ACCEPTING, 3L, "testhost2", "testuser2", uuid, 0L, 1L, ChangeRequestState.ROLLED_BACK, 1L, null, "change", "change-applied", "testhost1", "testuser1", "summary");
    verify(changeApplicator).tryApply(null, new SimpleNomadChange("change", "summary"));
  }

  @Test
  public void takeover() throws Exception {
    when(changeApplicator.tryApply(null, new SimpleNomadChange("change", "summary"))).thenReturn(PotentialApplicationResult.allow("change-applied"));

    DiscoverResponse<String> discoverResponse = server.discover();

    UUID uuid = UUID.randomUUID();

    server.prepare(new PrepareMessage(
        discoverResponse.getMutativeMessageCount(),
        "testhost1",
        "testuser1",
        uuid,
        discoverResponse.getHighestVersion() + 1,
        new SimpleNomadChange("change", "summary")
    ));

    AcceptRejectResponse response = server.takeover(new TakeoverMessage(
        discoverResponse.getMutativeMessageCount() + 1,
        "testhost2",
        "testuser2"
    ));

    assertTrue(response.isAccepted());
    assertState(PREPARED, 3L, "testhost2", "testuser2", uuid, 0L, 1L, ChangeRequestState.PREPARED, 1L, null, "change", "change-applied", "testhost1", "testuser1", "summary");
    verify(changeApplicator).tryApply(null, new SimpleNomadChange("change", "summary"));
  }

  @Test
  public void plainTakeover() throws Exception {
    DiscoverResponse<String> discoverResponse = server.discover();

    AcceptRejectResponse response = server.takeover(new TakeoverMessage(
        discoverResponse.getMutativeMessageCount(),
        "testhost1",
        "testuser1"
    ));

    assertTrue(response.isAccepted());
    assertState(ACCEPTING, 2L, "testhost1", "testuser1", null, 0L, 0L, null, null, null, null, null, null, null, null);
  }

  @Test
  public void deadPrepare() throws Exception {
    DiscoverResponse<String> discoverResponse = server.discover();

    server.takeover(new TakeoverMessage(
        discoverResponse.getMutativeMessageCount(),
        "testhost1",
        "testuser1"
    ));

    UUID uuid = UUID.randomUUID();

    AcceptRejectResponse response = server.prepare(new PrepareMessage(
        discoverResponse.getMutativeMessageCount(),
        "testhost2",
        "testuser2",
        uuid,
        discoverResponse.getHighestVersion() + 1,
        new SimpleNomadChange("change", "summary")
    ));

    assertRejection(response, DEAD, null, "testhost1", "testuser1");
    assertState(ACCEPTING, 2L, "testhost1", "testuser1", null, 0L, 0L, null, null, null, null, null, null, null, null);
  }

  private void assertRejection(AcceptRejectResponse response, RejectionReason rejectionReason, String rejectionMessage, String host, String user) {
    assertFalse(response.isAccepted());
    assertEquals(rejectionReason, response.getRejectionReason());
    assertEquals(rejectionMessage, response.getRejectionMessage());
    assertEquals(host, response.getLastMutationHost());
    assertEquals(user, response.getLastMutationUser());
  }

  @Test
  public void deadCommit() throws Exception {
    when(changeApplicator.tryApply(null, new SimpleNomadChange("change", "summary"))).thenReturn(PotentialApplicationResult.allow("change-applied"));

    DiscoverResponse<String> discoverResponse = server.discover();

    UUID uuid = UUID.randomUUID();

    server.prepare(new PrepareMessage(
        discoverResponse.getMutativeMessageCount(),
        "testhost1",
        "testuser1",
        uuid,
        discoverResponse.getHighestVersion() + 1,
        new SimpleNomadChange("change", "summary")
    ));

    server.takeover(new TakeoverMessage(
        discoverResponse.getMutativeMessageCount() + 1,
        "testhost1",
        "testuser1"
    ));

    AcceptRejectResponse response = server.commit(new CommitMessage(
        discoverResponse.getMutativeMessageCount() + 1,
        "testhost2",
        "testuser2",
        uuid
    ));

    assertRejection(response, DEAD, "expectedMutativeMessageCount != actualMutativeMessageCount", "testhost1", "testuser1");
    assertState(PREPARED, 3L, "testhost1", "testuser1", uuid, 0L, 1L, ChangeRequestState.PREPARED, 1L, null, "change", "change-applied", "testhost1", "testuser1", "summary");
    verify(changeApplicator).tryApply(null, new SimpleNomadChange("change", "summary"));
  }

  @Test
  public void deadRollback() throws Exception {
    when(changeApplicator.tryApply(null, new SimpleNomadChange("change", "summary"))).thenReturn(PotentialApplicationResult.allow("change-applied"));

    DiscoverResponse<String> discoverResponse = server.discover();

    UUID uuid = UUID.randomUUID();

    server.prepare(new PrepareMessage(
        discoverResponse.getMutativeMessageCount(),
        "testhost1",
        "testuser1",
        uuid,
        discoverResponse.getHighestVersion() + 1,
        new SimpleNomadChange("change", "summary")
    ));

    server.takeover(new TakeoverMessage(
        discoverResponse.getMutativeMessageCount() + 1,
        "testhost1",
        "testuser1"
    ));

    AcceptRejectResponse response = server.rollback(new RollbackMessage(
        discoverResponse.getMutativeMessageCount() + 1,
        "testhost2",
        "testuser2",
        uuid
    ));

    assertRejection(response, DEAD, null, "testhost1", "testuser1");
    assertState(PREPARED, 3L, "testhost1", "testuser1", uuid, 0L, 1L, ChangeRequestState.PREPARED, 1L, null, "change", "change-applied", "testhost1", "testuser1", "summary");
    verify(changeApplicator).tryApply(null, new SimpleNomadChange("change", "summary"));
  }

  @Test
  public void deadTakeover() throws Exception {
    when(changeApplicator.tryApply(null, new SimpleNomadChange("change", "summary"))).thenReturn(PotentialApplicationResult.allow("change-applied"));

    DiscoverResponse<String> discoverResponse = server.discover();

    UUID uuid = UUID.randomUUID();

    server.prepare(new PrepareMessage(
        discoverResponse.getMutativeMessageCount(),
        "testhost1",
        "testuser1",
        uuid,
        discoverResponse.getHighestVersion() + 1,
        new SimpleNomadChange("change", "summary")
    ));

    server.takeover(new TakeoverMessage(
        discoverResponse.getMutativeMessageCount() + 1,
        "testhost1",
        "testuser1"
    ));

    AcceptRejectResponse response = server.takeover(new TakeoverMessage(
        discoverResponse.getMutativeMessageCount() + 1,
        "testhost2",
        "testuser2"
    ));

    assertRejection(response, DEAD, null, "testhost1", "testuser1");
    assertState(PREPARED, 3L, "testhost1", "testuser1", uuid, 0L, 1L, ChangeRequestState.PREPARED, 1L, null, "change", "change-applied", "testhost1", "testuser1", "summary");
    verify(changeApplicator).tryApply(null, new SimpleNomadChange("change", "summary"));
  }

  @Test
  public void prepareUnacceptableChange() throws Exception {
    when(changeApplicator.tryApply(null, new SimpleNomadChange("change", "summary"))).thenReturn(PotentialApplicationResult.reject("fail"));

    DiscoverResponse<String> discoverResponse = server.discover();

    UUID uuid = UUID.randomUUID();

    AcceptRejectResponse response = server.prepare(new PrepareMessage(
        discoverResponse.getMutativeMessageCount(),
        "testhost",
        "testuser",
        uuid,
        discoverResponse.getHighestVersion() + 1,
        new SimpleNomadChange("change", "summary")
    ));

    assertRejection(response, UNACCEPTABLE, "fail", null, null);
    assertState(ACCEPTING, 1L, null, null, null, 0L, 0L, null, null, null, null, null, null, null, null);
    verify(changeApplicator).tryApply(null, new SimpleNomadChange("change", "summary"));
  }

  @Test
  public void testCommitWithPrevId() throws Exception {
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
    assertState(ACCEPTING, 3L, "testhost2", "testuser2", firstChangeUuid, 1L, 1L, ChangeRequestState.COMMITTED, 1L, null, "change", "change-applied", "testhost1", "testuser1", "summary");
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
    assertState(ACCEPTING, 5L, "testhost2", "testuser2", nextChangeUuid, 2L, 2L, ChangeRequestState.COMMITTED, 2L, firstChangeUuid.toString(), "change1", "change-applied1", "testhost1", "testuser1", "summary1");
    verify(changeApplicator).tryApply("change-applied", new SimpleNomadChange("change1", "summary1"));
    verify(changeApplicator).apply(new SimpleNomadChange("change1", "summary1"));
  }

  private void assertState(
      NomadServerMode mode,
      long mutativeMessageCount,
      String lastMutationHost,
      String lastMutationUser,
      UUID latestChangeUuid,
      long currentVersion,
      long highestVersion,
      ChangeRequestState changeState,
      Long changeVersion,
      String prevChangeUuid,
      String changeOperation,
      String changeResult,
      String changeCreationHost,
      String changeCreationUser,
      String changeSummary
  ) throws NomadException {
    assertTrue(state.isInitialized());
    assertEquals(mode, state.getMode());
    assertEquals(mutativeMessageCount, state.getMutativeMessageCount());
    assertEquals(lastMutationHost, state.getLastMutationHost());
    assertEquals(lastMutationUser, state.getLastMutationUser());
    assertEquals(latestChangeUuid, state.getLatestChangeUuid());
    assertEquals(currentVersion, state.getCurrentVersion());
    assertEquals(highestVersion, state.getHighestVersion());

    if (latestChangeUuid != null) {
      ChangeRequest<String> changeRequest = state.getChangeRequest(latestChangeUuid);
      assertEquals(changeState, changeRequest.getState());
      assertEquals(changeVersion, (Long) changeRequest.getVersion());
      assertEquals(prevChangeUuid, changeRequest.getPrevChangeId());
      assertEquals(changeOperation, ((SimpleNomadChange) changeRequest.getChange()).getChange());
      assertEquals(changeResult, changeRequest.getChangeResult());
      assertEquals(changeCreationHost, changeRequest.getCreationHost());
      assertEquals(changeCreationUser, changeRequest.getCreationUser());
      assertEquals(changeSummary, changeRequest.getChange().getSummary());
    }

    DiscoverResponse<String> discoverResponse = server.discover();
    assertEquals(mode, discoverResponse.getMode());
    assertEquals(mutativeMessageCount, discoverResponse.getMutativeMessageCount());
    assertEquals(lastMutationHost, discoverResponse.getLastMutationHost());
    assertEquals(lastMutationUser, discoverResponse.getLastMutationUser());
    assertEquals(currentVersion, discoverResponse.getCurrentVersion());
    assertEquals(highestVersion, discoverResponse.getHighestVersion());

    ChangeDetails<String> latestChange = discoverResponse.getLatestChange();
    if (latestChangeUuid == null) {
      assertNull(latestChange);
    } else {
      assertEquals(latestChangeUuid, latestChange.getChangeUuid());
      assertEquals(changeState, latestChange.getState());
      assertEquals((long) changeVersion, latestChange.getVersion());
      assertEquals(changeOperation, ((SimpleNomadChange) latestChange.getOperation()).getChange());
      assertEquals(changeResult, latestChange.getResult());
      assertEquals(changeCreationHost, latestChange.getCreationHost());
      assertEquals(changeCreationUser, latestChange.getCreationUser());
      assertEquals(changeSummary, latestChange.getOperation().getSummary());
    }
  }
}
