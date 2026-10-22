/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2026
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
package org.terracotta.platform.metrics.platform;

import org.junit.Before;
import org.junit.Test;
import org.terracotta.diagnostic.model.LogicalServerState;
import org.terracotta.platform.metrics.voltron.DefaultVoltronMetricCollector;
import org.terracotta.server.Server;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

import static com.tc.spi.metric.MetricService.Event.CLIENT_JOIN;
import static com.tc.spi.metric.MetricService.Event.CLIENT_LEAVE;
import static com.tc.spi.metric.MetricService.Event.CONNECTION_CLOSE;
import static com.tc.spi.metric.MetricService.Event.CONNECTION_OPEN;
import static com.tc.spi.metric.MetricService.Event.ENTITY_FETCH;
import static com.tc.spi.metric.MetricService.Event.ENTITY_INVOKE;
import static com.tc.spi.metric.MetricService.Event.ENTITY_RELEASE;
import static com.tc.spi.metric.MetricService.Event.MEMBER_JOIN;
import static com.tc.spi.metric.MetricService.Event.MEMBER_LEAVE;
import static com.tc.spi.metric.MetricService.Event.MEMBER_STATE;
import static com.tc.spi.metric.MetricService.Event.VOTER_JOIN;
import static com.tc.spi.metric.MetricService.Event.VOTER_LEAVE;
import static com.tc.spi.metric.MetricService.Metric.CLIENTS;
import static com.tc.spi.metric.MetricService.Metric.CONNECTIONS;
import static com.tc.spi.metric.MetricService.Metric.ENTITY_FETCHES;
import static com.tc.spi.metric.MetricService.Metric.MEMBERS;
import static com.tc.spi.metric.MetricService.Metric.VOTERS;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DefaultPlatformMetricService.
 */
public class DefaultPlatformMetricServiceTest {

  private static final String CACHE_ENTITY = "org.ehcache.clustered.client.internal.store.InternalClusterTierClientEntity";
  private static final String CACHE_MANAGER_ENTITY = "org.ehcache.clustered.client.internal.ClusterTierManagerClientEntity";
  private static final String STORE_ENTITY = "com.terracottatech.store.client.DatasetEntity";
  private static final String SEQ_ENTITY = "com.terracottatech.sequence.client.entity.SequenceEntity";

  private DefaultVoltronMetricCollector collector;
  private Server server;
  private DefaultPlatformMetricService service;

  @Before
  public void setUp() {
    collector = new DefaultVoltronMetricCollector();
    server = mock(Server.class);
    when(server.getServerHostName()).thenReturn("localhost");
    when(server.getServerPort()).thenReturn(9410);
    when(server.getState()).thenReturn("ACTIVE");
    service = new DefaultPlatformMetricService(server, collector);
  }

  // --- gauges ---

  @Test
  public void testGetCurrentConnectionCountReturnsGaugeValue() {
    AtomicLong value = new AtomicLong(7);
    collector.addMetricSupplier(CONNECTIONS, value::get);

    assertEquals(7L, service.getCurrentConnectionCount());
    value.set(12);
    assertEquals(12L, service.getCurrentConnectionCount());
  }

  @Test
  public void testGetCurrentConnectionCountZeroWhenNoSupplier() {
    assertEquals(0L, service.getCurrentConnectionCount());
  }

  @Test
  public void testGetCurrentServerCount() {
    collector.addMetricSupplier(MEMBERS, () -> 3L);
    assertEquals(3L, service.getCurrentServerCount());
  }

  @Test
  public void testGetCurrentVoterCount() {
    collector.addMetricSupplier(VOTERS, () -> 2L);
    assertEquals(2L, service.getCurrentVoterCount());
  }

  @Test
  public void testGetCurrentClientCount() {
    collector.addMetricSupplier(CLIENTS, () -> 5L);
    assertEquals(5L, service.getCurrentClientCount());
  }

  @Test
  public void testGetCurrentEntityFetchCount() {
    collector.addMetricSupplier(ENTITY_FETCHES, () -> 11L);
    assertEquals(11L, service.getCurrentEntityFetchCount());
  }

  // --- meters: read-only ---

  @Test
  public void testGetMeteredConnectionOpenedZeroInitially() {
    assertEquals(0L, service.getMeteredConnectionOpened());
  }

