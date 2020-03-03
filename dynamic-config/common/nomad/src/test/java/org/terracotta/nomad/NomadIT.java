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
package org.terracotta.nomad;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.terracotta.nomad.client.NomadClient;
import org.terracotta.nomad.client.NomadEndpoint;
import org.terracotta.nomad.client.change.ChangeResultReceiver;
import org.terracotta.nomad.client.recovery.RecoveryResultReceiver;
import org.terracotta.nomad.messages.AcceptRejectResponse;
import org.terracotta.nomad.messages.CommitMessage;
import org.terracotta.nomad.messages.DiscoverResponse;
import org.terracotta.nomad.messages.PrepareMessage;
import org.terracotta.nomad.messages.RollbackMessage;
import org.terracotta.nomad.messages.TakeoverMessage;
import org.terracotta.nomad.server.ChangeApplicator;
import org.terracotta.nomad.server.NomadException;
import org.terracotta.nomad.server.NomadServer;
import org.terracotta.nomad.server.NomadServerImpl;
import org.terracotta.nomad.server.state.MemoryNomadServerState;
import org.terracotta.nomad.server.state.NomadServerState;

import java.net.InetSocketAddress;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.terracotta.nomad.client.Consistency.CONSISTENT;
import static org.terracotta.nomad.client.Consistency.MAY_NEED_RECOVERY;
import static org.terracotta.nomad.client.NomadTestHelper.withItems;
import static org.terracotta.nomad.server.NomadServerMode.ACCEPTING;
import static org.terracotta.nomad.server.PotentialApplicationResult.allow;
import static org.terracotta.nomad.server.PotentialApplicationResult.reject;

@RunWith(MockitoJUnitRunner.class)
public class NomadIT {
  @Mock
  private ChangeResultReceiver<String> changeResults;

  @Mock
  private RecoveryResultReceiver<String> recoveryResults;

  @Mock
  private ChangeApplicator<String> changeApplicator1;

  @Mock
  private ChangeApplicator<String> changeApplicator2;

  @Mock
  private ChangeApplicator<String> changeApplicator3;

  private NomadServerState<String> serverState1;
  private List<NomadEndpoint<String>> servers;
  private NomadClient<String> client;
  private boolean assertNoMoreInteractions = true;
  private InetSocketAddress address1 = InetSocketAddress.createUnresolved("localhost", 9410);
  private InetSocketAddress address2 = InetSocketAddress.createUnresolved("localhost", 9411);
  private InetSocketAddress address3 = InetSocketAddress.createUnresolved("localhost", 9412);

  @Before
  @SuppressWarnings("unchecked")
  public void before() throws Exception {
    serverState1 = new MemoryNomadServerState<>();
    NomadServerState<String> serverState2 = new MemoryNomadServerState<>();
    NomadServerState<String> serverState3 = new MemoryNomadServerState<>();
    NomadServerImpl<String> serverImpl1 = new NomadServerImpl<>(serverState1);
    NomadServerImpl<String> serverImpl2 = new NomadServerImpl<>(serverState2);
    NomadServerImpl<String> serverImpl3 = new NomadServerImpl<>(serverState3);

    serverImpl1.setChangeApplicator(changeApplicator1);
    serverImpl2.setChangeApplicator(changeApplicator2);
    serverImpl3.setChangeApplicator(changeApplicator3);

    NomadEndpoint<String> server1 = new NomadEndpoint<>(address1, serverImpl1);
    NomadEndpoint<String> server2 = new NomadEndpoint<>(address2, serverImpl2);
    NomadEndpoint<String> server3 = new NomadEndpoint<>(address3, serverImpl3);

    servers = new ArrayList<>(Arrays.asList(server1, server2, server3));
    client = new NomadClient<>(servers, "host", "user", Clock.systemUTC());
  }

  @After
  public void after() {
    if (assertNoMoreInteractions) {
      verifyNoMoreInteractions(changeResults);
      verifyNoMoreInteractions(recoveryResults);
      verifyNoMoreInteractions(changeApplicator1);
      verifyNoMoreInteractions(changeApplicator2);
      verifyNoMoreInteractions(changeApplicator3);
    }
  }

