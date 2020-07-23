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
package org.terracotta.nomad.client.change;

import org.junit.After;
import org.junit.Test;
import org.mockito.Mock;
import org.terracotta.nomad.SimpleNomadChange;
import org.terracotta.nomad.client.NomadClient;
import org.terracotta.nomad.client.NomadClientProcessTest;
import org.terracotta.nomad.messages.CommitMessage;
import org.terracotta.nomad.messages.DiscoverResponse;
import org.terracotta.nomad.messages.PrepareMessage;
import org.terracotta.nomad.messages.RollbackMessage;
import org.terracotta.nomad.server.NomadException;

import java.net.InetSocketAddress;
import java.time.Clock;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.terracotta.nomad.client.Consistency.CONSISTENT;
import static org.terracotta.nomad.client.Consistency.MAY_NEED_RECOVERY;
import static org.terracotta.nomad.client.Consistency.UNKNOWN_BUT_NO_CHANGE;
import static org.terracotta.nomad.client.Consistency.UNRECOVERABLY_INCONSISTENT;
import static org.terracotta.nomad.client.NomadTestHelper.discovery;
import static org.terracotta.nomad.client.NomadTestHelper.withItems;
import static org.terracotta.nomad.messages.AcceptRejectResponse.accept;
import static org.terracotta.nomad.messages.AcceptRejectResponse.reject;
import static org.terracotta.nomad.messages.RejectionReason.DEAD;
import static org.terracotta.nomad.messages.RejectionReason.UNACCEPTABLE;
import static org.terracotta.nomad.server.ChangeRequestState.COMMITTED;
import static org.terracotta.nomad.server.ChangeRequestState.PREPARED;
import static org.terracotta.nomad.server.ChangeRequestState.ROLLED_BACK;

public class ChangeProcessTest extends NomadClientProcessTest {

  UUID uuid1 = UUID.randomUUID();
  UUID uuid2 = UUID.randomUUID();

  @Mock
  private ChangeResultReceiver<String> results;