  @Test
  public void testGetMeteredConnectionOpenedAccumulatesEvents() {
    // fire event (creates counter), read to prime meter (baseline=1), fire more, read delta
    collector.fireEvent(CONNECTION_OPEN, emptyMap());
    service.getMeteredConnectionOpened(); // prime: snapshot=1
    collector.fireEvent(CONNECTION_OPEN, emptyMap());
    collector.fireEvent(CONNECTION_OPEN, emptyMap());
    assertEquals(2L, service.getMeteredConnectionOpened());
  }

  @Test
  public void testGetMeteredConnectionClosed() {
    collector.fireEvent(CONNECTION_CLOSE, emptyMap());
    service.getMeteredConnectionClosed();
    collector.fireEvent(CONNECTION_CLOSE, emptyMap());
    assertEquals(1L, service.getMeteredConnectionClosed());
  }

  @Test
  public void testGetMeteredServerJoinsAndLeaves() {
    collector.fireEvent(MEMBER_JOIN, emptyMap());
    service.getMeteredServerJoins();
    collector.fireEvent(MEMBER_JOIN, emptyMap());
    collector.fireEvent(MEMBER_JOIN, emptyMap());
    assertEquals(2L, service.getMeteredServerJoins());

    collector.fireEvent(MEMBER_LEAVE, emptyMap());
    service.getMeteredServerLeaves();
    collector.fireEvent(MEMBER_LEAVE, emptyMap());
    assertEquals(1L, service.getMeteredServerLeaves());
  }

  @Test
  public void testGetMeteredVoterJoinsAndLeaves() {
    collector.fireEvent(VOTER_JOIN, emptyMap());
    service.getMeteredVoterJoins();
    collector.fireEvent(VOTER_JOIN, emptyMap());
    assertEquals(1L, service.getMeteredVoterJoins());

    collector.fireEvent(VOTER_LEAVE, emptyMap());
    service.getMeteredVoterLeaves();
    collector.fireEvent(VOTER_LEAVE, emptyMap());
    collector.fireEvent(VOTER_LEAVE, emptyMap());
    assertEquals(2L, service.getMeteredVoterLeaves());
  }

  @Test
  public void testGetMeteredClientJoinsAndLeaves() {
    collector.fireEvent(CLIENT_JOIN, emptyMap());
    service.getMeteredClientJoins();
    collector.fireEvent(CLIENT_JOIN, emptyMap());
    collector.fireEvent(CLIENT_JOIN, emptyMap());
    assertEquals(2L, service.getMeteredClientJoins());

    collector.fireEvent(CLIENT_LEAVE, emptyMap());
    service.getMeteredClientLeaves();
    collector.fireEvent(CLIENT_LEAVE, emptyMap());
    assertEquals(1L, service.getMeteredClientLeaves());
  }

  @Test
  public void testGetMeteredEntityFetchesAndReleases() {
    collector.fireEvent(ENTITY_FETCH, emptyMap());
    service.getMeteredEntityFetches();
    collector.fireEvent(ENTITY_FETCH, emptyMap());
    assertEquals(1L, service.getMeteredEntityFetches());

    collector.fireEvent(ENTITY_RELEASE, emptyMap());
    service.getMeteredEntityReleases();
    collector.fireEvent(ENTITY_RELEASE, emptyMap());
    assertEquals(1L, service.getMeteredEntityReleases());
  }

  @Test
  public void testGetMeteredTotalEntityInvocations() {
    collector.fireEvent(ENTITY_INVOKE, emptyMap());
    service.getMeteredTotalEntityInvocations();
    collector.fireEvent(ENTITY_INVOKE, emptyMap());
    collector.fireEvent(ENTITY_INVOKE, emptyMap());
    collector.fireEvent(ENTITY_INVOKE, emptyMap());
    assertEquals(3L, service.getMeteredTotalEntityInvocations());
  }

  // --- meters: entity type invocations ---

  @Test
  public void testGetMeteredCacheEntityInvocations() {
    // CIC sums InternalClusterTierClientEntity + ClusterTierManagerClientEntity
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", CACHE_ENTITY));
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", CACHE_MANAGER_ENTITY));
    service.getMeteredCacheEntityInvocations();
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", CACHE_ENTITY));
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", CACHE_ENTITY));
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", CACHE_MANAGER_ENTITY));
    assertEquals(3L, service.getMeteredCacheEntityInvocations());
  }

  @Test
  public void testGetMeteredStoreEntityInvocations() {
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", STORE_ENTITY));
    service.getMeteredStoreEntityInvocations();
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", STORE_ENTITY));
    assertEquals(1L, service.getMeteredStoreEntityInvocations());
  }

