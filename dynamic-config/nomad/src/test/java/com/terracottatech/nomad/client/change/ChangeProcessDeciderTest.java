/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.client.change;

import com.terracottatech.nomad.client.Consistency;
import com.terracottatech.nomad.client.results.AllResultsReceiver;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.InetSocketAddress;
import java.util.UUID;

import static com.terracottatech.nomad.client.NomadTestHelper.discovery;
import static com.terracottatech.nomad.server.ChangeRequestState.COMMITTED;
import static com.terracottatech.nomad.server.ChangeRequestState.PREPARED;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ChangeProcessDeciderTest {
  @Mock
  private AllResultsReceiver<String> results;

  ChangeProcessDecider<String> decider = new ChangeProcessDecider<>();
  InetSocketAddress address1 = InetSocketAddress.createUnresolved("localhost", 9410);
  InetSocketAddress address2 = InetSocketAddress.createUnresolved("localhost", 9411);

  @Test
  public void discoverSuccess() {
    decider.setResults(results);

    decider.startDiscovery(asList(address1, address2));
    decider.discovered(address1, discovery(COMMITTED));
    decider.discovered(address2, discovery(COMMITTED));
    decider.endDiscovery();

    assertTrue(decider.isDiscoverSuccessful());
  }

  @Test
  public void discoverFail() {
    decider.setResults(results);

    decider.startDiscovery(asList(address1, address2));
    decider.discovered(address1, discovery(COMMITTED));
    decider.discoverFail(address2, "reason");
    decider.endDiscovery();

    assertFalse(decider.isDiscoverSuccessful());
    assertEquals(Consistency.UNKNOWN_BUT_NO_CHANGE, decider.getConsistency());
  }

  @Test
  public void discoverPrepared() {
    decider.setResults(results);

    UUID uuid = UUID.randomUUID();

    decider.startDiscovery(asList(address1, address2));
    decider.discovered(address1, discovery(COMMITTED));
    decider.discovered(address2, discovery(PREPARED, uuid));
    decider.endDiscovery();

    assertFalse(decider.isDiscoverSuccessful());

    verify(results).discoverAlreadyPrepared(address2, uuid, "testCreationHost", "testCreationUser");
    assertEquals(Consistency.UNKNOWN_BUT_NO_CHANGE, decider.getConsistency());
  }

  @Test
  public void secondDiscoverSuccess() {
    decider.setResults(results);

    decider.startDiscovery(asList(address1, address2));
    decider.discovered(address1, discovery(COMMITTED));
    decider.discovered(address2, discovery(COMMITTED));
    decider.endDiscovery();
    decider.startSecondDiscovery();
    decider.discoverRepeated(address1);
    decider.discoverRepeated(address2);
    decider.endSecondDiscovery();

    assertTrue(decider.isDiscoverSuccessful());
  }

  @Test
  public void secondDiscoverOtherClient() {
    decider.setResults(results);

    decider.startDiscovery(asList(address1, address2));
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

    decider.startDiscovery(asList(address1, address2));
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
  public void prepareSuccess() {
    decider.setResults(results);

    UUID uuid = UUID.randomUUID();

    decider.startDiscovery(asList(address1, address2));
    decider.discovered(address1, discovery(COMMITTED));
    decider.discovered(address2, discovery(COMMITTED));
    decider.endDiscovery();
    decider.startPrepare(uuid);
    decider.prepared(address1);
    decider.prepared(address2);
    decider.endPrepare();

    assertTrue(decider.isDiscoverSuccessful());
    assertTrue(decider.isPrepareSuccessful());
    assertTrue(decider.shouldDoCommit());
  }

  @Test
  public void prepareSuccessCommitSuccess() {
    decider.setResults(results);

    UUID uuid = UUID.randomUUID();

    decider.startDiscovery(asList(address1, address2));
    decider.discovered(address1, discovery(COMMITTED));
    decider.discovered(address2, discovery(COMMITTED));
    decider.endDiscovery();
    decider.startPrepare(uuid);
    decider.prepared(address1);
    decider.prepared(address2);
    decider.endPrepare();
    decider.startCommit();
    decider.committed(address1);
    decider.committed(address2);
    decider.endCommit();

    assertTrue(decider.isDiscoverSuccessful());
    assertTrue(decider.isPrepareSuccessful());
    assertTrue(decider.shouldDoCommit());
    assertEquals(Consistency.CONSISTENT, decider.getConsistency());
  }

  @Test
  public void prepareSuccessCommitFail() {
    decider.setResults(results);

    UUID uuid = UUID.randomUUID();

    decider.startDiscovery(asList(address1, address2));
    decider.discovered(address1, discovery(COMMITTED));
    decider.discovered(address2, discovery(COMMITTED));
    decider.endDiscovery();
    decider.startPrepare(uuid);
    decider.prepared(address1);
    decider.prepared(address2);
    decider.endPrepare();
    decider.startCommit();
    decider.committed(address1);
    decider.commitFail(address2, "reason");
    decider.endCommit();

    assertTrue(decider.isDiscoverSuccessful());
    assertTrue(decider.isPrepareSuccessful());
    assertTrue(decider.shouldDoCommit());
    assertEquals(Consistency.MAY_NEED_RECOVERY, decider.getConsistency());
  }

  @Test
  public void prepareSuccessCommitOtherClient() {
    decider.setResults(results);

    UUID uuid = UUID.randomUUID();

    decider.startDiscovery(asList(address1, address2));
    decider.discovered(address1, discovery(COMMITTED));
    decider.discovered(address2, discovery(COMMITTED));
    decider.endDiscovery();
    decider.startPrepare(uuid);
    decider.prepared(address1);
    decider.prepared(address2);
    decider.endPrepare();
    decider.startCommit();
    decider.committed(address1);
    decider.commitOtherClient(address2, "lastMutationHost", "lastMutationUser");
    decider.endCommit();

    assertTrue(decider.isDiscoverSuccessful());
    assertTrue(decider.isPrepareSuccessful());
    assertTrue(decider.shouldDoCommit());
    assertEquals(Consistency.MAY_NEED_RECOVERY, decider.getConsistency());
  }

  @Test
  public void prepareFail() {
    decider.setResults(results);

    UUID uuid = UUID.randomUUID();

    decider.startDiscovery(asList(address1, address2));
    decider.discovered(address1, discovery(COMMITTED));
    decider.discovered(address2, discovery(COMMITTED));
    decider.endDiscovery();
    decider.startPrepare(uuid);
    decider.prepared(address1);
    decider.prepareFail(address2, "reason");
    decider.endPrepare();

    assertTrue(decider.isDiscoverSuccessful());
    assertFalse(decider.isPrepareSuccessful());
    assertFalse(decider.shouldDoCommit());
  }

  @Test
  public void prepareFailRollbackSuccess() {
    decider.setResults(results);

    UUID uuid = UUID.randomUUID();

    decider.startDiscovery(asList(address1, address2));
    decider.discovered(address1, discovery(COMMITTED));
    decider.discovered(address2, discovery(COMMITTED));
    decider.endDiscovery();
    decider.startPrepare(uuid);
    decider.prepared(address1);
    decider.prepareFail(address2, "reason");
    decider.endPrepare();
    decider.startRollback();
    decider.rolledBack(address2);
    decider.endRollback();

    assertTrue(decider.isDiscoverSuccessful());
    assertFalse(decider.isPrepareSuccessful());
    assertFalse(decider.shouldDoCommit());
    assertEquals(Consistency.CONSISTENT, decider.getConsistency());
  }

  @Test
  public void prepareFailRollbackFail() {
    decider.setResults(results);

    UUID uuid = UUID.randomUUID();

    decider.startDiscovery(asList(address1, address2));
    decider.discovered(address1, discovery(COMMITTED));
    decider.discovered(address2, discovery(COMMITTED));
    decider.endDiscovery();
    decider.startPrepare(uuid);
    decider.prepared(address1);
    decider.prepareFail(address2, "reason");
    decider.endPrepare();
    decider.startRollback();
    decider.rollbackFail(address2, "reason");
    decider.endRollback();

    assertTrue(decider.isDiscoverSuccessful());
    assertFalse(decider.isPrepareSuccessful());
    assertFalse(decider.shouldDoCommit());
    assertEquals(Consistency.MAY_NEED_RECOVERY, decider.getConsistency());
  }

  @Test
  public void prepareFailRollbackOtherClient() {
    decider.setResults(results);

    UUID uuid = UUID.randomUUID();

    decider.startDiscovery(asList(address1, address2));
    decider.discovered(address1, discovery(COMMITTED));
    decider.discovered(address2, discovery(COMMITTED));
    decider.endDiscovery();
    decider.startPrepare(uuid);
    decider.prepared(address1);
    decider.prepareFail(address2, "reason");
    decider.endPrepare();
    decider.startRollback();
    decider.rollbackOtherClient(address2, "lastMutationHost", "lastMutationUser");
    decider.endRollback();

    assertTrue(decider.isDiscoverSuccessful());
    assertFalse(decider.isPrepareSuccessful());
    assertFalse(decider.shouldDoCommit());
    assertEquals(Consistency.MAY_NEED_RECOVERY, decider.getConsistency());
  }

  @Test
  public void prepareOtherClient() {
    decider.setResults(results);

    UUID uuid = UUID.randomUUID();

    decider.startDiscovery(asList(address1, address2));
    decider.discovered(address1, discovery(COMMITTED));
    decider.discovered(address2, discovery(COMMITTED));
    decider.endDiscovery();
    decider.startPrepare(uuid);
    decider.prepared(address1);
    decider.prepareOtherClient(address2, "lastMutationHost", "lastMutationUser");
    decider.endPrepare();

    assertTrue(decider.isDiscoverSuccessful());
    assertFalse(decider.isPrepareSuccessful());
    assertFalse(decider.shouldDoCommit());
  }
}
