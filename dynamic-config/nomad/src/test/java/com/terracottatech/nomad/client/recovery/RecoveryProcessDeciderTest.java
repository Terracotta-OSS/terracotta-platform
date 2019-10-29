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

import java.net.InetSocketAddress;
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
  private AllResultsReceiver<String> results;

  RecoveryProcessDecider<String> decider = new RecoveryProcessDecider<>();
  InetSocketAddress address1 = InetSocketAddress.createUnresolved("localhost", 9410);
  InetSocketAddress address2 = InetSocketAddress.createUnresolved("localhost", 9411);

  @Test
  public void discoverSuccess() {
    decider.setResults(results);

    decider.startDiscovery(setOf(address1, address2));
    decider.discovered(address1, discovery(COMMITTED));
    decider.discovered(address2, discovery(COMMITTED));
    decider.endDiscovery();

    assertTrue(decider.isDiscoverSuccessful());
  }

  @Test
  public void discoverFail() {
    decider.setResults(results);

    decider.startDiscovery(setOf(address1, address2));
    decider.discovered(address1, discovery(COMMITTED));
    decider.discoverFail(address2, "reason");
    decider.endDiscovery();

    assertFalse(decider.isDiscoverSuccessful());
    assertEquals(Consistency.UNKNOWN_BUT_NO_CHANGE, decider.getConsistency());
  }

  @Test
  public void discoverPrepared() {
    decider.setResults(results);

    decider.startDiscovery(setOf(address1, address2));
    decider.discovered(address1, discovery(COMMITTED));
    decider.discovered(address2, discovery(PREPARED));
    decider.endDiscovery();

    assertTrue(decider.isDiscoverSuccessful());
  }

  @Test
  public void secondDiscoverSuccessConsistent() {
    decider.setResults(results);

    decider.startDiscovery(setOf(address1, address2));
    decider.discovered(address1, discovery(COMMITTED));
    decider.discovered(address2, discovery(COMMITTED));
    decider.endDiscovery();
    decider.startSecondDiscovery();
    decider.discoverRepeated(address1);
    decider.discoverRepeated(address2);
    decider.endSecondDiscovery();

    assertTrue(decider.isDiscoverSuccessful());
    assertTrue(decider.isWholeClusterAccepting());
  }

  @Test
  public void secondDiscoverSuccessRecoveryNeeded() {
    decider.setResults(results);

    decider.startDiscovery(setOf(address1, address2));
    decider.discovered(address1, discovery(COMMITTED));
    decider.discovered(address2, discovery(PREPARED));
    decider.endDiscovery();
    decider.startSecondDiscovery();
    decider.discoverRepeated(address1);
    decider.discoverRepeated(address2);
    decider.endSecondDiscovery();

    assertTrue(decider.isDiscoverSuccessful());
    assertFalse(decider.isWholeClusterAccepting());
  }

  @Test
  public void secondDiscoverOtherClient() {

    decider.setResults(results);

    decider.startDiscovery(setOf(address1, address2));
    decider.discovered(address1, discovery(COMMITTED));
    decider.discovered(address2, discovery(COMMITTED));
    decider.endDiscovery();
    decider.startSecondDiscovery();
    decider.discoverRepeated(address1);
    decider.discoverOtherClient(address2, "lastMutationHost", "lastMutationUser");
    decider.endSecondDiscovery();

    assertFalse(decider.isDiscoverSuccessful());
    assertEquals(Consistency.UNKNOWN_BUT_NO_CHANGE, decider.getConsistency());
  }

  @Test
  public void secondDiscoverFail() {
    decider.setResults(results);

    decider.startDiscovery(setOf(address1, address2));
    decider.discovered(address1, discovery(COMMITTED));
    decider.discovered(address2, discovery(COMMITTED));
    decider.endDiscovery();
    decider.startSecondDiscovery();
    decider.discoverRepeated(address1);
    decider.discoverFail(address2, "reason");
    decider.endSecondDiscovery();

    assertFalse(decider.isDiscoverSuccessful());
    assertEquals(Consistency.UNKNOWN_BUT_NO_CHANGE, decider.getConsistency());
  }

  @Test
  public void takeoverSuccessCommit() {
    decider.setResults(results);

    UUID uuid = UUID.randomUUID();

    decider.startDiscovery(setOf(address1, address2));
    decider.discovered(address1, discovery(COMMITTED, uuid));
    decider.discovered(address2, discovery(PREPARED, uuid));
    decider.endDiscovery();
    decider.startTakeover();
    decider.takeover(address1);
    decider.takeover(address2);
    decider.endPrepare();

    assertTrue(decider.isDiscoverSuccessful());
    assertFalse(decider.isWholeClusterAccepting());
    assertTrue(decider.isTakeoverSuccessful());
    assertTrue(decider.shouldDoCommit());
  }

  @Test
  public void takeoverSuccessRollback() {
    decider.setResults(results);

    decider.startDiscovery(setOf(address1, address2));
    decider.discovered(address1, discovery(COMMITTED));
    decider.discovered(address2, discovery(PREPARED));
    decider.endDiscovery();
    decider.startTakeover();
    decider.takeover(address1);
    decider.takeover(address2);
    decider.endPrepare();

    assertTrue(decider.isDiscoverSuccessful());
    assertFalse(decider.isWholeClusterAccepting());
    assertTrue(decider.isTakeoverSuccessful());
    assertFalse(decider.shouldDoCommit());
  }

  @Test
  public void takeoverFail() {
    decider.setResults(results);

    UUID uuid = UUID.randomUUID();

    decider.startDiscovery(setOf(address1, address2));
    decider.discovered(address1, discovery(COMMITTED, uuid));
    decider.discovered(address2, discovery(PREPARED, uuid));
    decider.endDiscovery();
    decider.startTakeover();
    decider.takeover(address1);
    decider.takeoverFail(address2, "reason");
    decider.endPrepare();

    assertTrue(decider.isDiscoverSuccessful());
    assertFalse(decider.isWholeClusterAccepting());
    assertFalse(decider.isTakeoverSuccessful());
    assertEquals(Consistency.MAY_NEED_RECOVERY, decider.getConsistency());
  }

  @Test
  public void takeoverOtherClient() {
    decider.setResults(results);

    UUID uuid = UUID.randomUUID();

    decider.startDiscovery(setOf(address1, address2));
    decider.discovered(address1, discovery(COMMITTED, uuid));
    decider.discovered(address2, discovery(PREPARED, uuid));
    decider.endDiscovery();
    decider.startTakeover();
    decider.takeover(address1);
    decider.takeoverOtherClient(address2, "lastMutationHost", "lastMutationUser");
    decider.endPrepare();

    assertTrue(decider.isDiscoverSuccessful());
    assertFalse(decider.isWholeClusterAccepting());
    assertFalse(decider.isTakeoverSuccessful());
    assertEquals(Consistency.MAY_NEED_RECOVERY, decider.getConsistency());
  }

  @Test
  public void takeoverSuccessCommitSuccess() {
    decider.setResults(results);

    UUID uuid = UUID.randomUUID();

    decider.startDiscovery(setOf(address1, address2));
    decider.discovered(address1, discovery(COMMITTED, uuid));
    decider.discovered(address2, discovery(PREPARED, uuid));
    decider.endDiscovery();
    decider.startTakeover();
    decider.takeover(address1);
    decider.takeover(address2);
    decider.endPrepare();
    decider.startCommit();
    decider.committed(address2);
    decider.endCommit();

    assertTrue(decider.isDiscoverSuccessful());
    assertFalse(decider.isWholeClusterAccepting());
    assertTrue(decider.isTakeoverSuccessful());
    assertTrue(decider.shouldDoCommit());
    assertEquals(Consistency.CONSISTENT, decider.getConsistency());
  }

  @Test
  public void takeoverSuccessCommitFail() {
    decider.setResults(results);

    UUID uuid = UUID.randomUUID();

    decider.startDiscovery(setOf(address1, address2));
    decider.discovered(address1, discovery(COMMITTED, uuid));
    decider.discovered(address2, discovery(PREPARED, uuid));
    decider.endDiscovery();
    decider.startTakeover();
    decider.takeover(address1);
    decider.takeover(address2);
    decider.endPrepare();
    decider.startCommit();
    decider.commitFail(address2, "reason");
    decider.endCommit();

    assertTrue(decider.isDiscoverSuccessful());
    assertFalse(decider.isWholeClusterAccepting());
    assertTrue(decider.isTakeoverSuccessful());
    assertTrue(decider.shouldDoCommit());
    assertEquals(Consistency.MAY_NEED_RECOVERY, decider.getConsistency());
  }

  @Test
  public void takeoverSuccessCommitOtherClient() {
    decider.setResults(results);

    UUID uuid = UUID.randomUUID();

    decider.startDiscovery(setOf(address1, address2));
    decider.discovered(address1, discovery(COMMITTED, uuid));
    decider.discovered(address2, discovery(PREPARED, uuid));
    decider.endDiscovery();
    decider.startTakeover();
    decider.takeover(address1);
    decider.takeover(address2);
    decider.endPrepare();
    decider.startCommit();
    decider.commitOtherClient(address2, "lastMutationHost", "lastMutationUser");
    decider.endCommit();

    assertTrue(decider.isDiscoverSuccessful());
    assertFalse(decider.isWholeClusterAccepting());
    assertTrue(decider.isTakeoverSuccessful());
    assertTrue(decider.shouldDoCommit());
    assertEquals(Consistency.MAY_NEED_RECOVERY, decider.getConsistency());
  }

  @Test
  public void takeoverSuccessRollbackSuccess() {
    decider.setResults(results);

    decider.startDiscovery(setOf(address1, address2));
    decider.discovered(address1, discovery(COMMITTED));
    decider.discovered(address2, discovery(PREPARED));
    decider.endDiscovery();
    decider.startTakeover();
    decider.takeover(address1);
    decider.takeover(address2);
    decider.endPrepare();
    decider.startRollback();
    decider.rolledBack(address2);
    decider.endRollback();

    assertTrue(decider.isDiscoverSuccessful());
    assertFalse(decider.isWholeClusterAccepting());
    assertTrue(decider.isTakeoverSuccessful());
    assertFalse(decider.shouldDoCommit());
    assertEquals(Consistency.CONSISTENT, decider.getConsistency());
  }

  @Test
  public void takeoverSuccessRollbackFail() {
    decider.setResults(results);

    decider.startDiscovery(setOf(address1, address2));
    decider.discovered(address1, discovery(COMMITTED));
    decider.discovered(address2, discovery(PREPARED));
    decider.endDiscovery();
    decider.startTakeover();
    decider.takeover(address1);
    decider.takeover(address2);
    decider.endPrepare();
    decider.startRollback();
    decider.rollbackFail(address2, "reason");
    decider.endRollback();

    assertTrue(decider.isDiscoverSuccessful());
    assertFalse(decider.isWholeClusterAccepting());
    assertTrue(decider.isTakeoverSuccessful());
    assertFalse(decider.shouldDoCommit());
    assertEquals(Consistency.MAY_NEED_RECOVERY, decider.getConsistency());
  }

  @Test
  public void takeoverSuccessRollbackOtherClient() {
    decider.setResults(results);

    decider.startDiscovery(setOf(address1, address2));
    decider.discovered(address1, discovery(COMMITTED));
    decider.discovered(address2, discovery(PREPARED));
    decider.endDiscovery();
    decider.startTakeover();
    decider.takeover(address1);
    decider.takeover(address2);
    decider.endPrepare();
    decider.startRollback();
    decider.rollbackOtherClient(address2, "lastMutationHost", "lastMutationUser");
    decider.endRollback();

    assertTrue(decider.isDiscoverSuccessful());
    assertFalse(decider.isWholeClusterAccepting());
    assertTrue(decider.isTakeoverSuccessful());
    assertFalse(decider.shouldDoCommit());
    assertEquals(Consistency.MAY_NEED_RECOVERY, decider.getConsistency());
  }
}