  @Test
  public void testGetMeteredSequenceEntityInvocations() {
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", SEQ_ENTITY));
    service.getMeteredSequenceEntityInvocations();
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", SEQ_ENTITY));
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", SEQ_ENTITY));
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", SEQ_ENTITY));
    assertEquals(3L, service.getMeteredSequenceEntityInvocations());
  }

  @Test
  public void testEntityTypeInvocationsAreIndependent() {
    // prime all meters (CIC uses both cache entity types)
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", CACHE_ENTITY));
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", CACHE_MANAGER_ENTITY));
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", STORE_ENTITY));
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", SEQ_ENTITY));
    service.getMeteredCacheEntityInvocations();
    service.getMeteredStoreEntityInvocations();
    service.getMeteredSequenceEntityInvocations();
    // fire new events
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", CACHE_ENTITY));
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", CACHE_MANAGER_ENTITY));
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", STORE_ENTITY));
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", STORE_ENTITY));

    assertEquals(2L, service.getMeteredCacheEntityInvocations());
    assertEquals(2L, service.getMeteredStoreEntityInvocations());
    assertEquals(0L, service.getMeteredSequenceEntityInvocations());
  }

  // --- getAndReset: three-phase (fire to create counter, prime meter, fire more, measure delta) ---

  @Test
  public void testGetAndResetMeteredConnectionOpenedResetsDelta() {
    // fire to create counter, prime the meter
    collector.fireEvent(CONNECTION_OPEN, emptyMap());
    service.getAndResetMeteredConnectionOpened(); // primes snapshot=1, returns 0
    // now fire 2 more events
    collector.fireEvent(CONNECTION_OPEN, emptyMap());
    collector.fireEvent(CONNECTION_OPEN, emptyMap());
    assertEquals(2L, service.getAndResetMeteredConnectionOpened());
    // after reset, delta is 0
    assertEquals(0L, service.getMeteredConnectionOpened());
  }

  @Test
  public void testGetAndResetMeteredConnectionClosedResetsDelta() {
    collector.fireEvent(CONNECTION_CLOSE, emptyMap());
    service.getAndResetMeteredConnectionClosed();
    collector.fireEvent(CONNECTION_CLOSE, emptyMap());
    assertEquals(1L, service.getAndResetMeteredConnectionClosed());
    assertEquals(0L, service.getMeteredConnectionClosed());
  }

  @Test
  public void testGetAndResetMeteredServerJoins() {
    collector.fireEvent(MEMBER_JOIN, emptyMap());
    service.getAndResetMeteredServerJoins();
    collector.fireEvent(MEMBER_JOIN, emptyMap());
    collector.fireEvent(MEMBER_JOIN, emptyMap());
    assertEquals(2L, service.getAndResetMeteredServerJoins());
    assertEquals(0L, service.getMeteredServerJoins());
  }

  @Test
  public void testGetAndResetMeteredServerLeaves() {
    collector.fireEvent(MEMBER_LEAVE, emptyMap());
    service.getAndResetMeteredServerLeaves();
    collector.fireEvent(MEMBER_LEAVE, emptyMap());
    assertEquals(1L, service.getAndResetMeteredServerLeaves());
    assertEquals(0L, service.getMeteredServerLeaves());
  }

  @Test
  public void testGetAndResetMeteredVoterJoins() {
    collector.fireEvent(VOTER_JOIN, emptyMap());
    service.getAndResetMeteredVoterJoins();
    collector.fireEvent(VOTER_JOIN, emptyMap());
    assertEquals(1L, service.getAndResetMeteredVoterJoins());
    assertEquals(0L, service.getMeteredVoterJoins());
  }

  @Test
  public void testGetAndResetMeteredVoterLeaves() {
    collector.fireEvent(VOTER_LEAVE, emptyMap());
    service.getAndResetMeteredVoterLeaves();
    collector.fireEvent(VOTER_LEAVE, emptyMap());
    assertEquals(1L, service.getAndResetMeteredVoterLeaves());
    assertEquals(0L, service.getMeteredVoterLeaves());
  }

  @Test
  public void testGetAndResetMeteredClientJoins() {
    collector.fireEvent(CLIENT_JOIN, emptyMap());
    service.getAndResetMeteredClientJoins();
    collector.fireEvent(CLIENT_JOIN, emptyMap());
    assertEquals(1L, service.getAndResetMeteredClientJoins());
    assertEquals(0L, service.getMeteredClientJoins());
  }

