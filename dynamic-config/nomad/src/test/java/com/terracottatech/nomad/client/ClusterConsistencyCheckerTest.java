/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.client;

import com.terracottatech.nomad.client.results.DiscoverResultsReceiver;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.UUID;

import static com.terracottatech.nomad.client.NomadTestHelper.discovery;
import static com.terracottatech.nomad.client.NomadTestHelper.matchSetOf;
import static com.terracottatech.nomad.server.ChangeRequestState.COMMITTED;
import static com.terracottatech.nomad.server.ChangeRequestState.ROLLED_BACK;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class ClusterConsistencyCheckerTest {
  @Mock
  private DiscoverResultsReceiver results;

  @After
  public void after() {
    verifyNoMoreInteractions(results);
  }

  @Test
  public void allCommitForSameUuid() {
    ClusterConsistencyChecker consistencyChecker = new ClusterConsistencyChecker();

    UUID uuid = UUID.randomUUID();

    consistencyChecker.discovered("server1", discovery(COMMITTED, uuid));
    consistencyChecker.discovered("server2", discovery(COMMITTED, uuid));

    consistencyChecker.checkClusterConsistency(results);

    verifyNoMoreInteractions(results);
  }

  @Test
  public void allRollbackForSameUuid() {
    ClusterConsistencyChecker consistencyChecker = new ClusterConsistencyChecker();

    UUID uuid = UUID.randomUUID();

    consistencyChecker.discovered("server1", discovery(ROLLED_BACK, uuid));
    consistencyChecker.discovered("server2", discovery(ROLLED_BACK, uuid));

    consistencyChecker.checkClusterConsistency(results);
  }

  @Test
  public void inconsistentCluster() {
    ClusterConsistencyChecker consistencyChecker = new ClusterConsistencyChecker();

    UUID uuid = UUID.randomUUID();

    consistencyChecker.discovered("server1", discovery(COMMITTED, uuid));
    consistencyChecker.discovered("server2", discovery(ROLLED_BACK, uuid));

    consistencyChecker.checkClusterConsistency(results);

    verify(results).discoverClusterInconsistent(eq(uuid), matchSetOf("server1"), matchSetOf("server2"));
  }

  @Test
  public void differentUuids() {
    ClusterConsistencyChecker consistencyChecker = new ClusterConsistencyChecker();

    consistencyChecker.discovered("server1", discovery(COMMITTED));
    consistencyChecker.discovered("server2", discovery(ROLLED_BACK));

    consistencyChecker.checkClusterConsistency(results);
  }

  @Test
  public void multipleInconsistencies() {
    ClusterConsistencyChecker consistencyChecker = new ClusterConsistencyChecker();

    UUID uuid1 = UUID.randomUUID();
    UUID uuid2 = UUID.randomUUID();

    consistencyChecker.discovered("server1", discovery(COMMITTED, uuid1));
    consistencyChecker.discovered("server2", discovery(ROLLED_BACK, uuid1));
    consistencyChecker.discovered("server3", discovery(COMMITTED, uuid2));
    consistencyChecker.discovered("server4", discovery(COMMITTED, uuid2));
    consistencyChecker.discovered("server5", discovery(ROLLED_BACK, uuid2));

    consistencyChecker.checkClusterConsistency(results);

    verify(results).discoverClusterInconsistent(eq(uuid1), matchSetOf("server1"), matchSetOf("server2"));
    verify(results).discoverClusterInconsistent(eq(uuid2), matchSetOf("server3", "server4"), matchSetOf("server5"));
  }
}
