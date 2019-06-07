/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.client.recovery;

import com.terracottatech.nomad.client.Consistency;
import com.terracottatech.nomad.client.results.AllResultsReceiver;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.UUID;

import static com.terracottatech.nomad.client.NomadTestHelper.discovery;
import static com.terracottatech.nomad.client.NomadTestHelper.setOf;
import static com.terracottatech.nomad.server.ChangeRequestState.COMMITTED;
import static com.terracottatech.nomad.server.ChangeRequestState.PREPARED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class RecoveryProcessDeciderTest {
  @Mock
  private AllResultsReceiver results;

  @Test
  public void discoverSuccess() {
    RecoveryProcessDecider decider = new RecoveryProcessDecider();
    decider.setResults(results);

    decider.startDiscovery(setOf("server1", "server2"));
    decider.discovered("server1", discovery(COMMITTED));
    decider.discovered("server2", discovery(COMMITTED));
    decider.endDiscovery();

    assertTrue(decider.isDiscoverSuccessful());
  }

  @Test
  public void discoverFail() {
    RecoveryProcessDecider decider = new RecoveryProcessDecider();
    decider.setResults(results);

    decider.startDiscovery(setOf("server1", "server2"));
    decider.discovered("server1", discovery(COMMITTED));
    decider.discoverFail("server2");
    decider.endDiscovery();

    assertFalse(decider.isDiscoverSuccessful());
    assertEquals(Consistency.UNKNOWN_BUT_NO_CHANGE, decider.getConsistency());
  }

  @Test
  public void discoverPrepared() {
    RecoveryProcessDecider decider = new RecoveryProcessDecider();
    decider.setResults(results);

    decider.startDiscovery(setOf("server1", "server2"));
    decider.discovered("server1", discovery(COMMITTED));
    decider.discovered("server2", discovery(PREPARED));
    decider.endDiscovery();

    assertTrue(decider.isDiscoverSuccessful());
  }

  @Test
  public void secondDiscoverSuccessConsistent() {
    RecoveryProcessDecider decider = new RecoveryProcessDecider();
    decider.setResults(results);

    decider.startDiscovery(setOf("server1", "server2"));
    decider.discovered("server1", discovery(COMMITTED));
    decider.discovered("server2", discovery(COMMITTED));
    decider.endDiscovery();
    decider.startSecondDiscovery();
    decider.discoverRepeated("server1");
    decider.discoverRepeated("server2");
    decider.endSecondDiscovery();

    assertTrue(decider.isDiscoverSuccessful());
    assertTrue(decider.isWholeClusterAccepting());
  }

  @Test
  public void secondDiscoverSuccessRecoveryNeeded() {
    RecoveryProcessDecider decider = new RecoveryProcessDecider();
    decider.setResults(results);

    decider.startDiscovery(setOf("server1", "server2"));
    decider.discovered("server1", discovery(COMMITTED));
    decider.discovered("server2", discovery(PREPARED));
    decider.endDiscovery();
    decider.startSecondDiscovery();
    decider.discoverRepeated("server1");
    decider.discoverRepeated("server2");
    decider.endSecondDiscovery();

    assertTrue(decider.isDiscoverSuccessful());
    assertFalse(decider.isWholeClusterAccepting());
  }

  @Test
  public void secondDiscoverOtherClient() {
    RecoveryProcessDecider decider = new RecoveryProcessDecider();
    decider.setResults(results);

    decider.startDiscovery(setOf("server1", "server2"));
    decider.discovered("server1", discovery(COMMITTED));
    decider.discovered("server2", discovery(COMMITTED));
    decider.endDiscovery();
    decider.startSecondDiscovery();
    decider.discoverRepeated("server1");
    decider.discoverOtherClient("server2", "lastMutationHost", "lastMutationUser");
    decider.endSecondDiscovery();

    assertFalse(decider.isDiscoverSuccessful());
    assertEquals(Consistency.UNKNOWN_BUT_NO_CHANGE, decider.getConsistency());
  }

  @Test
  public void secondDiscoverFail() {
    RecoveryProcessDecider decider = new RecoveryProcessDecider();
    decider.setResults(results);

    decider.startDiscovery(setOf("server1", "server2"));
    decider.discovered("server1", discovery(COMMITTED));
    decider.discovered("server2", discovery(COMMITTED));
    decider.endDiscovery();
    decider.startSecondDiscovery();
    decider.discoverRepeated("server1");
    decider.discoverFail("server2");
    decider.endSecondDiscovery();

    assertFalse(decider.isDiscoverSuccessful());
    assertEquals(Consistency.UNKNOWN_BUT_NO_CHANGE, decider.getConsistency());
  }

  @Test
  public void takeoverSuccessCommit() {
    RecoveryProcessDecider decider = new RecoveryProcessDecider();
    decider.setResults(results);

    UUID uuid = UUID.randomUUID();

    decider.startDiscovery(setOf("server1", "server2"));
    decider.discovered("server1", discovery(COMMITTED, uuid));
    decider.discovered("server2", discovery(PREPARED, uuid));
    decider.endDiscovery();
    decider.startTakeover();
    decider.takeover("server1");
    decider.takeover("server2");
    decider.endPrepare();

    assertTrue(decider.isDiscoverSuccessful());
    assertFalse(decider.isWholeClusterAccepting());
    assertTrue(decider.isTakeoverSuccessful());
    assertTrue(decider.shouldDoCommit());
  }

  @Test
  public void takeoverSuccessRollback() {
    RecoveryProcessDecider decider = new RecoveryProcessDecider();
    decider.setResults(results);

    decider.startDiscovery(setOf("server1", "server2"));
    decider.discovered("server1", discovery(COMMITTED));
    decider.discovered("server2", discovery(PREPARED));
    decider.endDiscovery();
    decider.startTakeover();
    decider.takeover("server1");
    decider.takeover("server2");
    decider.endPrepare();

    assertTrue(decider.isDiscoverSuccessful());
    assertFalse(decider.isWholeClusterAccepting());
    assertTrue(decider.isTakeoverSuccessful());
    assertFalse(decider.shouldDoCommit());
  }

  @Test
  public void takeoverFail() {
    RecoveryProcessDecider decider = new RecoveryProcessDecider();
    decider.setResults(results);

    UUID uuid = UUID.randomUUID();

    decider.startDiscovery(setOf("server1", "server2"));
    decider.discovered("server1", discovery(COMMITTED, uuid));
    decider.discovered("server2", discovery(PREPARED, uuid));
    decider.endDiscovery();
    decider.startTakeover();
    decider.takeover("server1");
    decider.takeoverFail("server2");
    decider.endPrepare();

    assertTrue(decider.isDiscoverSuccessful());
    assertFalse(decider.isWholeClusterAccepting());
    assertFalse(decider.isTakeoverSuccessful());
    assertEquals(Consistency.MAY_NEED_RECOVERY, decider.getConsistency());
  }

  @Test
  public void takeoverOtherClient() {
    RecoveryProcessDecider decider = new RecoveryProcessDecider();
    decider.setResults(results);

    UUID uuid = UUID.randomUUID();

    decider.startDiscovery(setOf("server1", "server2"));
    decider.discovered("server1", discovery(COMMITTED, uuid));
    decider.discovered("server2", discovery(PREPARED, uuid));
    decider.endDiscovery();
    decider.startTakeover();
    decider.takeover("server1");
    decider.takeoverOtherClient("server2", "lastMutationHost", "lastMutationUser");
    decider.endPrepare();

    assertTrue(decider.isDiscoverSuccessful());
    assertFalse(decider.isWholeClusterAccepting());
    assertFalse(decider.isTakeoverSuccessful());
    assertEquals(Consistency.MAY_NEED_RECOVERY, decider.getConsistency());
  }

  @Test
  public void takeoverSuccessCommitSuccess() {
    RecoveryProcessDecider decider = new RecoveryProcessDecider();
    decider.setResults(results);

    UUID uuid = UUID.randomUUID();

    decider.startDiscovery(setOf("server1", "server2"));
    decider.discovered("server1", discovery(COMMITTED, uuid));
    decider.discovered("server2", discovery(PREPARED, uuid));
    decider.endDiscovery();
    decider.startTakeover();
    decider.takeover("server1");
    decider.takeover("server2");
    decider.endPrepare();
    decider.startCommit();
    decider.committed("server2");
    decider.endCommit();

    assertTrue(decider.isDiscoverSuccessful());
    assertFalse(decider.isWholeClusterAccepting());
    assertTrue(decider.isTakeoverSuccessful());
    assertTrue(decider.shouldDoCommit());
    assertEquals(Consistency.CONSISTENT, decider.getConsistency());
  }

  @Test
  public void takeoverSuccessCommitFail() {
    RecoveryProcessDecider decider = new RecoveryProcessDecider();
    decider.setResults(results);

    UUID uuid = UUID.randomUUID();

    decider.startDiscovery(setOf("server1", "server2"));
    decider.discovered("server1", discovery(COMMITTED, uuid));
    decider.discovered("server2", discovery(PREPARED, uuid));
    decider.endDiscovery();
    decider.startTakeover();
    decider.takeover("server1");
    decider.takeover("server2");
    decider.endPrepare();
    decider.startCommit();
    decider.commitFail("server2");
    decider.endCommit();

    assertTrue(decider.isDiscoverSuccessful());
    assertFalse(decider.isWholeClusterAccepting());
    assertTrue(decider.isTakeoverSuccessful());
    assertTrue(decider.shouldDoCommit());
    assertEquals(Consistency.MAY_NEED_RECOVERY, decider.getConsistency());
  }

  @Test
  public void takeoverSuccessCommitOtherClient() {
    RecoveryProcessDecider decider = new RecoveryProcessDecider();
    decider.setResults(results);

    UUID uuid = UUID.randomUUID();

    decider.startDiscovery(setOf("server1", "server2"));
    decider.discovered("server1", discovery(COMMITTED, uuid));
    decider.discovered("server2", discovery(PREPARED, uuid));
    decider.endDiscovery();
    decider.startTakeover();
    decider.takeover("server1");
    decider.takeover("server2");
    decider.endPrepare();
    decider.startCommit();
    decider.commitOtherClient("server2", "lastMutationHost", "lastMutationUser");
    decider.endCommit();

    assertTrue(decider.isDiscoverSuccessful());
    assertFalse(decider.isWholeClusterAccepting());
    assertTrue(decider.isTakeoverSuccessful());
    assertTrue(decider.shouldDoCommit());
    assertEquals(Consistency.MAY_NEED_RECOVERY, decider.getConsistency());
  }

  @Test
  public void takeoverSuccessRollbackSuccess() {
    RecoveryProcessDecider decider = new RecoveryProcessDecider();
    decider.setResults(results);

    decider.startDiscovery(setOf("server1", "server2"));
    decider.discovered("server1", discovery(COMMITTED));
    decider.discovered("server2", discovery(PREPARED));
    decider.endDiscovery();
    decider.startTakeover();
    decider.takeover("server1");
    decider.takeover("server2");
    decider.endPrepare();
    decider.startRollback();
    decider.rolledBack("server2");
    decider.endRollback();

    assertTrue(decider.isDiscoverSuccessful());
    assertFalse(decider.isWholeClusterAccepting());
    assertTrue(decider.isTakeoverSuccessful());
    assertFalse(decider.shouldDoCommit());
    assertEquals(Consistency.CONSISTENT, decider.getConsistency());
  }

  @Test
  public void takeoverSuccessRollbackFail() {
    RecoveryProcessDecider decider = new RecoveryProcessDecider();
    decider.setResults(results);

    decider.startDiscovery(setOf("server1", "server2"));
    decider.discovered("server1", discovery(COMMITTED));
    decider.discovered("server2", discovery(PREPARED));
    decider.endDiscovery();
    decider.startTakeover();
    decider.takeover("server1");
    decider.takeover("server2");
    decider.endPrepare();
    decider.startRollback();
    decider.rollbackFail("server2");
    decider.endRollback();

    assertTrue(decider.isDiscoverSuccessful());
    assertFalse(decider.isWholeClusterAccepting());
    assertTrue(decider.isTakeoverSuccessful());
    assertFalse(decider.shouldDoCommit());
    assertEquals(Consistency.MAY_NEED_RECOVERY, decider.getConsistency());
  }

  @Test
  public void takeoverSuccessRollbackOtherClient() {
    RecoveryProcessDecider decider = new RecoveryProcessDecider();
    decider.setResults(results);

    decider.startDiscovery(setOf("server1", "server2"));
    decider.discovered("server1", discovery(COMMITTED));
    decider.discovered("server2", discovery(PREPARED));
    decider.endDiscovery();
    decider.startTakeover();
    decider.takeover("server1");
    decider.takeover("server2");
    decider.endPrepare();
    decider.startRollback();
    decider.rollbackOtherClient("server2", "lastMutationHost", "lastMutationUser");
    decider.endRollback();

    assertTrue(decider.isDiscoverSuccessful());
    assertFalse(decider.isWholeClusterAccepting());
    assertTrue(decider.isTakeoverSuccessful());
    assertFalse(decider.shouldDoCommit());
    assertEquals(Consistency.MAY_NEED_RECOVERY, decider.getConsistency());
  }
}