  @Test
  public void testGetAndResetMeteredClientLeaves() {
    collector.fireEvent(CLIENT_LEAVE, emptyMap());
    service.getAndResetMeteredClientLeaves();
    collector.fireEvent(CLIENT_LEAVE, emptyMap());
    assertEquals(1L, service.getAndResetMeteredClientLeaves());
    assertEquals(0L, service.getMeteredClientLeaves());
  }

  @Test
  public void testGetAndResetMeteredEntityFetches() {
    collector.fireEvent(ENTITY_FETCH, emptyMap());
    service.getAndResetMeteredEntityFetches();
    collector.fireEvent(ENTITY_FETCH, emptyMap());
    collector.fireEvent(ENTITY_FETCH, emptyMap());
    assertEquals(2L, service.getAndResetMeteredEntityFetches());
    assertEquals(0L, service.getMeteredEntityFetches());
  }

  @Test
  public void testGetAndResetMeteredEntityReleases() {
    collector.fireEvent(ENTITY_RELEASE, emptyMap());
    service.getAndResetMeteredEntityReleases();
    collector.fireEvent(ENTITY_RELEASE, emptyMap());
    assertEquals(1L, service.getAndResetMeteredEntityReleases());
    assertEquals(0L, service.getMeteredEntityReleases());
  }

  @Test
  public void testGetAndResetMeteredTotalEntityInvocations() {
    collector.fireEvent(ENTITY_INVOKE, emptyMap());
    service.getAndResetMeteredTotalEntityInvocations();
    collector.fireEvent(ENTITY_INVOKE, emptyMap());
    collector.fireEvent(ENTITY_INVOKE, emptyMap());
    collector.fireEvent(ENTITY_INVOKE, emptyMap());
    assertEquals(3L, service.getAndResetMeteredTotalEntityInvocations());
    assertEquals(0L, service.getMeteredTotalEntityInvocations());
  }

  @Test
  public void testGetAndResetMeteredCacheEntityInvocations() {
    // CIC sums InternalClusterTierClientEntity + ClusterTierManagerClientEntity
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", CACHE_ENTITY));
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", CACHE_MANAGER_ENTITY));
    service.getAndResetMeteredCacheEntityInvocations();
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", CACHE_ENTITY));
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", CACHE_MANAGER_ENTITY));
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", CACHE_MANAGER_ENTITY));
    assertEquals(3L, service.getAndResetMeteredCacheEntityInvocations());
    assertEquals(0L, service.getMeteredCacheEntityInvocations());
  }

  @Test
  public void testGetAndResetMeteredStoreEntityInvocations() {
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", STORE_ENTITY));
    service.getAndResetMeteredStoreEntityInvocations();
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", STORE_ENTITY));
    assertEquals(1L, service.getAndResetMeteredStoreEntityInvocations());
    assertEquals(0L, service.getMeteredStoreEntityInvocations());
  }

  @Test
  public void testGetAndResetMeteredSequenceEntityInvocations() {
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", SEQ_ENTITY));
    service.getAndResetMeteredSequenceEntityInvocations();
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", SEQ_ENTITY));
    assertEquals(1L, service.getAndResetMeteredSequenceEntityInvocations());
    assertEquals(0L, service.getMeteredSequenceEntityInvocations());
  }

  // --- resetMeters ---

  @Test
  public void testResetMetersZeroesAllDeltas() {
    collector.fireEvent(CONNECTION_OPEN, emptyMap());
    collector.fireEvent(CLIENT_JOIN, emptyMap());
    collector.fireEvent(MEMBER_JOIN, emptyMap());
    collector.fireEvent(VOTER_JOIN, emptyMap());
    collector.fireEvent(ENTITY_FETCH, emptyMap());
    collector.fireEvent(ENTITY_INVOKE, emptyMap());

    // Initialise meters (first access creates the MeteredCounter objects)
    service.getMeteredConnectionOpened();
    service.getMeteredClientJoins();
    service.getMeteredServerJoins();
    service.getMeteredVoterJoins();
    service.getMeteredEntityFetches();
    service.getMeteredTotalEntityInvocations();

    service.resetMeters();

    assertEquals(0L, service.getMeteredConnectionOpened());
    assertEquals(0L, service.getMeteredClientJoins());
    assertEquals(0L, service.getMeteredServerJoins());
    assertEquals(0L, service.getMeteredVoterJoins());
    assertEquals(0L, service.getMeteredEntityFetches());
    assertEquals(0L, service.getMeteredTotalEntityInvocations());
  }

