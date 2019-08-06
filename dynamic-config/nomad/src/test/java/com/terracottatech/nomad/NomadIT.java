/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad;

import com.terracottatech.nomad.client.NamedNomadServer;
import com.terracottatech.nomad.client.NomadClient;
import com.terracottatech.nomad.client.change.ChangeResultReceiver;
import com.terracottatech.nomad.client.change.SimpleNomadChange;
import com.terracottatech.nomad.client.recovery.RecoveryResultReceiver;
import com.terracottatech.nomad.messages.AcceptRejectResponse;
import com.terracottatech.nomad.messages.CommitMessage;
import com.terracottatech.nomad.messages.DiscoverResponse;
import com.terracottatech.nomad.messages.PrepareMessage;
import com.terracottatech.nomad.messages.RollbackMessage;
import com.terracottatech.nomad.messages.TakeoverMessage;
import com.terracottatech.nomad.server.ChangeApplicator;
import com.terracottatech.nomad.server.NomadException;
import com.terracottatech.nomad.server.NomadServer;
import com.terracottatech.nomad.server.NomadServerImpl;
import com.terracottatech.nomad.server.PotentialApplicationResult;
import com.terracottatech.nomad.server.state.MemoryNomadServerState;
import com.terracottatech.nomad.server.state.NomadServerState;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.terracottatech.nomad.client.Consistency.CONSISTENT;
import static com.terracottatech.nomad.client.Consistency.MAY_NEED_RECOVERY;
import static com.terracottatech.nomad.client.NomadTestHelper.matchSetOf;
import static com.terracottatech.nomad.client.NomadTestHelper.setOf;
import static com.terracottatech.nomad.server.NomadServerMode.ACCEPTING;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

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
  private Set<NamedNomadServer<String>> servers;
  private NomadClient<String> client;
  private boolean assertNoMoreInteractions = true;

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

    NamedNomadServer<String> server1 = new NamedNomadServer<>("server1", serverImpl1);
    NamedNomadServer<String> server2 = new NamedNomadServer<>("server2", serverImpl2);
    NamedNomadServer<String> server3 = new NamedNomadServer<>("server3", serverImpl3);

    servers = setOf(server1, server2, server3);
    client = new NomadClient<>(servers, "host", "user");
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
    when(changeApplicator1.tryApply(null, new SimpleNomadChange("change", "summary"))).thenReturn(PotentialApplicationResult.allow("changeResult"));
    when(changeApplicator2.tryApply(null, new SimpleNomadChange("change", "summary"))).thenReturn(PotentialApplicationResult.allow("changeResult"));
    when(changeApplicator3.tryApply(null, new SimpleNomadChange("change", "summary"))).thenReturn(PotentialApplicationResult.allow("changeResult"));

    client.tryApplyChange(changeResults, new SimpleNomadChange("change", "summary"));

    verify(changeApplicator1).tryApply(null, new SimpleNomadChange("change", "summary"));
    verify(changeApplicator2).tryApply(null, new SimpleNomadChange("change", "summary"));
    verify(changeApplicator3).tryApply(null, new SimpleNomadChange("change", "summary"));
    verify(changeApplicator1).apply(new SimpleNomadChange("change", "summary"));
    verify(changeApplicator2).apply(new SimpleNomadChange("change", "summary"));
    verify(changeApplicator3).apply(new SimpleNomadChange("change", "summary"));
    verify(changeResults).startDiscovery(matchSetOf("server1", "server2", "server3"));
    verify(changeResults).discovered(eq("server1"), any(DiscoverResponse.class));
    verify(changeResults).discovered(eq("server2"), any(DiscoverResponse.class));
    verify(changeResults).discovered(eq("server3"), any(DiscoverResponse.class));
    verify(changeResults).endDiscovery();
    verify(changeResults).startSecondDiscovery();
    verify(changeResults).discoverRepeated("server1");
    verify(changeResults).discoverRepeated("server2");
    verify(changeResults).discoverRepeated("server3");
    verify(changeResults).endSecondDiscovery();
    verify(changeResults).startPrepare(any(UUID.class));
    verify(changeResults).prepared("server1");
    verify(changeResults).prepared("server2");
    verify(changeResults).prepared("server3");
    verify(changeResults).endPrepare();
    verify(changeResults).startCommit();
    verify(changeResults).committed("server1");
    verify(changeResults).committed("server2");
    verify(changeResults).committed("server3");
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
    when(changeApplicator1.tryApply(null, new SimpleNomadChange("change1", "summary1"))).thenReturn(PotentialApplicationResult.allow("changeResult1"));
    when(changeApplicator1.tryApply("changeResult1", new SimpleNomadChange("change2", "summary2"))).thenReturn(PotentialApplicationResult.allow("changeResult2"));
    when(changeApplicator2.tryApply(null, new SimpleNomadChange("change1", "summary1"))).thenReturn(PotentialApplicationResult.allow("changeResult1"));
    when(changeApplicator2.tryApply("changeResult1", new SimpleNomadChange("change2", "summary2"))).thenReturn(PotentialApplicationResult.allow("changeResult2"));
    when(changeApplicator3.tryApply(null, new SimpleNomadChange("change1", "summary1"))).thenReturn(PotentialApplicationResult.allow("changeResult1"));
    when(changeApplicator3.tryApply("changeResult1", new SimpleNomadChange("change2", "summary2"))).thenReturn(PotentialApplicationResult.allow("changeResult2"));

    client.tryApplyChange(changeResults, new SimpleNomadChange("change1", "summary1"));
    client.tryApplyChange(changeResults, new SimpleNomadChange("change2", "summary2"));

    verify(changeApplicator1).apply(new SimpleNomadChange("change1", "summary1"));
    verify(changeApplicator1).apply(new SimpleNomadChange("change2", "summary2"));
    verify(changeApplicator2).apply(new SimpleNomadChange("change1", "summary1"));
    verify(changeApplicator2).apply(new SimpleNomadChange("change2", "summary2"));
    verify(changeApplicator3).apply(new SimpleNomadChange("change1", "summary1"));
    verify(changeApplicator3).apply(new SimpleNomadChange("change2", "summary2"));
    verify(changeResults, times(2)).committed("server1");
    verify(changeResults, times(2)).committed("server2");
    verify(changeResults, times(2)).committed("server3");
    verify(changeResults, times(2)).done(CONSISTENT);

    assertEquals(2L, serverState1.getCurrentVersion());

    assertNoMoreInteractions = false;
  }

  @SuppressWarnings("unchecked")
  @Test
  public void rejectChange() {
    when(changeApplicator1.tryApply(null, new SimpleNomadChange("change", "summary"))).thenReturn(PotentialApplicationResult.allow("changeResult"));
    when(changeApplicator2.tryApply(null, new SimpleNomadChange("change", "summary"))).thenReturn(PotentialApplicationResult.reject("fail"));
    when(changeApplicator3.tryApply(null, new SimpleNomadChange("change", "summary"))).thenReturn(PotentialApplicationResult.allow("changeResult"));

    client.tryApplyChange(changeResults, new SimpleNomadChange("change", "summary"));

    verify(changeApplicator1).tryApply(null, new SimpleNomadChange("change", "summary"));
    verify(changeApplicator2).tryApply(null, new SimpleNomadChange("change", "summary"));
    verify(changeApplicator3).tryApply(null, new SimpleNomadChange("change", "summary"));
    verify(changeResults).startDiscovery(matchSetOf("server1", "server2", "server3"));
    verify(changeResults).discovered(eq("server1"), any(DiscoverResponse.class));
    verify(changeResults).discovered(eq("server2"), any(DiscoverResponse.class));
    verify(changeResults).discovered(eq("server3"), any(DiscoverResponse.class));
    verify(changeResults).endDiscovery();
    verify(changeResults).startSecondDiscovery();
    verify(changeResults).discoverRepeated("server1");
    verify(changeResults).discoverRepeated("server2");
    verify(changeResults).discoverRepeated("server3");
    verify(changeResults).endSecondDiscovery();
    verify(changeResults).startPrepare(any(UUID.class));
    verify(changeResults).prepared("server1");
    verify(changeResults).prepareChangeUnacceptable("server2", "fail");
    verify(changeResults).prepared("server3");
    verify(changeResults).endPrepare();
    verify(changeResults).startRollback();
    verify(changeResults).rolledBack("server1");
    verify(changeResults).rolledBack("server3");
    verify(changeResults).endRollback();
    verify(changeResults).done(CONSISTENT);

    assertEquals(ACCEPTING, serverState1.getMode());
    assertEquals("host", serverState1.getLastMutationHost());
    assertEquals("user", serverState1.getLastMutationUser());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void recovery() throws Exception {
    InterceptionServer<String> interceptionServer = interceptServer("server1");
    interceptionServer.setAllowCommit(false);

    when(changeApplicator1.tryApply(null, new SimpleNomadChange("change", "summary"))).thenReturn(PotentialApplicationResult.allow("changeResult"));
    when(changeApplicator2.tryApply(null, new SimpleNomadChange("change", "summary"))).thenReturn(PotentialApplicationResult.allow("changeResult"));
    when(changeApplicator3.tryApply(null, new SimpleNomadChange("change", "summary"))).thenReturn(PotentialApplicationResult.allow("changeResult"));

    client.tryApplyChange(changeResults, new SimpleNomadChange("change", "summary"));

    verify(changeApplicator1).tryApply(null, new SimpleNomadChange("change", "summary"));
    verify(changeApplicator2).tryApply(null, new SimpleNomadChange("change", "summary"));
    verify(changeApplicator3).tryApply(null, new SimpleNomadChange("change", "summary"));
    verifyNoMoreInteractions(changeApplicator1);
    verify(changeApplicator2).apply(new SimpleNomadChange("change", "summary"));
    verify(changeApplicator3).apply(new SimpleNomadChange("change", "summary"));
    verify(changeResults).startDiscovery(matchSetOf("server1", "server2", "server3"));
    verify(changeResults).discovered(eq("server1"), any(DiscoverResponse.class));
    verify(changeResults).discovered(eq("server2"), any(DiscoverResponse.class));
    verify(changeResults).discovered(eq("server3"), any(DiscoverResponse.class));
    verify(changeResults).endDiscovery();
    verify(changeResults).startSecondDiscovery();
    verify(changeResults).discoverRepeated("server1");
    verify(changeResults).discoverRepeated("server2");
    verify(changeResults).discoverRepeated("server3");
    verify(changeResults).endSecondDiscovery();
    verify(changeResults).startPrepare(any(UUID.class));
    verify(changeResults).prepared("server1");
    verify(changeResults).prepared("server2");
    verify(changeResults).prepared("server3");
    verify(changeResults).endPrepare();
    verify(changeResults).startCommit();
    verify(changeResults).commitFail("server1");
    verify(changeResults).committed("server2");
    verify(changeResults).committed("server3");
    verify(changeResults).endCommit();
    verify(changeResults).done(MAY_NEED_RECOVERY);

    interceptionServer.setAllowCommit(true);

    client.tryRecovery(recoveryResults);

    verify(changeApplicator1).apply(new SimpleNomadChange("change", "summary"));
    verify(recoveryResults).startDiscovery(matchSetOf("server1", "server2", "server3"));
    verify(recoveryResults).discovered(eq("server1"), any(DiscoverResponse.class));
    verify(recoveryResults).discovered(eq("server2"), any(DiscoverResponse.class));
    verify(recoveryResults).discovered(eq("server3"), any(DiscoverResponse.class));
    verify(recoveryResults).endDiscovery();
    verify(recoveryResults).startSecondDiscovery();
    verify(recoveryResults).discoverRepeated("server1");
    verify(recoveryResults).discoverRepeated("server2");
    verify(recoveryResults).discoverRepeated("server3");
    verify(recoveryResults).endSecondDiscovery();
    verify(recoveryResults).startTakeover();
    verify(recoveryResults).takeover("server1");
    verify(recoveryResults).takeover("server2");
    verify(recoveryResults).takeover("server3");
    verify(recoveryResults).endTakeover();
    verify(recoveryResults).startCommit();
    verify(recoveryResults).committed("server1");
    verify(recoveryResults).endCommit();
    verify(recoveryResults).done(CONSISTENT);
  }

  private InterceptionServer<String> interceptServer(String serverName) {
    List<NamedNomadServer<String>> serverList = new ArrayList<>(servers);
    servers.clear();

    InterceptionServer<String> interceptionServer = null;
    for (NamedNomadServer<String> server : serverList) {
      if (server.getName().equals(serverName)) {
        interceptionServer = new InterceptionServer<>(server);
        servers.add(new NamedNomadServer<>(serverName, interceptionServer));
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
  }
}
