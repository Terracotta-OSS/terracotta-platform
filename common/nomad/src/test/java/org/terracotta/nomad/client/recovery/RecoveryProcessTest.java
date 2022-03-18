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
package org.terracotta.nomad.client.recovery;

import org.junit.After;
import org.junit.Test;
import org.mockito.Mock;
import org.terracotta.nomad.client.NomadClient;
import org.terracotta.nomad.client.NomadClientProcessTest;
import org.terracotta.nomad.messages.CommitMessage;
import org.terracotta.nomad.messages.DiscoverResponse;
import org.terracotta.nomad.messages.RollbackMessage;
import org.terracotta.nomad.messages.TakeoverMessage;
import org.terracotta.nomad.server.NomadException;

import java.time.Clock;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
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
import static org.terracotta.nomad.server.ChangeRequestState.COMMITTED;
import static org.terracotta.nomad.server.ChangeRequestState.PREPARED;
import static org.terracotta.nomad.server.ChangeRequestState.ROLLED_BACK;

public class RecoveryProcessTest extends NomadClientProcessTest {
  @Mock
  protected RecoveryResultReceiver<String> results;

  @After
  public void after() {
    verifyNoMoreInteractions(results);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void partialCommit() throws Exception {
    UUID uuid = UUID.randomUUID();
    when(server1.discover()).thenReturn(discovery(COMMITTED, uuid));
    when(server1.takeover(any(TakeoverMessage.class))).thenReturn(accept());
    when(server2.discover()).thenReturn(discovery(PREPARED, uuid));
    when(server2.takeover(any(TakeoverMessage.class))).thenReturn(accept());
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
    verify(results).startTakeover();
    verify(results).takeover(address1);
    verify(results).takeover(address2);
    verify(results).endTakeover();
    verify(results).startCommit();
    verify(results).committed(address2);
    verify(results).endCommit();
    verify(results).done(CONSISTENT);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void partialPrepare() throws Exception {
    when(server1.discover()).thenReturn(discovery(COMMITTED));
    when(server1.takeover(any(TakeoverMessage.class))).thenReturn(accept());
    when(server2.discover()).thenReturn(discovery(PREPARED));
    when(server2.takeover(any(TakeoverMessage.class))).thenReturn(accept());
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
    verify(results).startTakeover();
    verify(results).takeover(address1);
    verify(results).takeover(address2);
    verify(results).endTakeover();
    verify(results).startRollback();
    verify(results).rolledBack(address2);
    verify(results).endRollback();
    verify(results).done(CONSISTENT);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void alreadyConsistent() throws Exception {
    UUID uuid = UUID.randomUUID();
    when(server1.discover()).thenReturn(discovery(COMMITTED, uuid));
    when(server2.discover()).thenReturn(discovery(COMMITTED, uuid));

    runTest();

    verify(results).startDiscovery(withItems(address1, address2));
    verify(results).discovered(eq(address1), any(DiscoverResponse.class));
    verify(results).discovered(eq(address2), any(DiscoverResponse.class));
    verify(results).endDiscovery();
    verify(results).startSecondDiscovery();
    verify(results).discoverRepeated(address1);
    verify(results).discoverRepeated(address2);
    verify(results).endSecondDiscovery();
    verify(results).done(CONSISTENT);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void discoverFail() throws Exception {
    when(server1.discover()).thenReturn(discovery(COMMITTED));
    when(server2.discover()).thenThrow(NomadException.class);

    runTest();

    verify(results).startDiscovery(withItems(address1, address2));
    verify(results).discovered(eq(address1), any(DiscoverResponse.class));
    verify(results).discoverFail(eq(address2), any());
    verify(results).endDiscovery();
    verify(results).done(UNKNOWN_BUT_NO_CHANGE);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void discoverInconsistentConfig() throws Exception {
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
    verify(results).discoverConfigInconsistent(eq(uuid), withItems(address1), withItems(address2));
    verify(results).endSecondDiscovery();
    verify(results).done(UNRECOVERABLY_INCONSISTENT);
  }

  @Test
  @SuppressWarnings("unchecked")
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

  @Test
  @SuppressWarnings("unchecked")
  public void takeoverFail() throws Exception {
    when(server1.discover()).thenReturn(discovery(COMMITTED));
    when(server1.takeover(any(TakeoverMessage.class))).thenReturn(accept());
    when(server2.discover()).thenReturn(discovery(PREPARED));
    when(server2.takeover(any(TakeoverMessage.class))).thenThrow(NomadException.class);

    runTest();

    verify(results).startDiscovery(withItems(address1, address2));
    verify(results).discovered(eq(address1), any(DiscoverResponse.class));
    verify(results).discovered(eq(address2), any(DiscoverResponse.class));
    verify(results).endDiscovery();
    verify(results).startSecondDiscovery();
    verify(results).discoverRepeated(address1);
    verify(results).discoverRepeated(address2);
    verify(results).endSecondDiscovery();
    verify(results).startTakeover();
    verify(results).takeover(address1);
    verify(results).takeoverFail(eq(address2), any());
    verify(results).endTakeover();
    verify(results).done(MAY_NEED_RECOVERY);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void takeoverOtherClient() throws Exception {
    when(server1.discover()).thenReturn(discovery(COMMITTED));
    when(server1.takeover(any(TakeoverMessage.class))).thenReturn(accept());
    when(server2.discover()).thenReturn(discovery(PREPARED));
    when(server2.takeover(any(TakeoverMessage.class))).thenReturn(reject(DEAD, "lastMutationHost", "lastMutationUser"));

    runTest();

    verify(results).startDiscovery(withItems(address1, address2));
    verify(results).discovered(eq(address1), any(DiscoverResponse.class));
    verify(results).discovered(eq(address2), any(DiscoverResponse.class));
    verify(results).endDiscovery();
    verify(results).startSecondDiscovery();
    verify(results).discoverRepeated(address1);
    verify(results).discoverRepeated(address2);
    verify(results).endSecondDiscovery();
    verify(results).startTakeover();
    verify(results).takeover(address1);
    verify(results).takeoverOtherClient(address2, "lastMutationHost", "lastMutationUser");
    verify(results).endTakeover();
    verify(results).done(MAY_NEED_RECOVERY);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void commitFail() throws Exception {
    UUID uuid = UUID.randomUUID();
    when(server1.discover()).thenReturn(discovery(COMMITTED, uuid));
    when(server1.takeover(any(TakeoverMessage.class))).thenReturn(accept());
    when(server2.discover()).thenReturn(discovery(PREPARED, uuid));
    when(server2.takeover(any(TakeoverMessage.class))).thenReturn(accept());
    when(server2.commit(any(CommitMessage.class))).thenThrow(NomadException.class);

    runTest();

    verify(results).startDiscovery(withItems(address1, address2));
    verify(results).discovered(eq(address1), any(DiscoverResponse.class));
    verify(results).discovered(eq(address2), any(DiscoverResponse.class));
    verify(results).endDiscovery();
    verify(results).startSecondDiscovery();
    verify(results).discoverRepeated(address1);
    verify(results).discoverRepeated(address2);
    verify(results).endSecondDiscovery();
    verify(results).startTakeover();
    verify(results).takeover(address1);
    verify(results).takeover(address2);
    verify(results).endTakeover();
    verify(results).startCommit();
    verify(results).commitFail(eq(address2), any());
    verify(results).endCommit();
    verify(results).done(MAY_NEED_RECOVERY);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void commitOtherClient() throws Exception {
    UUID uuid = UUID.randomUUID();
    when(server1.discover()).thenReturn(discovery(COMMITTED, uuid));
    when(server1.takeover(any(TakeoverMessage.class))).thenReturn(accept());
    when(server2.discover()).thenReturn(discovery(PREPARED, uuid));
    when(server2.takeover(any(TakeoverMessage.class))).thenReturn(accept());
    when(server2.commit(any(CommitMessage.class))).thenReturn(reject(DEAD, "lastMutationHost", "lastMutationUser"));

    runTest();

    verify(results).startDiscovery(withItems(address1, address2));
    verify(results).discovered(eq(address1), any(DiscoverResponse.class));
    verify(results).discovered(eq(address2), any(DiscoverResponse.class));
    verify(results).endDiscovery();
    verify(results).startSecondDiscovery();
    verify(results).discoverRepeated(address1);
    verify(results).discoverRepeated(address2);
    verify(results).endSecondDiscovery();
    verify(results).startTakeover();
    verify(results).takeover(address1);
    verify(results).takeover(address2);
    verify(results).endTakeover();
    verify(results).startCommit();
    verify(results).commitOtherClient(address2, "lastMutationHost", "lastMutationUser");
    verify(results).endCommit();
    verify(results).done(MAY_NEED_RECOVERY);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void rollbackFail() throws Exception {
    when(server1.discover()).thenReturn(discovery(COMMITTED));
    when(server1.takeover(any(TakeoverMessage.class))).thenReturn(accept());
    when(server2.discover()).thenReturn(discovery(PREPARED));
    when(server2.takeover(any(TakeoverMessage.class))).thenReturn(accept());
    when(server2.rollback(any(RollbackMessage.class))).thenThrow(NomadException.class);

    runTest();

    verify(results).startDiscovery(withItems(address1, address2));
    verify(results).discovered(eq(address1), any(DiscoverResponse.class));
    verify(results).discovered(eq(address2), any(DiscoverResponse.class));
    verify(results).endDiscovery();
    verify(results).startSecondDiscovery();
    verify(results).discoverRepeated(address1);
    verify(results).discoverRepeated(address2);
    verify(results).endSecondDiscovery();
    verify(results).startTakeover();
    verify(results).takeover(address1);
    verify(results).takeover(address2);
    verify(results).endTakeover();
    verify(results).startRollback();
    verify(results).rollbackFail(eq(address2), any());
    verify(results).endRollback();
    verify(results).done(MAY_NEED_RECOVERY);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void rollbackOtherClient() throws Exception {
    when(server1.discover()).thenReturn(discovery(COMMITTED));
    when(server1.takeover(any(TakeoverMessage.class))).thenReturn(accept());
    when(server2.discover()).thenReturn(discovery(PREPARED));
    when(server2.takeover(any(TakeoverMessage.class))).thenReturn(accept());
    when(server2.rollback(any(RollbackMessage.class))).thenReturn(reject(DEAD, "lastMutationHost", "lastMutationUser"));

    runTest();

    verify(results).startDiscovery(withItems(address1, address2));
    verify(results).discovered(eq(address1), any(DiscoverResponse.class));
    verify(results).discovered(eq(address2), any(DiscoverResponse.class));
    verify(results).endDiscovery();
    verify(results).startSecondDiscovery();
    verify(results).discoverRepeated(address1);
    verify(results).discoverRepeated(address2);
    verify(results).endSecondDiscovery();
    verify(results).startTakeover();
    verify(results).takeover(address1);
    verify(results).takeover(address2);
    verify(results).endTakeover();
    verify(results).startRollback();
    verify(results).rollbackOtherClient(address2, "lastMutationHost", "lastMutationUser");
    verify(results).endRollback();
    verify(results).done(MAY_NEED_RECOVERY);
  }

  private void runTest() {
    NomadClient<String> client = new NomadClient<>(servers, "host", "user", Clock.systemUTC());
    client.tryRecovery(results, 2, null);
  }
}
