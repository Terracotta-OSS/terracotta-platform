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

import java.util.UUID;

import static com.terracottatech.nomad.client.NomadTestHelper.discovery;
import static com.terracottatech.nomad.client.NomadTestHelper.setOf;
import static com.terracottatech.nomad.server.ChangeRequestState.COMMITTED;
import static com.terracottatech.nomad.server.ChangeRequestState.PREPARED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ChangeProcessDeciderTest {
  @Mock
  private AllResultsReceiver results;

  @Test
  public void discoverSuccess() {
    ChangeProcessDecider decider = new ChangeProcessDecider();
    decider.setResults(results);

    decider.startDiscovery(setOf("server1", "server2"));
    decider.discovered("server1", discovery(COMMITTED));
    decider.discovered("server2", discovery(COMMITTED));
    decider.endDiscovery();

    assertTrue(decider.isDiscoverSuccessful());
  }

  @Test
  public void discoverFail() {
    ChangeProcessDecider decider = new ChangeProcessDecider();
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
    ChangeProcessDecider decider = new ChangeProcessDecider();
    decider.setResults(results);

    UUID uuid = UUID.randomUUID();

    decider.startDiscovery(setOf("server1", "server2"));
    decider.discovered("server1", discovery(COMMITTED));
    decider.discovered("server2", discovery(PREPARED, uuid));
    decider.endDiscovery();

    assertFalse(decider.isDiscoverSuccessful());

    verify(results).discoverAlreadyPrepared("server2", uuid, "testCreationHost", "testCreationUser");
    assertEquals(Consistency.UNKNOWN_BUT_NO_CHANGE, decider.getConsistency());
  }

  @Test
  public void secondDiscoverSuccess() {
    ChangeProcessDecider decider = new ChangeProcessDecider();
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
  }

  @Test
  public void secondDiscoverOtherClient() {
    ChangeProcessDecider decider = new ChangeProcessDecider();
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
    ChangeProcessDecider decider = new ChangeProcessDecider();
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
  public void prepareSuccess() {
    ChangeProcessDecider decider = new ChangeProcessDecider();
    decider.setResults(results);

    UUID uuid = UUID.randomUUID();

    decider.startDiscovery(setOf("server1", "server2"));
    decider.discovered("server1", discovery(COMMITTED));
    decider.discovered("server2", discovery(COMMITTED));
    decider.endDiscovery();
    decider.startPrepare(uuid);
    decider.prepared("server1");
    decider.prepared("server2");
    decider.endPrepare();

    assertTrue(decider.isDiscoverSuccessful());
    assertTrue(decider.isPrepareSuccessful());
    assertTrue(decider.shouldDoCommit());
  }

  @Test
  public void prepareSuccessCommitSuccess() {
    ChangeProcessDecider decider = new ChangeProcessDecider();
    decider.setResults(results);

    UUID uuid = UUID.randomUUID();

    decider.startDiscovery(setOf("server1", "server2"));
    decider.discovered("server1", discovery(COMMITTED));
    decider.discovered("server2", discovery(COMMITTED));
    decider.endDiscovery();
    decider.startPrepare(uuid);
    decider.prepared("server1");
    decider.prepared("server2");
    decider.endPrepare();
    decider.startCommit();
    decider.committed("server1");
    decider.committed("server2");
    decider.endCommit();

    assertTrue(decider.isDiscoverSuccessful());
    assertTrue(decider.isPrepareSuccessful());
    assertTrue(decider.shouldDoCommit());
    assertEquals(Consistency.CONSISTENT, decider.getConsistency());
  }

  @Test
  public void prepareSuccessCommitFail() {
    ChangeProcessDecider decider = new ChangeProcessDecider();
    decider.setResults(results);

    UUID uuid = UUID.randomUUID();

    decider.startDiscovery(setOf("server1", "server2"));
    decider.discovered("server1", discovery(COMMITTED));
    decider.discovered("server2", discovery(COMMITTED));
    decider.endDiscovery();
    decider.startPrepare(uuid);
    decider.prepared("server1");
    decider.prepared("server2");
    decider.endPrepare();
    decider.startCommit();
    decider.committed("server1");
    decider.commitFail("server2");
    decider.endCommit();

    assertTrue(decider.isDiscoverSuccessful());
    assertTrue(decider.isPrepareSuccessful());
    assertTrue(decider.shouldDoCommit());
    assertEquals(Consistency.MAY_NEED_RECOVERY, decider.getConsistency());
  }

  @Test
  public void prepareSuccessCommitOtherClient() {
    ChangeProcessDecider decider = new ChangeProcessDecider();
    decider.setResults(results);

    UUID uuid = UUID.randomUUID();

    decider.startDiscovery(setOf("server1", "server2"));
    decider.discovered("server1", discovery(COMMITTED));
    decider.discovered("server2", discovery(COMMITTED));
    decider.endDiscovery();
    decider.startPrepare(uuid);
    decider.prepared("server1");
    decider.prepared("server2");
    decider.endPrepare();
    decider.startCommit();
    decider.committed("server1");
    decider.commitOtherClient("server2", "lastMutationHost", "lastMutationUser");
    decider.endCommit();

    assertTrue(decider.isDiscoverSuccessful());
    assertTrue(decider.isPrepareSuccessful());
    assertTrue(decider.shouldDoCommit());
    assertEquals(Consistency.MAY_NEED_RECOVERY, decider.getConsistency());
  }

  @Test
  public void prepareFail() {
    ChangeProcessDecider decider = new ChangeProcessDecider();
    decider.setResults(results);

    UUID uuid = UUID.randomUUID();

    decider.startDiscovery(setOf("server1", "server2"));
    decider.discovered("server1", discovery(COMMITTED));
    decider.discovered("server2", discovery(COMMITTED));
    decider.endDiscovery();
    decider.startPrepare(uuid);
    decider.prepared("server1");
    decider.prepareFail("server2");
    decider.endPrepare();

    assertTrue(decider.isDiscoverSuccessful());
    assertFalse(decider.isPrepareSuccessful());
    assertFalse(decider.shouldDoCommit());
  }

  @Test
  public void prepareFailRollbackSuccess() {
    ChangeProcessDecider decider = new ChangeProcessDecider();
    decider.setResults(results);

    UUID uuid = UUID.randomUUID();

    decider.startDiscovery(setOf("server1", "server2"));
    decider.discovered("server1", discovery(COMMITTED));
    decider.discovered("server2", discovery(COMMITTED));
    decider.endDiscovery();
    decider.startPrepare(uuid);
    decider.prepared("server1");
    decider.prepareFail("server2");
    decider.endPrepare();
    decider.startRollback();
    decider.rolledBack("server2");
    decider.endRollback();

    assertTrue(decider.isDiscoverSuccessful());
    assertFalse(decider.isPrepareSuccessful());
    assertFalse(decider.shouldDoCommit());
    assertEquals(Consistency.CONSISTENT, decider.getConsistency());
  }

  @Test
  public void prepareFailRollbackFail() {
    ChangeProcessDecider decider = new ChangeProcessDecider();
    decider.setResults(results);

    UUID uuid = UUID.randomUUID();

    decider.startDiscovery(setOf("server1", "server2"));
    decider.discovered("server1", discovery(COMMITTED));
    decider.discovered("server2", discovery(COMMITTED));
    decider.endDiscovery();
    decider.startPrepare(uuid);
    decider.prepared("server1");
    decider.prepareFail("server2");
    decider.endPrepare();
    decider.startRollback();
    decider.rollbackFail("server2");
    decider.endRollback();

    assertTrue(decider.isDiscoverSuccessful());
    assertFalse(decider.isPrepareSuccessful());
    assertFalse(decider.shouldDoCommit());
    assertEquals(Consistency.MAY_NEED_RECOVERY, decider.getConsistency());
  }

  @Test
  public void prepareFailRollbackOtherClient() {
    ChangeProcessDecider decider = new ChangeProcessDecider();
    decider.setResults(results);

    UUID uuid = UUID.randomUUID();

    decider.startDiscovery(setOf("server1", "server2"));
    decider.discovered("server1", discovery(COMMITTED));
    decider.discovered("server2", discovery(COMMITTED));
    decider.endDiscovery();
    decider.startPrepare(uuid);
    decider.prepared("server1");
    decider.prepareFail("server2");
    decider.endPrepare();
    decider.startRollback();
    decider.rollbackOtherClient("server2", "lastMutationHost", "lastMutationUser");
    decider.endRollback();

    assertTrue(decider.isDiscoverSuccessful());
    assertFalse(decider.isPrepareSuccessful());
    assertFalse(decider.shouldDoCommit());
    assertEquals(Consistency.MAY_NEED_RECOVERY, decider.getConsistency());
  }

  @Test
  public void prepareOtherClient() {
    ChangeProcessDecider decider = new ChangeProcessDecider();
    decider.setResults(results);

    UUID uuid = UUID.randomUUID();

    decider.startDiscovery(setOf("server1", "server2"));
    decider.discovered("server1", discovery(COMMITTED));
    decider.discovered("server2", discovery(COMMITTED));
    decider.endDiscovery();
    decider.startPrepare(uuid);
    decider.prepared("server1");
    decider.prepareOtherClient("server2", "lastMutationHost", "lastMutationUser");
    decider.endPrepare();

    assertTrue(decider.isDiscoverSuccessful());
    assertFalse(decider.isPrepareSuccessful());
    assertFalse(decider.shouldDoCommit());
  }
}
