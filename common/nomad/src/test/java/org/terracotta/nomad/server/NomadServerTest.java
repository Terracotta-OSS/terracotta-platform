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
package org.terracotta.nomad.server;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.terracotta.nomad.SimpleNomadChange;
import org.terracotta.nomad.messages.AcceptRejectResponse;
import org.terracotta.nomad.messages.ChangeDetails;
import org.terracotta.nomad.messages.CommitMessage;
import org.terracotta.nomad.messages.DiscoverResponse;
import org.terracotta.nomad.messages.PrepareMessage;
import org.terracotta.nomad.messages.RejectionReason;
import org.terracotta.nomad.messages.RollbackMessage;
import org.terracotta.nomad.messages.TakeoverMessage;
import org.terracotta.nomad.server.state.MemoryNomadServerState;
import org.terracotta.nomad.server.state.NomadServerState;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.terracotta.nomad.messages.RejectionReason.BAD;
import static org.terracotta.nomad.messages.RejectionReason.DEAD;
import static org.terracotta.nomad.messages.RejectionReason.UNACCEPTABLE;
import static org.terracotta.nomad.server.NomadServerMode.ACCEPTING;
import static org.terracotta.nomad.server.NomadServerMode.PREPARED;

@RunWith(MockitoJUnitRunner.class)
public class NomadServerTest {
  @Mock
  private ChangeApplicator<String> changeApplicator;

  private NomadServerState<String> state;
  private NomadServerImpl<String> server;

  @Before
  public void before() throws Exception {
    state = new MemoryNomadServerState<>();
    assertFalse(state.isInitialized());
    server = new NomadServerImpl<>(state, changeApplicator);
    assertFalse(server.hasIncompleteChange());
  }

  @After
  public void after() {
    verifyNoMoreInteractions(changeApplicator);
  }