  @SuppressWarnings("unchecked")
  @Test
  public void applyChange() throws Exception {
    SimpleNomadChange change = new SimpleNomadChange("change", "summary");

    when(changeApplicator1.tryApply(null, change)).thenReturn(allow("changeResult"));
    when(changeApplicator2.tryApply(null, change)).thenReturn(allow("changeResult"));
    when(changeApplicator3.tryApply(null, change)).thenReturn(allow("changeResult"));

    client.tryApplyChange(changeResults, change);

    verify(changeApplicator1).tryApply(null, change);
    verify(changeApplicator2).tryApply(null, change);
    verify(changeApplicator3).tryApply(null, change);
    verify(changeApplicator1).apply(change);
    verify(changeApplicator2).apply(change);
    verify(changeApplicator3).apply(change);
    verify(changeResults).startDiscovery(withItems(address1, address2, address3));
    verify(changeResults).discovered(eq(address1), any(DiscoverResponse.class));
    verify(changeResults).discovered(eq(address2), any(DiscoverResponse.class));
    verify(changeResults).discovered(eq(address3), any(DiscoverResponse.class));
    verify(changeResults).endDiscovery();
    verify(changeResults).startSecondDiscovery();
    verify(changeResults).discoverRepeated(address1);
    verify(changeResults).discoverRepeated(address2);
    verify(changeResults).discoverRepeated(address3);
    verify(changeResults).endSecondDiscovery();
    verify(changeResults).startPrepare(any(UUID.class));
    verify(changeResults).prepared(address1);
    verify(changeResults).prepared(address2);
    verify(changeResults).prepared(address3);
    verify(changeResults).endPrepare();
    verify(changeResults).startCommit();
    verify(changeResults).committed(address1);
    verify(changeResults).committed(address2);
    verify(changeResults).committed(address3);
    verify(changeResults).endCommit();
    verify(changeResults).done(CONSISTENT);
    verifyNoMoreInteractions(changeResults);

    assertEquals(ACCEPTING, serverState1.getMode());
    assertEquals("host", serverState1.getLastMutationHost());
    assertEquals("user", serverState1.getLastMutationUser());
    assertEquals(1L, serverState1.getCurrentVersion());
  }

  @Test
  public void applyMultipleChanges() throws Exception {
    SimpleNomadChange change1 = new SimpleNomadChange("change1", "summary1");
    SimpleNomadChange change2 = new SimpleNomadChange("change2", "summary2");

    when(changeApplicator1.tryApply(null, change1)).thenReturn(allow("changeResult1"));
    when(changeApplicator1.tryApply("changeResult1", change2)).thenReturn(allow("changeResult2"));
    when(changeApplicator2.tryApply(null, change1)).thenReturn(allow("changeResult1"));
    when(changeApplicator2.tryApply("changeResult1", change2)).thenReturn(allow("changeResult2"));
    when(changeApplicator3.tryApply(null, change1)).thenReturn(allow("changeResult1"));
    when(changeApplicator3.tryApply("changeResult1", change2)).thenReturn(allow("changeResult2"));

    client.tryApplyChange(changeResults, change1);
    client.tryApplyChange(changeResults, change2);

    verify(changeApplicator1).apply(change1);
    verify(changeApplicator1).apply(change2);
    verify(changeApplicator2).apply(change1);
    verify(changeApplicator2).apply(change2);
    verify(changeApplicator3).apply(change1);
    verify(changeApplicator3).apply(change2);
    verify(changeResults, times(2)).committed(address1);
    verify(changeResults, times(2)).committed(address2);
    verify(changeResults, times(2)).committed(address3);
    verify(changeResults, times(2)).done(CONSISTENT);

    assertEquals(2L, serverState1.getCurrentVersion());

    assertNoMoreInteractions = false;
  }