  @Test
  public void testGetAndResetAccumulatesNewEventsAfterReset() {
    collector.fireEvent(CONNECTION_OPEN, emptyMap());
    service.getAndResetMeteredConnectionOpened(); // reset

    // Fire more events after reset
    collector.fireEvent(CONNECTION_OPEN, emptyMap());
    collector.fireEvent(CONNECTION_OPEN, emptyMap());

    assertEquals(2L, service.getAndResetMeteredConnectionOpened());
  }

  // --- gauges are not affected by resetMeters ---

  @Test
  public void testResetMetersDoesNotAffectGauges() {
    AtomicLong value = new AtomicLong(15);
    collector.addMetricSupplier(CONNECTIONS, value::get);

    service.resetMeters();

    assertEquals(15L, service.getCurrentConnectionCount());
  }

  // --- getStripeStates ---

  @Test
  public void testGetStripeStatesIncludesThisServer() {
    // Server mock is already set up: hostname=localhost, port=9410, state=ACTIVE
    TreeMap<String, LogicalServerState> states = service.getStripeStates();

    assertNotNull(states);
    assertTrue("must contain this server's key", states.containsKey("localhost:9410"));
    assertEquals(LogicalServerState.ACTIVE, states.get("localhost:9410"));
  }

  @Test
  public void testGetStripeStatesReflectsCurrentServerState() {
    // Server starts as ACTIVE
    assertEquals(LogicalServerState.ACTIVE, service.getStripeStates().get("localhost:9410"));

    // Simulate the server reporting PASSIVE
    when(server.getState()).thenReturn("PASSIVE-STANDBY");
    assertEquals(LogicalServerState.PASSIVE, service.getStripeStates().get("localhost:9410"));
  }

  @Test
  public void testGetStripeStatesIncludesPeersFromMemberStateEvents() {
    // Fire two MEMBER_STATE events for two peers
    collector.fireEvent(MEMBER_STATE, Map.of("peer", "peer1:9410", "state", "PASSIVE-STANDBY"));
    collector.fireEvent(MEMBER_STATE, Map.of("peer", "peer2:9410", "state", "ACTIVE"));

    TreeMap<String, LogicalServerState> states = service.getStripeStates();

    assertTrue("must contain peer1", states.containsKey("peer1:9410"));
    assertEquals(LogicalServerState.PASSIVE, states.get("peer1:9410"));
    assertTrue("must contain peer2", states.containsKey("peer2:9410"));
    assertEquals(LogicalServerState.ACTIVE, states.get("peer2:9410"));
    // must also contain this server
    assertTrue("must contain this server", states.containsKey("localhost:9410"));
  }

  @Test
  public void testGetStripeStatesPeerStateUpdatedByLaterEvent() {
    // Peer joins as passive
    collector.fireEvent(MEMBER_STATE, Map.of("peer", "node2:9410", "state", "PASSIVE-STANDBY"));
    assertEquals(LogicalServerState.PASSIVE, service.getStripeStates().get("node2:9410"));

    // Peer promotes to active (e.g. after failover)
    collector.fireEvent(MEMBER_STATE, Map.of("peer", "node2:9410", "state", "ACTIVE"));
    assertEquals(LogicalServerState.ACTIVE, service.getStripeStates().get("node2:9410"));
  }

  @Test
  public void testGetStripeStatesIsTreeMapOrderedByKey() {
    collector.fireEvent(MEMBER_STATE, Map.of("peer", "zzz:9410", "state", "PASSIVE-STANDBY"));
    collector.fireEvent(MEMBER_STATE, Map.of("peer", "aaa:9410", "state", "ACTIVE"));

    TreeMap<String, LogicalServerState> states = service.getStripeStates();

    // TreeMap preserves natural (alphabetical) order
    String first = states.firstKey();
    String last = states.lastKey();
    assertTrue("'aaa:9410' must sort before 'zzz:9410'",
      first.compareTo(last) < 0);
  }

  @Test
  public void testGetStripeStatesNoPeers() {
    // No MEMBER_STATE events fired — only this server should appear
    TreeMap<String, LogicalServerState> states = service.getStripeStates();
    assertEquals(1, states.size());
    assertTrue(states.containsKey("localhost:9410"));
  }
}