  @Test
  public void initializesState() throws Exception {
    assertState(ACCEPTING, 1L, null, null, null, 0L, 0L, null, null, null, null, null, null, null, null);
    assertFalse(server.hasIncompleteChange());
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
        Clock.systemDefaultZone().instant(),
        uuid,
        discoverResponse.getHighestVersion() + 1,
        new SimpleNomadChange("change", "summary")
    ));

    assertTrue(response.isAccepted());
    assertState(PREPARED, 2L, "testhost", "testuser", uuid, 0L, 1L, ChangeRequestState.PREPARED, 1L, null, "change", "change-applied", "testhost", "testuser", "summary");
    verify(changeApplicator).tryApply(null, new SimpleNomadChange("change", "summary"));
    assertTrue(server.hasIncompleteChange());
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
        Clock.systemDefaultZone().instant(),
        uuid,
        discoverResponse.getHighestVersion() + 1,
        new SimpleNomadChange("change", "summary")
    ));

    assertTrue(server.hasIncompleteChange());

    AcceptRejectResponse response = server.commit(new CommitMessage(
        discoverResponse.getMutativeMessageCount() + 1,
        "testhost2",
        "testuser2",
        Clock.systemDefaultZone().instant(),
        uuid
    ));

    assertTrue(response.isAccepted());
    assertState(ACCEPTING, 3L, "testhost2", "testuser2", uuid, 1L, 1L, ChangeRequestState.COMMITTED, 1L, null, "change", "change-applied", "testhost1", "testuser1", "summary");
    verify(changeApplicator).tryApply(null, new SimpleNomadChange("change", "summary"));
    verify(changeApplicator).apply(new SimpleNomadChange("change", "summary"));
    assertFalse(server.hasIncompleteChange());
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
        Clock.systemDefaultZone().instant(),
        uuid,
        discoverResponse.getHighestVersion() + 1,
        new SimpleNomadChange("change", "summary")
    ));

    assertTrue(server.hasIncompleteChange());

    AcceptRejectResponse response = server.rollback(new RollbackMessage(
        discoverResponse.getMutativeMessageCount() + 1,
        "testhost2",
        "testuser2",
        Clock.systemDefaultZone().instant(),
        uuid
    ));

    assertTrue(response.isAccepted());
    assertState(ACCEPTING, 3L, "testhost2", "testuser2", uuid, 0L, 1L, ChangeRequestState.ROLLED_BACK, 1L, null, "change", "change-applied", "testhost1", "testuser1", "summary");
    verify(changeApplicator).tryApply(null, new SimpleNomadChange("change", "summary"));
    assertFalse(server.hasIncompleteChange());
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
        Clock.systemDefaultZone().instant(),
        uuid,
        discoverResponse.getHighestVersion() + 1,
        new SimpleNomadChange("change", "summary")
    ));

    assertTrue(server.hasIncompleteChange());

    AcceptRejectResponse response = server.takeover(new TakeoverMessage(
        discoverResponse.getMutativeMessageCount() + 1,
        "testhost2",
        "testuser2",
        Clock.systemDefaultZone().instant()
    ));

    assertTrue(response.isAccepted());
    assertState(PREPARED, 3L, "testhost2", "testuser2", uuid, 0L, 1L, ChangeRequestState.PREPARED, 1L, null, "change", "change-applied", "testhost1", "testuser1", "summary");
    verify(changeApplicator).tryApply(null, new SimpleNomadChange("change", "summary"));
    assertTrue(server.hasIncompleteChange());
  }

  @Test
  public void plainTakeover() throws Exception {
    DiscoverResponse<String> discoverResponse = server.discover();

    AcceptRejectResponse response = server.takeover(new TakeoverMessage(
        discoverResponse.getMutativeMessageCount(),
        "testhost1",
        "testuser1",
        Clock.systemDefaultZone().instant()
    ));

    assertTrue(response.isAccepted());
    assertState(ACCEPTING, 2L, "testhost1", "testuser1", null, 0L, 0L, null, null, null, null, null, null, null, null);
    assertFalse(server.hasIncompleteChange());
  }

  @Test
  public void deadPrepare() throws Exception {
    DiscoverResponse<String> discoverResponse = server.discover();

    server.takeover(new TakeoverMessage(
        discoverResponse.getMutativeMessageCount(),
        "testhost1",
        "testuser1",
        Clock.systemDefaultZone().instant()
    ));

    assertFalse(server.hasIncompleteChange());

    UUID uuid = UUID.randomUUID();

    AcceptRejectResponse response = server.prepare(new PrepareMessage(
        discoverResponse.getMutativeMessageCount(),
        "testhost2",
        "testuser2",
        Clock.systemDefaultZone().instant(),
        uuid,
        discoverResponse.getHighestVersion() + 1,
        new SimpleNomadChange("change", "summary")
    ));

    assertRejection(response, DEAD, "expectedMutativeMessageCount != actualMutativeMessageCount", "testhost1", "testuser1");
    assertState(ACCEPTING, 2L, "testhost1", "testuser1", null, 0L, 0L, null, null, null, null, null, null, null, null);
    assertFalse(server.hasIncompleteChange());
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
        Clock.systemDefaultZone().instant(),
        uuid,
        discoverResponse.getHighestVersion() + 1,
        new SimpleNomadChange("change", "summary")
    ));

    assertTrue(server.hasIncompleteChange());

    server.takeover(new TakeoverMessage(
        discoverResponse.getMutativeMessageCount() + 1,
        "testhost1",
        "testuser1",
        Clock.systemDefaultZone().instant()
    ));

    assertTrue(server.hasIncompleteChange());

    AcceptRejectResponse response = server.commit(new CommitMessage(
        discoverResponse.getMutativeMessageCount() + 1,
        "testhost2",
        "testuser2",
        Clock.systemDefaultZone().instant(), uuid
    ));

    assertRejection(response, DEAD, "expectedMutativeMessageCount != actualMutativeMessageCount", "testhost1", "testuser1");
    assertState(PREPARED, 3L, "testhost1", "testuser1", uuid, 0L, 1L, ChangeRequestState.PREPARED, 1L, null, "change", "change-applied", "testhost1", "testuser1", "summary");
    verify(changeApplicator).tryApply(null, new SimpleNomadChange("change", "summary"));
    assertTrue(server.hasIncompleteChange());
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
        Clock.systemDefaultZone().instant(),
        uuid,
        discoverResponse.getHighestVersion() + 1,
        new SimpleNomadChange("change", "summary")
    ));

    assertTrue(server.hasIncompleteChange());

    server.takeover(new TakeoverMessage(
        discoverResponse.getMutativeMessageCount() + 1,
        "testhost1",
        "testuser1",
        Clock.systemDefaultZone().instant()
    ));

    assertTrue(server.hasIncompleteChange());

    AcceptRejectResponse response = server.rollback(new RollbackMessage(
        discoverResponse.getMutativeMessageCount() + 1,
        "testhost2",
        "testuser2",
        Clock.systemDefaultZone().instant(), uuid
    ));

    assertRejection(response, DEAD, "expectedMutativeMessageCount != actualMutativeMessageCount", "testhost1", "testuser1");
    assertState(PREPARED, 3L, "testhost1", "testuser1", uuid, 0L, 1L, ChangeRequestState.PREPARED, 1L, null, "change", "change-applied", "testhost1", "testuser1", "summary");
    verify(changeApplicator).tryApply(null, new SimpleNomadChange("change", "summary"));
    assertTrue(server.hasIncompleteChange());
  }

  @Test
  public void deadTakeover() throws Exception {
    when(changeApplicator.tryApply(null, new SimpleNomadChange("change", "summary"))).thenReturn(PotentialApplicationResult.allow("change-applied"));

    DiscoverResponse<String> discoverResponse = server.discover();

    UUID uuid = UUID.randomUUID();

    assertFalse(server.hasIncompleteChange());

    server.prepare(new PrepareMessage(
        discoverResponse.getMutativeMessageCount(),
        "testhost1",
        "testuser1",
        Clock.systemDefaultZone().instant(),
        uuid,
        discoverResponse.getHighestVersion() + 1,
        new SimpleNomadChange("change", "summary")
    ));

    assertTrue(server.hasIncompleteChange());

    server.takeover(new TakeoverMessage(
        discoverResponse.getMutativeMessageCount() + 1,
        "testhost1",
        "testuser1",
        Clock.systemDefaultZone().instant()
    ));

    assertTrue(server.hasIncompleteChange());

    AcceptRejectResponse response = server.takeover(new TakeoverMessage(
        discoverResponse.getMutativeMessageCount() + 1,
        "testhost2",
        "testuser2",
        Clock.systemDefaultZone().instant()
    ));

    assertTrue(server.hasIncompleteChange());

    assertRejection(response, DEAD, "expectedMutativeMessageCount != actualMutativeMessageCount", "testhost1", "testuser1");
    assertState(PREPARED, 3L, "testhost1", "testuser1", uuid, 0L, 1L, ChangeRequestState.PREPARED, 1L, null, "change", "change-applied", "testhost1", "testuser1", "summary");
    verify(changeApplicator).tryApply(null, new SimpleNomadChange("change", "summary"));
  }

  @Test
  public void prepareUnacceptableChange() throws Exception {
    when(changeApplicator.tryApply(null, new SimpleNomadChange("change", "summary"))).thenReturn(PotentialApplicationResult.reject(null, "fail"));

    DiscoverResponse<String> discoverResponse = server.discover();

    UUID uuid = UUID.randomUUID();

    assertFalse(server.hasIncompleteChange());

    AcceptRejectResponse response = server.prepare(new PrepareMessage(
        discoverResponse.getMutativeMessageCount(),
        "testhost",
        "testuser",
        Clock.systemDefaultZone().instant(),
        uuid,
        discoverResponse.getHighestVersion() + 1,
        new SimpleNomadChange("change", "summary")
    ));

    assertFalse(server.hasIncompleteChange());

    assertRejection(response, UNACCEPTABLE, "fail", null, null);
    assertState(ACCEPTING, 1L, null, null, null, 0L, 0L, null, null, null, null, null, null, null, null);
    verify(changeApplicator).tryApply(null, new SimpleNomadChange("change", "summary"));
  }

  @Test
  public void prepareWrongMode() throws Exception {
    when(changeApplicator.tryApply(null, new SimpleNomadChange("change", "summary"))).thenReturn(PotentialApplicationResult.allow("change-applied"));

    DiscoverResponse<String> discoverResponse = server.discover();
    UUID uuid = UUID.randomUUID();

    server.prepare(new PrepareMessage(
        discoverResponse.getMutativeMessageCount(),
        "host",
        "user",
        Clock.systemDefaultZone().instant(),
        uuid,
        discoverResponse.getHighestVersion() + 1,
        new SimpleNomadChange("change", "summary")
    ));

    assertState(PREPARED, 2L, "host", "user", uuid, 0L, 1L, ChangeRequestState.PREPARED, 1L, null, "change", "change-applied", "host", "user", "summary");
    verify(changeApplicator).tryApply(null, new SimpleNomadChange("change", "summary"));

    AcceptRejectResponse response = server.prepare(new PrepareMessage(
        discoverResponse.getMutativeMessageCount() + 1,
        "testhost",
        "testuser",
        Clock.systemDefaultZone().instant(),
        uuid,
        discoverResponse.getHighestVersion() + 2,
        new SimpleNomadChange("change", "summary")
    ));

    assertRejection(response, BAD, "Expected mode: ACCEPTING. Was: PREPARED", "host", "user");
  }

  @Test
  public void prepareWrongVersion() throws Exception {
    DiscoverResponse<String> discoverResponse = server.discover();
    UUID uuid = UUID.randomUUID();

    AcceptRejectResponse response = server.prepare(new PrepareMessage(
        discoverResponse.getMutativeMessageCount(),
        "host",
        "user",
        Clock.systemDefaultZone().instant(),
        uuid,
        0,
        new SimpleNomadChange("change", "summary")
    ));

    assertRejection(response, BAD, "Wrong change version number", null, null);
  }

  @Test
  public void prepareWrongUUID() throws Exception {
    when(changeApplicator.tryApply(null, new SimpleNomadChange("change", "summary"))).thenReturn(PotentialApplicationResult.allow("change-applied"));

    DiscoverResponse<String> discoverResponse = server.discover();
    UUID uuid = UUID.randomUUID();

    server.prepare(new PrepareMessage(
        discoverResponse.getMutativeMessageCount(),
        "testhost1",
        "testuser1",
        Clock.systemDefaultZone().instant(),
        uuid,
        discoverResponse.getHighestVersion() + 1,
        new SimpleNomadChange("change", "summary")
    ));

    verify(changeApplicator).tryApply(null, new SimpleNomadChange("change", "summary"));

    server.commit(new CommitMessage(
        discoverResponse.getMutativeMessageCount() + 1,
        "testhost2",
        "testuser2",
        Clock.systemDefaultZone().instant(),
        uuid
    ));

    verify(changeApplicator).apply(new SimpleNomadChange("change", "summary"));

    AcceptRejectResponse response = server.prepare(new PrepareMessage(
        discoverResponse.getMutativeMessageCount() + 2,
        "host",
        "user",
        Clock.systemDefaultZone().instant(),
        uuid,
        discoverResponse.getHighestVersion() + 2,
        new SimpleNomadChange("change", "summary")
    ));

    assertRejection(response, BAD, "Received an alive PrepareMessage for a change that already exists: " + uuid, "testhost2", "testuser2");
  }

  @Test
  public void testCommitWithPrevId() throws Exception {
    when(changeApplicator.tryApply(null, new SimpleNomadChange("change", "summary"))).thenReturn(PotentialApplicationResult.allow("change-applied"));

    DiscoverResponse<String> discoverResponse = server.discover();

    UUID firstChangeUuid = UUID.randomUUID();

    assertFalse(server.hasIncompleteChange());

    server.prepare(new PrepareMessage(
        discoverResponse.getMutativeMessageCount(),
        "testhost1",
        "testuser1",
        Clock.systemDefaultZone().instant(),
        firstChangeUuid,
        discoverResponse.getHighestVersion() + 1,
        new SimpleNomadChange("change", "summary")
    ));

    assertTrue(server.hasIncompleteChange());

    AcceptRejectResponse response = server.commit(new CommitMessage(
        discoverResponse.getMutativeMessageCount() + 1,
        "testhost2",
        "testuser2",
        Clock.systemDefaultZone().instant(),
        firstChangeUuid
    ));

    assertTrue(response.isAccepted());
    assertState(ACCEPTING, 3L, "testhost2", "testuser2", firstChangeUuid, 1L, 1L, ChangeRequestState.COMMITTED, 1L, null, "change", "change-applied", "testhost1", "testuser1", "summary");
    verify(changeApplicator).tryApply(null, new SimpleNomadChange("change", "summary"));
    verify(changeApplicator).apply(new SimpleNomadChange("change", "summary"));
    assertFalse(server.hasIncompleteChange());

    // Apply more changes
    when(changeApplicator.tryApply("change-applied", new SimpleNomadChange("change1", "summary1"))).thenReturn(PotentialApplicationResult.allow("change-applied1"));
    discoverResponse = server.discover();
    UUID nextChangeUuid = UUID.randomUUID();

    server.prepare(new PrepareMessage(
        discoverResponse.getMutativeMessageCount(),
        "testhost1",
        "testuser1",
        Clock.systemDefaultZone().instant(),
        nextChangeUuid,
        discoverResponse.getHighestVersion() + 1,
        new SimpleNomadChange("change1", "summary1")
    ));

    response = server.commit(new CommitMessage(
        discoverResponse.getMutativeMessageCount() + 1,
        "testhost2",
        "testuser2",
        Clock.systemDefaultZone().instant(),
        nextChangeUuid
    ));

    assertTrue(response.isAccepted());
    assertState(ACCEPTING, 5L, "testhost2", "testuser2", nextChangeUuid, 2L, 2L, ChangeRequestState.COMMITTED, 2L, firstChangeUuid.toString(), "change1", "change-applied1", "testhost1", "testuser1", "summary1");
    verify(changeApplicator).tryApply("change-applied", new SimpleNomadChange("change1", "summary1"));
    verify(changeApplicator).apply(new SimpleNomadChange("change1", "summary1"));
  }

  @Test
  public void commitWrongMode() throws Exception {
    when(changeApplicator.tryApply(null, new SimpleNomadChange("change", "summary"))).thenReturn(PotentialApplicationResult.allow("change-applied"));

    DiscoverResponse<String> discoverResponse = server.discover();
    UUID firstChangeUuid = UUID.randomUUID();

    server.prepare(new PrepareMessage(
        discoverResponse.getMutativeMessageCount(),
        "testhost1",
        "testuser1",
        Clock.systemDefaultZone().instant(),
        firstChangeUuid,
        discoverResponse.getHighestVersion() + 1,
        new SimpleNomadChange("change", "summary")
    ));

    server.commit(new CommitMessage(
        discoverResponse.getMutativeMessageCount() + 1,
        "testhost2",
        "testuser2",
        Clock.systemDefaultZone().instant(),
        firstChangeUuid
    ));

    verify(changeApplicator).tryApply(null, new SimpleNomadChange("change", "summary"));
    verify(changeApplicator).apply(new SimpleNomadChange("change", "summary"));

    AcceptRejectResponse response = server.commit(new CommitMessage(
        discoverResponse.getMutativeMessageCount() + 2,
        "testhost2",
        "testuser2",
        Clock.systemDefaultZone().instant(),
        firstChangeUuid
    ));

    assertRejection(response, BAD, "Expected mode: PREPARED. Was: ACCEPTING", "testhost2", "testuser2");
  }

  @Test
  public void commitWrongChange() throws Exception {
    when(changeApplicator.tryApply(null, new SimpleNomadChange("change", "summary"))).thenReturn(PotentialApplicationResult.allow("change-applied"));

    DiscoverResponse<String> discoverResponse = server.discover();
    UUID firstChangeUuid = UUID.randomUUID();

    server.prepare(new PrepareMessage(
        discoverResponse.getMutativeMessageCount(),
        "testhost1",
        "testuser1",
        Clock.systemDefaultZone().instant(),
        firstChangeUuid,
        discoverResponse.getHighestVersion() + 1,
        new SimpleNomadChange("change", "summary")
    ));

    verify(changeApplicator).tryApply(null, new SimpleNomadChange("change", "summary"));

    UUID inexisting = UUID.randomUUID();

    AcceptRejectResponse response = server.commit(new CommitMessage(
        discoverResponse.getMutativeMessageCount() + 1,
        "testhost2",
        "testuser2",
        Clock.systemDefaultZone().instant(),
        inexisting
    ));

    assertRejection(response, BAD, "Received an alive CommitMessage for a change that does not exist: " + inexisting, "testhost1", "testuser1");
  }

  @Test
  public void rollbackWrongMode() throws Exception {
    when(changeApplicator.tryApply(null, new SimpleNomadChange("change", "summary"))).thenReturn(PotentialApplicationResult.allow("change-applied"));

    DiscoverResponse<String> discoverResponse = server.discover();
    UUID firstChangeUuid = UUID.randomUUID();

    server.prepare(new PrepareMessage(
        discoverResponse.getMutativeMessageCount(),
        "testhost1",
        "testuser1",
        Clock.systemDefaultZone().instant(),
        firstChangeUuid,
        discoverResponse.getHighestVersion() + 1,
        new SimpleNomadChange("change", "summary")
    ));

    server.commit(new CommitMessage(
        discoverResponse.getMutativeMessageCount() + 1,
        "testhost2",
        "testuser2",
        Clock.systemDefaultZone().instant(),
        firstChangeUuid
    ));

    verify(changeApplicator).tryApply(null, new SimpleNomadChange("change", "summary"));
    verify(changeApplicator).apply(new SimpleNomadChange("change", "summary"));

    AcceptRejectResponse response = server.rollback(new RollbackMessage(
        discoverResponse.getMutativeMessageCount() + 2,
        "testhost2",
        "testuser2",
        Clock.systemDefaultZone().instant(),
        firstChangeUuid
    ));

    assertRejection(response, BAD, "Expected mode: PREPARED. Was: ACCEPTING", "testhost2", "testuser2");
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testSetChangeApplicatorAlreadySet() throws Exception {
    NomadServerState<String> serverState = mock(NomadServerState.class);
    when(serverState.isInitialized()).thenReturn(true);
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
  @Test
  public void testSetNullChangeApplicator() throws Exception {
    NomadServerState<String> serverState = mock(NomadServerState.class);
    when(serverState.isInitialized()).thenReturn(true);
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
        Clock.systemDefaultZone().instant(),
        firstChangeUuid,
        discoverResponse.getHighestVersion() + 1,
        new SimpleNomadChange("change", "summary")
    ));

    AcceptRejectResponse response = server.commit(new CommitMessage(
        discoverResponse.getMutativeMessageCount() + 1,
        "testhost2",
        "testuser2",
        Clock.systemDefaultZone().instant(),
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
        Clock.systemDefaultZone().instant(),
        nextChangeUuid,
        discoverResponse.getHighestVersion() + 1,
        new SimpleNomadChange("change1", "summary1")
    ));

    response = server.commit(new CommitMessage(
        discoverResponse.getMutativeMessageCount() + 1,
        "testhost2",
        "testuser2",
        Clock.systemDefaultZone().instant(),
        nextChangeUuid
    ));

    assertTrue(response.isAccepted());
    verify(changeApplicator).tryApply("change-applied", new SimpleNomadChange("change1", "summary1"));
    verify(changeApplicator).apply(new SimpleNomadChange("change1", "summary1"));

    //Verifying changes are as it happened
    List<NomadChangeInfo> getAllChanges = server.getAllNomadChanges();
    assertEquals(getAllChanges.size(), 2);
    assertEquals(getAllChanges.get(0).getChangeUuid(), firstChangeUuid);
    assertEquals(getAllChanges.get(0).getNomadChange(), new SimpleNomadChange("change", "summary"));
    assertEquals(getAllChanges.get(1).getChangeUuid(), nextChangeUuid);
    assertEquals(getAllChanges.get(1).getNomadChange(), new SimpleNomadChange("change1", "summary1"));
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