  @After
  public void after() {
    verifyNoMoreInteractions(results);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void makeChange() throws Exception {
    when(server1.discover()).thenReturn(discovery(COMMITTED, uuid1));
    when(server1.prepare(any(PrepareMessage.class))).thenReturn(accept());
    when(server1.commit(any(CommitMessage.class))).thenReturn(accept());
    when(server2.discover()).thenReturn(discovery(COMMITTED, uuid1));
    when(server2.prepare(any(PrepareMessage.class))).thenReturn(accept());
    when(server2.commit(any(CommitMessage.class))).thenReturn(accept());

    runTest();

    verify(results).startDiscovery(withItems(address1, address2));
    verify(results).discovered(eq(address1), any(DiscoverResponse.class));
    verify(results).discovered(eq(address2), any(DiscoverResponse.class));
    verify(results).endDiscovery();
    verify(results).startSecondDiscovery();
    verify(results).discoverRepeated(address1);
    verify(results).discoverRepeated(address2);
    verify(results).endSecondDiscovery();
    verify(results).startPrepare(any(UUID.class));
    verify(results).prepared(address1);
    verify(results).prepared(address2);
    verify(results).endPrepare();
    verify(results).startCommit();
    verify(results).committed(address1);
    verify(results).committed(address2);
    verify(results).endCommit();
    verify(results).done(CONSISTENT);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void discoverFail() throws Exception {
    when(server1.discover()).thenReturn(discovery(COMMITTED));
    when(server2.discover()).thenThrow(NomadException.class);

    runTest();

    verify(results).startDiscovery(withItems(address1, address2));
    verify(results).discovered(eq(address1), any(DiscoverResponse.class));
    verify(results).discoverFail(eq(address2), anyString());
    verify(results).endDiscovery();
    verify(results).done(UNKNOWN_BUT_NO_CHANGE);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void discoverAlreadyPrepared() throws Exception {
    when(server1.discover()).thenReturn(discovery(COMMITTED));
    when(server2.discover()).thenReturn(discovery(PREPARED));

    runTest();

    verify(results).startDiscovery(withItems(address1, address2));
    verify(results).discovered(eq(address1), any(DiscoverResponse.class));
    verify(results).discovered(eq(address2), any(DiscoverResponse.class));
    verify(results).discoverAlreadyPrepared(eq(address2), any(UUID.class), eq("testCreationHost"), eq("testCreationUser"));
    verify(results).endDiscovery();
    verify(results).done(UNKNOWN_BUT_NO_CHANGE);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void discoverInconsistentCluster() throws Exception {
    UUID uuid = UUID.randomUUID();
    when(server1.discover()).thenReturn(discovery(COMMITTED, uuid));
    when(server2.discover()).thenReturn(discovery(ROLLED_BACK, uuid));

    runTest();

    verify(results).startDiscovery(withItems(address1, address2));
    verify(results).discovered(eq(address1), any(DiscoverResponse.class));
    verify(results).discovered(eq(address2), any(DiscoverResponse.class));
    verify(results).endDiscovery();
    verify(results).startSecondDiscovery();
    verify(results).discoverRepeated(address1);
    verify(results).discoverRepeated(address2);
    verify(results).discoverClusterInconsistent(eq(uuid), withItems(address1), withItems(address2));
    verify(results).endSecondDiscovery();
    verify(results).done(UNRECOVERABLY_INCONSISTENT);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void discoverDesynchronizedCluster_commits() throws Exception {
    when(server1.discover()).thenReturn(discovery(COMMITTED, uuid1));
    when(server2.discover()).thenReturn(discovery(COMMITTED, uuid2));

    runTest();

    verify(results).startDiscovery(withItems(address1, address2));
    verify(results).discovered(eq(address1), any(DiscoverResponse.class));
    verify(results).discovered(eq(address2), any(DiscoverResponse.class));
    verify(results).endDiscovery();
    verify(results).startSecondDiscovery();
    verify(results).discoverRepeated(address1);
    verify(results).discoverRepeated(address2);
    verify(results).discoverClusterDesynchronized(any());
    verify(results).endSecondDiscovery();
    verify(results).done(UNRECOVERABLY_INCONSISTENT);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void discoverDesynchronizedCluster_rollbacks() throws Exception {
    when(server1.discover()).thenReturn(discovery(ROLLED_BACK, uuid1));
    when(server2.discover()).thenReturn(discovery(ROLLED_BACK, uuid2));

    runTest();

    verify(results).startDiscovery(withItems(address1, address2));
    verify(results).discovered(eq(address1), any(DiscoverResponse.class));
    verify(results).discovered(eq(address2), any(DiscoverResponse.class));
    verify(results).endDiscovery();
    verify(results).startSecondDiscovery();
    verify(results).discoverRepeated(address1);
    verify(results).discoverRepeated(address2);
    verify(results).discoverClusterDesynchronized(any());
    verify(results).endSecondDiscovery();
    verify(results).done(UNRECOVERABLY_INCONSISTENT);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void discoverAlreadyPrepared_diff_uuids() throws Exception {
    when(server1.discover()).thenReturn(discovery(PREPARED, uuid1));
    when(server2.discover()).thenReturn(discovery(PREPARED, uuid2));

    runTest();

    verify(results).startDiscovery(withItems(address1, address2));
    verify(results).discovered(eq(address1), any(DiscoverResponse.class));
    verify(results).discovered(eq(address2), any(DiscoverResponse.class));
    verify(results).endDiscovery();
    verify(results).discoverAlreadyPrepared(InetSocketAddress.createUnresolved("localhost", 9410), uuid1, "testCreationHost", "testCreationUser");
    verify(results).discoverAlreadyPrepared(InetSocketAddress.createUnresolved("localhost", 9411), uuid2, "testCreationHost", "testCreationUser");
    verify(results).done(UNKNOWN_BUT_NO_CHANGE);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void discoverOtherClient() throws Exception {
    when(server1.discover()).thenReturn(discovery(COMMITTED, 1L), discovery(COMMITTED, 2L));
    when(server2.discover()).thenReturn(discovery(COMMITTED));

    runTest();

    verify(results).startDiscovery(withItems(address1, address2));
    verify(results).discovered(eq(address1), any(DiscoverResponse.class));
    verify(results).discovered(eq(address2), any(DiscoverResponse.class));
    verify(results).endDiscovery();
    verify(results).startSecondDiscovery();
    verify(results).discoverOtherClient(address1, "testMutationHost", "testMutationUser");
    verify(results).discoverRepeated(address2);
    verify(results).endSecondDiscovery();
    verify(results).done(UNKNOWN_BUT_NO_CHANGE);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void prepareFail() throws Exception {
    when(server1.discover()).thenReturn(discovery(COMMITTED, uuid1));
    when(server1.prepare(any(PrepareMessage.class))).thenReturn(accept());
    when(server1.rollback(any(RollbackMessage.class))).thenReturn(accept());
    when(server2.discover()).thenReturn(discovery(COMMITTED, uuid1));
    when(server2.prepare(any(PrepareMessage.class))).thenThrow(NomadException.class);

    runTest();

    verify(results).startDiscovery(withItems(address1, address2));
    verify(results).discovered(eq(address1), any(DiscoverResponse.class));
    verify(results).discovered(eq(address2), any(DiscoverResponse.class));
    verify(results).endDiscovery();
    verify(results).startSecondDiscovery();
    verify(results).discoverRepeated(address1);
    verify(results).discoverRepeated(address2);
    verify(results).endSecondDiscovery();
    verify(results).startPrepare(any(UUID.class));
    verify(results).prepared(address1);
    verify(results).prepareFail(eq(address2), anyString());
    verify(results).endPrepare();
    verify(results).startRollback();
    verify(results).rolledBack(address1);
    verify(results).endRollback();
    verify(results).done(CONSISTENT);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void prepareFailRollbackFail() throws Exception {
    when(server1.discover()).thenReturn(discovery(COMMITTED, uuid1));
    when(server1.prepare(any(PrepareMessage.class))).thenReturn(accept());
    when(server1.rollback(any(RollbackMessage.class))).thenThrow(NomadException.class);
    when(server2.discover()).thenReturn(discovery(COMMITTED, uuid1));
    when(server2.prepare(any(PrepareMessage.class))).thenThrow(NomadException.class);

    runTest();

    verify(results).startDiscovery(withItems(address1, address2));
    verify(results).discovered(eq(address1), any(DiscoverResponse.class));
    verify(results).discovered(eq(address2), any(DiscoverResponse.class));
    verify(results).endDiscovery();
    verify(results).startSecondDiscovery();
    verify(results).discoverRepeated(address1);
    verify(results).discoverRepeated(address2);
    verify(results).endSecondDiscovery();
    verify(results).startPrepare(any(UUID.class));
    verify(results).prepared(address1);
    verify(results).prepareFail(eq(address2), anyString());
    verify(results).endPrepare();
    verify(results).startRollback();
    verify(results).rollbackFail(eq(address1), anyString());
    verify(results).endRollback();
    verify(results).done(MAY_NEED_RECOVERY);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void prepareFailRollbackOtherClient() throws Exception {
    when(server1.discover()).thenReturn(discovery(COMMITTED, uuid1));
    when(server1.prepare(any(PrepareMessage.class))).thenReturn(accept());
    when(server1.rollback(any(RollbackMessage.class))).thenReturn(reject(DEAD, "lastMutationHost", "lastMutationUser"));
    when(server2.discover()).thenReturn(discovery(COMMITTED, uuid1));
    when(server2.prepare(any(PrepareMessage.class))).thenThrow(NomadException.class);

    runTest();

    verify(results).startDiscovery(withItems(address1, address2));
    verify(results).discovered(eq(address1), any(DiscoverResponse.class));
    verify(results).discovered(eq(address2), any(DiscoverResponse.class));
    verify(results).endDiscovery();
    verify(results).startSecondDiscovery();
    verify(results).discoverRepeated(address1);
    verify(results).discoverRepeated(address2);
    verify(results).endSecondDiscovery();
    verify(results).startPrepare(any(UUID.class));
    verify(results).prepared(address1);
    verify(results).prepareFail(eq(address2), anyString());
    verify(results).endPrepare();
    verify(results).startRollback();
    verify(results).rollbackOtherClient(address1, "lastMutationHost", "lastMutationUser");
    verify(results).endRollback();
    verify(results).done(MAY_NEED_RECOVERY);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void prepareOtherClient() throws Exception {
    when(server1.discover()).thenReturn(discovery(COMMITTED, uuid1));
    when(server1.prepare(any(PrepareMessage.class))).thenReturn(accept());
    when(server1.rollback(any(RollbackMessage.class))).thenReturn(accept());
    when(server2.discover()).thenReturn(discovery(COMMITTED, uuid1));
    when(server2.prepare(any(PrepareMessage.class))).thenReturn(reject(DEAD, "lastMutationHost", "lastMutationUser"));

    runTest();

    verify(results).startDiscovery(withItems(address1, address2));
    verify(results).discovered(eq(address1), any(DiscoverResponse.class));
    verify(results).discovered(eq(address2), any(DiscoverResponse.class));
    verify(results).endDiscovery();
    verify(results).startSecondDiscovery();
    verify(results).discoverRepeated(address1);
    verify(results).discoverRepeated(address2);
    verify(results).endSecondDiscovery();
    verify(results).startPrepare(any(UUID.class));
    verify(results).prepared(address1);
    verify(results).prepareOtherClient(address2, "lastMutationHost", "lastMutationUser");
    verify(results).endPrepare();
    verify(results).startRollback();
    verify(results).rolledBack(address1);
    verify(results).endRollback();
    verify(results).done(CONSISTENT);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void prepareChangeUnacceptable() throws Exception {
    when(server1.discover()).thenReturn(discovery(COMMITTED, uuid1));
    when(server1.prepare(any(PrepareMessage.class))).thenReturn(accept());
    when(server1.rollback(any(RollbackMessage.class))).thenReturn(accept());
    when(server2.discover()).thenReturn(discovery(COMMITTED, uuid1));
    when(server2.prepare(any(PrepareMessage.class))).thenReturn(reject(UNACCEPTABLE, "reason", "lastMutationHost", "lastMutationUser"));
    when(server2.rollback(any(RollbackMessage.class))).thenReturn(accept());

    runTest();

    verify(results).startDiscovery(withItems(address1, address2));
    verify(results).discovered(eq(address1), any(DiscoverResponse.class));
    verify(results).discovered(eq(address2), any(DiscoverResponse.class));
    verify(results).endDiscovery();
    verify(results).startSecondDiscovery();
    verify(results).discoverRepeated(address1);
    verify(results).discoverRepeated(address2);
    verify(results).endSecondDiscovery();
    verify(results).startPrepare(any(UUID.class));
    verify(results).prepared(address1);
    verify(results).prepareChangeUnacceptable(address2, "reason");
    verify(results).endPrepare();
    verify(results).startRollback();
    verify(results).rolledBack(address1);
    verify(results).rolledBack(address2);
    verify(results).endRollback();
    verify(results).done(CONSISTENT);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void commitFail() throws Exception {
    when(server1.discover()).thenReturn(discovery(COMMITTED, uuid1));
    when(server1.prepare(any(PrepareMessage.class))).thenReturn(accept());
    when(server1.commit(any(CommitMessage.class))).thenThrow(NomadException.class);
    when(server2.discover()).thenReturn(discovery(COMMITTED, uuid1));
    when(server2.prepare(any(PrepareMessage.class))).thenReturn(accept());
    when(server2.commit(any(CommitMessage.class))).thenReturn(accept());

    runTest();

    verify(results).startDiscovery(withItems(address1, address2));
    verify(results).discovered(eq(address1), any(DiscoverResponse.class));
    verify(results).discovered(eq(address2), any(DiscoverResponse.class));
    verify(results).endDiscovery();
    verify(results).startSecondDiscovery();
    verify(results).discoverRepeated(address1);
    verify(results).discoverRepeated(address2);
    verify(results).endSecondDiscovery();
    verify(results).startPrepare(any(UUID.class));
    verify(results).prepared(address1);
    verify(results).prepared(address2);
    verify(results).endPrepare();
    verify(results).startCommit();
    verify(results).commitFail(eq(address1), any());
    verify(results).committed(address2);
    verify(results).endCommit();
    verify(results).done(MAY_NEED_RECOVERY);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void commitOtherClient() throws Exception {
    when(server1.discover()).thenReturn(discovery(COMMITTED, uuid1));
    when(server1.prepare(any(PrepareMessage.class))).thenReturn(accept());
    when(server1.commit(any(CommitMessage.class))).thenReturn(reject(DEAD, "lastMutationHost", "lastMutationUser"));
    when(server2.discover()).thenReturn(discovery(COMMITTED, uuid1));
    when(server2.prepare(any(PrepareMessage.class))).thenReturn(accept());
    when(server2.commit(any(CommitMessage.class))).thenReturn(accept());

    runTest();

    verify(results).startDiscovery(withItems(address1, address2));
    verify(results).discovered(eq(address1), any(DiscoverResponse.class));
    verify(results).discovered(eq(address2), any(DiscoverResponse.class));
    verify(results).endDiscovery();
    verify(results).startSecondDiscovery();
    verify(results).discoverRepeated(address1);
    verify(results).discoverRepeated(address2);
    verify(results).endSecondDiscovery();
    verify(results).startPrepare(any(UUID.class));
    verify(results).prepared(address1);
    verify(results).prepared(address2);
    verify(results).endPrepare();
    verify(results).startCommit();
    verify(results).commitOtherClient(address1, "lastMutationHost", "lastMutationUser");
    verify(results).committed(address2);
    verify(results).endCommit();
    verify(results).done(MAY_NEED_RECOVERY);
  }

  private void runTest() {
    NomadClient<String> client = new NomadClient<>(servers, "host", "user", Clock.systemUTC());
    client.tryApplyChange(results, new SimpleNomadChange("change", "summary"));
  }
}
