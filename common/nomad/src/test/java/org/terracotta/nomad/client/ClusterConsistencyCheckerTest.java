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
package org.terracotta.nomad.client;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.terracotta.nomad.client.results.DiscoverResultsReceiver;

import java.net.InetSocketAddress;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.terracotta.nomad.client.NomadTestHelper.discovery;
import static org.terracotta.nomad.client.NomadTestHelper.withItems;
import static org.terracotta.nomad.server.ChangeRequestState.COMMITTED;

@RunWith(MockitoJUnitRunner.class)
public class ClusterConsistencyCheckerTest {

  UUID uuid1 = UUID.randomUUID();
  UUID uuid2 = UUID.randomUUID();

  @Mock
  private DiscoverResultsReceiver<String> results;

  private ClusterConsistencyChecker<String> consistencyChecker = new ClusterConsistencyChecker<>();
  private InetSocketAddress address1 = InetSocketAddress.createUnresolved("localhost", 9410);
  private InetSocketAddress address2 = InetSocketAddress.createUnresolved("localhost", 9411);
  private InetSocketAddress address3 = InetSocketAddress.createUnresolved("localhost", 9412);
  private InetSocketAddress address4 = InetSocketAddress.createUnresolved("localhost", 9413);
  private InetSocketAddress address5 = InetSocketAddress.createUnresolved("localhost", 9414);

  @After
  public void after() {
    verifyNoMoreInteractions(results);
  }

  @Test
  public void allCommitForSameUuid() {
    UUID uuid = UUID.randomUUID();

    consistencyChecker.discovered(address1, discovery(COMMITTED, uuid));
    consistencyChecker.discovered(address2, discovery(COMMITTED, uuid));

    consistencyChecker.checkClusterConsistency(results);

    verifyNoMoreInteractions(results);
  }

  @Test
  public void inconsistentCluster() {
    consistencyChecker.discovered(address1, discovery(COMMITTED, uuid1, "hash1"));
    consistencyChecker.discovered(address2, discovery(COMMITTED, uuid2, "hash2"));

    consistencyChecker.checkClusterConsistency(results);

    verify(results).discoverClusterInconsistent(withItems(uuid1, uuid2), withItems(address1, address2));
  }

  @Test
  public void differentHashes() {
    consistencyChecker.discovered(address1, discovery(COMMITTED, uuid1, "hash1"));
    consistencyChecker.discovered(address2, discovery(COMMITTED, uuid2, "hash2"));

    consistencyChecker.checkClusterConsistency(results);

    verify(results).discoverClusterInconsistent(withItems(uuid1, uuid2), withItems(address1, address2));
  }

  @Test
  public void differentCommittedUuidsSameHash() {
    consistencyChecker.discovered(address1, discovery(COMMITTED, UUID.randomUUID(), "hash"));
    consistencyChecker.discovered(address2, discovery(COMMITTED, UUID.randomUUID(), "hash"));

    consistencyChecker.checkClusterConsistency(results);

    verifyNoMoreInteractions(results);
  }

  @Test
  public void multipleInconsistencies() {
    UUID uuid1 = UUID.randomUUID();
    UUID uuid2 = UUID.randomUUID();

    consistencyChecker.discovered(address1, discovery(COMMITTED, uuid1, "hash1"));
    consistencyChecker.discovered(address3, discovery(COMMITTED, uuid2, "hash2"));
    consistencyChecker.discovered(address4, discovery(COMMITTED, uuid2, "hash2"));

    consistencyChecker.checkClusterConsistency(results);

    verify(results).discoverClusterInconsistent(withItems(uuid1, uuid2), withItems(address1, address3, address4));
  }
}