  @SuppressWarnings("unchecked")
  @Test
  public void rejectChange() {
    SimpleNomadChange change = new SimpleNomadChange("change", "summary");

    when(changeApplicator1.tryApply(null, change)).thenReturn(allow("changeResult"));
    when(changeApplicator2.tryApply(null, change)).thenReturn(reject("fail"));
    when(changeApplicator3.tryApply(null, change)).thenReturn(allow("changeResult"));

    client.tryApplyChange(changeResults, change);

    verify(changeApplicator1).tryApply(null, change);
    verify(changeApplicator2).tryApply(null, change);
    verify(changeApplicator3).tryApply(null, change);
    verify(changeResults).startDiscovery(withItems(address1, address2, address3));
    verify(changeResults).discovered(eq(address1), any(DiscoverResponse.class));
    verify(changeResults).discovered(eq(address2), any(DiscoverResponse.class));
    verify(changeResults).discovered(eq(address3), any(DiscoverResponse.class));
    verify(changeResults).endDiscovery();
    verify(changeResults).startSecondDiscovery();
    verify(changeResults).discoverRepeated(address1);
    verify(changeResults).discoverRepeated(address2);
    verify(changeResults).discoverRepeated(address3);
    verify(changeResults).endSecondDiscovery();
    verify(changeResults).startPrepare(any(UUID.class));
    verify(changeResults).prepared(address1);
    verify(changeResults).prepareChangeUnacceptable(address2, "fail");
    verify(changeResults).prepared(address3);
    verify(changeResults).endPrepare();
    verify(changeResults).startRollback();
    verify(changeResults).rolledBack(address1);
    verify(changeResults).rolledBack(address3);
    verify(changeResults).endRollback();
    verify(changeResults).done(CONSISTENT);

    assertEquals(ACCEPTING, serverState1.getMode());
    assertEquals("host", serverState1.getLastMutationHost());
    assertEquals("user", serverState1.getLastMutationUser());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void recovery() throws Exception {
    SimpleNomadChange change = new SimpleNomadChange("change", "summary");

    InterceptionServer<String> interceptionServer = interceptServer(address1);
    interceptionServer.setAllowCommit(false);

    when(changeApplicator1.tryApply(null, change)).thenReturn(allow("changeResult"));
    when(changeApplicator2.tryApply(null, change)).thenReturn(allow("changeResult"));
    when(changeApplicator3.tryApply(null, change)).thenReturn(allow("changeResult"));

    client.tryApplyChange(changeResults, change);

    verify(changeApplicator1).tryApply(null, change);
    verify(changeApplicator2).tryApply(null, change);
    verify(changeApplicator3).tryApply(null, change);
    verifyNoMoreInteractions(changeApplicator1);
    verify(changeApplicator2).apply(change);
    verify(changeApplicator3).apply(change);
    verify(changeResults).startDiscovery(withItems(address1, address2, address3));
    verify(changeResults).discovered(eq(address1), any(DiscoverResponse.class));
    verify(changeResults).discovered(eq(address2), any(DiscoverResponse.class));
    verify(changeResults).discovered(eq(address3), any(DiscoverResponse.class));
    verify(changeResults).endDiscovery();
    verify(changeResults).startSecondDiscovery();
    verify(changeResults).discoverRepeated(address1);
    verify(changeResults).discoverRepeated(address2);
    verify(changeResults).discoverRepeated(address3);
    verify(changeResults).endSecondDiscovery();
    verify(changeResults).startPrepare(any(UUID.class));
    verify(changeResults).prepared(address1);
    verify(changeResults).prepared(address2);
    verify(changeResults).prepared(address3);
    verify(changeResults).endPrepare();
    verify(changeResults).startCommit();
    verify(changeResults).commitFail(eq(address1), any());
    verify(changeResults).committed(address2);
    verify(changeResults).committed(address3);
    verify(changeResults).endCommit();
    verify(changeResults).done(MAY_NEED_RECOVERY);

    interceptionServer.setAllowCommit(true);

    client.tryRecovery(recoveryResults, 3, null);

    verify(changeApplicator1).apply(change);
    verify(recoveryResults).startDiscovery(withItems(address1, address2, address3));
    verify(recoveryResults).discovered(eq(address1), any(DiscoverResponse.class));
    verify(recoveryResults).discovered(eq(address2), any(DiscoverResponse.class));
    verify(recoveryResults).discovered(eq(address3), any(DiscoverResponse.class));
    verify(recoveryResults).endDiscovery();
    verify(recoveryResults).startSecondDiscovery();
    verify(recoveryResults).discoverRepeated(address1);
    verify(recoveryResults).discoverRepeated(address2);
    verify(recoveryResults).discoverRepeated(address3);
    verify(recoveryResults).endSecondDiscovery();
    verify(recoveryResults).startTakeover();
    verify(recoveryResults).takeover(address1);
    verify(recoveryResults).takeover(address2);
    verify(recoveryResults).takeover(address3);
    verify(recoveryResults).endTakeover();
    verify(recoveryResults).startCommit();
    verify(recoveryResults).committed(address1);
    verify(recoveryResults).endCommit();
    verify(recoveryResults).done(CONSISTENT);
  }

  private InterceptionServer<String> interceptServer(InetSocketAddress address) {
    List<NomadEndpoint<String>> serverList = new ArrayList<>(servers);
    servers.clear();

    InterceptionServer<String> interceptionServer = null;
    for (NomadEndpoint<String> server : serverList) {
      if (server.getAddress().equals(address)) {
        interceptionServer = new InterceptionServer<>(server);
        servers.add(new NomadEndpoint<>(address, interceptionServer));
      } else {
        servers.add(server);
      }
    }

    return interceptionServer;
  }

  private static class InterceptionServer<T> implements NomadServer<T> {
    private final NomadServer<T> underlying;
    private volatile boolean allowCommit = true;

    public InterceptionServer(NomadServer<T> underlying) {
      this.underlying = underlying;
    }

    public void setAllowCommit(boolean allowCommit) {
      this.allowCommit = allowCommit;
    }

    @Override
    public DiscoverResponse<T> discover() throws NomadException {
      return underlying.discover();
    }

    @Override
    public AcceptRejectResponse prepare(PrepareMessage message) throws NomadException {
      return underlying.prepare(message);
    }

    @Override
    public AcceptRejectResponse commit(CommitMessage message) throws NomadException {
      if (allowCommit) {
        return underlying.commit(message);
      } else {
        throw new NomadException();
      }
    }

    @Override
    public AcceptRejectResponse rollback(RollbackMessage message) throws NomadException {
      return underlying.rollback(message);
    }

    @Override
    public AcceptRejectResponse takeover(TakeoverMessage message) throws NomadException {
      return underlying.takeover(message);
    }

    @Override
    public void close() {
      underlying.close();
    }
  }
}
