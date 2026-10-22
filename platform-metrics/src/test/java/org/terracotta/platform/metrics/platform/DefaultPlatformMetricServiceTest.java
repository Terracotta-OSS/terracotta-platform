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

import static org.terracotta.event.EventService.Event.CLIENT_JOIN;
import static org.terracotta.event.EventService.Event.CLIENT_LEAVE;
import static org.terracotta.event.EventService.Event.CONNECTION_CLOSE;
import static org.terracotta.event.EventService.Event.CONNECTION_OPEN;
import static org.terracotta.event.EventService.Event.ENTITY_FETCH;
import static org.terracotta.event.EventService.Event.ENTITY_INVOKE;
import static org.terracotta.event.EventService.Event.ENTITY_RELEASE;
import static org.terracotta.event.EventService.Event.MEMBER_STATE;
import static org.terracotta.event.EventService.Event.PEER_JOIN;
import static org.terracotta.event.EventService.Event.PEER_LEAVE;
import static org.terracotta.event.EventService.Event.PEER_STATE;
import static org.terracotta.event.EventService.Event.VOTER_JOIN;
import static org.terracotta.event.EventService.Event.VOTER_LEAVE;
import static org.terracotta.metrics.MetricService.Metric.CLIENTS;
import static org.terracotta.metrics.MetricService.Metric.CONNECTIONS;
import static org.terracotta.metrics.MetricService.Metric.ENTITY_FETCHES;
import static org.terracotta.metrics.MetricService.Metric.PEERS;
import static org.terracotta.metrics.MetricService.Metric.VOTERS;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
  public void testGetCurrentPeerCount() {
    collector.addMetricSupplier(PEERS, () -> 3L);
    assertEquals(3L, service.getCurrentPeerCount());
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
    assertEquals(0L, service.getMeteredConnectionOpened(false));
  }

  @Test
  public void testGetMeteredConnectionOpenedAccumulatesEvents() {
    // fire event (creates counter), read to prime meter (baseline=1), fire more, read delta
    collector.fireEvent(CONNECTION_OPEN, emptyMap());
    service.getMeteredConnectionOpened(false); // prime: snapshot=1
    collector.fireEvent(CONNECTION_OPEN, emptyMap());
    collector.fireEvent(CONNECTION_OPEN, emptyMap());
    assertEquals(2L, service.getMeteredConnectionOpened(false));
  }

  @Test
  public void testGetMeteredConnectionClosed() {
    collector.fireEvent(CONNECTION_CLOSE, emptyMap());
    service.getMeteredConnectionClosed(false);
    collector.fireEvent(CONNECTION_CLOSE, emptyMap());
    assertEquals(1L, service.getMeteredConnectionClosed(false));
  }

  @Test
  public void testGetMeteredServerJoinsAndLeaves() {
    collector.fireEvent(PEER_JOIN, emptyMap());
    service.getMeteredServerJoins(false);
    collector.fireEvent(PEER_JOIN, emptyMap());
    collector.fireEvent(PEER_JOIN, emptyMap());
    assertEquals(2L, service.getMeteredServerJoins(false));

    collector.fireEvent(PEER_LEAVE, emptyMap());
    service.getMeteredServerLeaves(false);
    collector.fireEvent(PEER_LEAVE, emptyMap());
    assertEquals(1L, service.getMeteredServerLeaves(false));
  }

  @Test
  public void testGetMeteredVoterJoinsAndLeaves() {
    collector.fireEvent(VOTER_JOIN, emptyMap());
    service.getMeteredVoterJoins(false);
    collector.fireEvent(VOTER_JOIN, emptyMap());
    assertEquals(1L, service.getMeteredVoterJoins(false));

    collector.fireEvent(VOTER_LEAVE, emptyMap());
    service.getMeteredVoterLeaves(false);
    collector.fireEvent(VOTER_LEAVE, emptyMap());
    collector.fireEvent(VOTER_LEAVE, emptyMap());
    assertEquals(2L, service.getMeteredVoterLeaves(false));
  }

  @Test
  public void testGetMeteredClientJoinsAndLeaves() {
    collector.fireEvent(CLIENT_JOIN, emptyMap());
    service.getMeteredClientJoins(false);
    collector.fireEvent(CLIENT_JOIN, emptyMap());
    collector.fireEvent(CLIENT_JOIN, emptyMap());
    assertEquals(2L, service.getMeteredClientJoins(false));

    collector.fireEvent(CLIENT_LEAVE, emptyMap());
    service.getMeteredClientLeaves(false);
    collector.fireEvent(CLIENT_LEAVE, emptyMap());
    assertEquals(1L, service.getMeteredClientLeaves(false));
  }

  @Test
  public void testGetMeteredEntityFetchesAndReleases() {
    collector.fireEvent(ENTITY_FETCH, emptyMap());
    service.getMeteredEntityFetches(false);
    collector.fireEvent(ENTITY_FETCH, emptyMap());
    assertEquals(1L, service.getMeteredEntityFetches(false));

    collector.fireEvent(ENTITY_RELEASE, emptyMap());
    service.getMeteredEntityReleases(false);
    collector.fireEvent(ENTITY_RELEASE, emptyMap());
    assertEquals(1L, service.getMeteredEntityReleases(false));
  }

  @Test
  public void testGetMeteredTotalEntityInvocations() {
    collector.fireEvent(ENTITY_INVOKE, emptyMap());
    service.getMeteredTotalEntityInvocations(false);
    collector.fireEvent(ENTITY_INVOKE, emptyMap());
    collector.fireEvent(ENTITY_INVOKE, emptyMap());
    collector.fireEvent(ENTITY_INVOKE, emptyMap());
    assertEquals(3L, service.getMeteredTotalEntityInvocations(false));
  }

  // --- meters: entity type invocations ---

  @Test
  public void testConstructorRegistersEntityTypeCounters() {
    // the collector only counts registered types, so the service must register them up front
    for (String type : new String[]{CACHE_ENTITY, CACHE_MANAGER_ENTITY, STORE_ENTITY, SEQ_ENTITY}) {
      assertTrue(type + " must be registered", collector.getCounter(ENTITY_INVOKE, type).isPresent());
      assertEquals(0L, collector.getCounter(ENTITY_INVOKE, type).get().get());
    }
  }

  @Test
  public void testUnregisteredEntityTypeIsNotCountedPerType() {
    String other = "com.example.OtherEntity";
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", other));

    assertFalse(collector.getCounter(ENTITY_INVOKE, other).isPresent());
    assertEquals(1L, collector.getCounter(ENTITY_INVOKE).get().get());
  }

  @Test
  public void testEntityTypeMeterCountsFromFirstReadWithoutPriorFire() {
    // counters exist from construction: no event has to be fired before the meter is first read
    service.getMeteredCacheEntityInvocations(false);
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", CACHE_ENTITY));
    assertEquals(1L, service.getMeteredCacheEntityInvocations(false));
  }

  @Test
  public void testGetMeteredCacheEntityInvocations() {
    // CIC sums InternalClusterTierClientEntity + ClusterTierManagerClientEntity
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", CACHE_ENTITY));
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", CACHE_MANAGER_ENTITY));
    service.getMeteredCacheEntityInvocations(false);
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", CACHE_ENTITY));
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", CACHE_ENTITY));
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", CACHE_MANAGER_ENTITY));
    assertEquals(3L, service.getMeteredCacheEntityInvocations(false));
  }

  @Test
  public void testGetMeteredStoreEntityInvocations() {
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", STORE_ENTITY));
    service.getMeteredStoreEntityInvocations(false);
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", STORE_ENTITY));
    assertEquals(1L, service.getMeteredStoreEntityInvocations(false));
  }

  @Test
  public void testGetMeteredSequenceEntityInvocations() {
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", SEQ_ENTITY));
    service.getMeteredSequenceEntityInvocations(false);
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", SEQ_ENTITY));
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", SEQ_ENTITY));
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", SEQ_ENTITY));
    assertEquals(3L, service.getMeteredSequenceEntityInvocations(false));
  }

  @Test
  public void testEntityTypeInvocationsAreIndependent() {
    // prime all meters (CIC uses both cache entity types)
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", CACHE_ENTITY));
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", CACHE_MANAGER_ENTITY));
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", STORE_ENTITY));
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", SEQ_ENTITY));
    service.getMeteredCacheEntityInvocations(false);
    service.getMeteredStoreEntityInvocations(false);
    service.getMeteredSequenceEntityInvocations(false);
    // fire new events
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", CACHE_ENTITY));
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", CACHE_MANAGER_ENTITY));
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", STORE_ENTITY));
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", STORE_ENTITY));

    assertEquals(2L, service.getMeteredCacheEntityInvocations(false));
    assertEquals(2L, service.getMeteredStoreEntityInvocations(false));
    assertEquals(0L, service.getMeteredSequenceEntityInvocations(false));
  }

  // --- getAndReset: three-phase (fire to create counter, prime meter, fire more, measure delta) ---

  @Test
  public void testGetAndResetMeteredConnectionOpenedResetsDelta() {
    // fire to create counter, prime the meter
    collector.fireEvent(CONNECTION_OPEN, emptyMap());
    service.getMeteredConnectionOpened(true); // primes snapshot=1, returns 0
    // now fire 2 more events
    collector.fireEvent(CONNECTION_OPEN, emptyMap());
    collector.fireEvent(CONNECTION_OPEN, emptyMap());
    assertEquals(2L, service.getMeteredConnectionOpened(true));
    // after reset, delta is 0
    assertEquals(0L, service.getMeteredConnectionOpened(false));
  }

  @Test
  public void testGetAndResetMeteredConnectionClosedResetsDelta() {
    collector.fireEvent(CONNECTION_CLOSE, emptyMap());
    service.getMeteredConnectionClosed(true);
    collector.fireEvent(CONNECTION_CLOSE, emptyMap());
    assertEquals(1L, service.getMeteredConnectionClosed(true));
    assertEquals(0L, service.getMeteredConnectionClosed(false));
  }

  @Test
  public void testGetAndResetMeteredServerJoins() {
    collector.fireEvent(PEER_JOIN, emptyMap());
    service.getMeteredServerJoins(true);
    collector.fireEvent(PEER_JOIN, emptyMap());
    collector.fireEvent(PEER_JOIN, emptyMap());
    assertEquals(2L, service.getMeteredServerJoins(true));
    assertEquals(0L, service.getMeteredServerJoins(false));
  }

  @Test
  public void testGetAndResetMeteredServerLeaves() {
    collector.fireEvent(PEER_LEAVE, emptyMap());
    service.getMeteredServerLeaves(true);
    collector.fireEvent(PEER_LEAVE, emptyMap());
    assertEquals(1L, service.getMeteredServerLeaves(true));
    assertEquals(0L, service.getMeteredServerLeaves(false));
  }

  @Test
  public void testGetAndResetMeteredVoterJoins() {
    collector.fireEvent(VOTER_JOIN, emptyMap());
    service.getMeteredVoterJoins(true);
    collector.fireEvent(VOTER_JOIN, emptyMap());
    assertEquals(1L, service.getMeteredVoterJoins(true));
    assertEquals(0L, service.getMeteredVoterJoins(false));
  }

  @Test
  public void testGetAndResetMeteredVoterLeaves() {
    collector.fireEvent(VOTER_LEAVE, emptyMap());
    service.getMeteredVoterLeaves(true);
    collector.fireEvent(VOTER_LEAVE, emptyMap());
    assertEquals(1L, service.getMeteredVoterLeaves(true));
    assertEquals(0L, service.getMeteredVoterLeaves(false));
  }

  @Test
  public void testGetAndResetMeteredClientJoins() {
    collector.fireEvent(CLIENT_JOIN, emptyMap());
    service.getMeteredClientJoins(true);
    collector.fireEvent(CLIENT_JOIN, emptyMap());
    assertEquals(1L, service.getMeteredClientJoins(true));
    assertEquals(0L, service.getMeteredClientJoins(false));
  }

  @Test
  public void testGetAndResetMeteredClientLeaves() {
    collector.fireEvent(CLIENT_LEAVE, emptyMap());
    service.getMeteredClientLeaves(true);
    collector.fireEvent(CLIENT_LEAVE, emptyMap());
    assertEquals(1L, service.getMeteredClientLeaves(true));
    assertEquals(0L, service.getMeteredClientLeaves(false));
  }

  @Test
  public void testGetAndResetMeteredEntityFetches() {
    collector.fireEvent(ENTITY_FETCH, emptyMap());
    service.getMeteredEntityFetches(true);
    collector.fireEvent(ENTITY_FETCH, emptyMap());
    collector.fireEvent(ENTITY_FETCH, emptyMap());
    assertEquals(2L, service.getMeteredEntityFetches(true));
    assertEquals(0L, service.getMeteredEntityFetches(false));
  }

  @Test
  public void testGetAndResetMeteredEntityReleases() {
    collector.fireEvent(ENTITY_RELEASE, emptyMap());
    service.getMeteredEntityReleases(true);
    collector.fireEvent(ENTITY_RELEASE, emptyMap());
    assertEquals(1L, service.getMeteredEntityReleases(true));
    assertEquals(0L, service.getMeteredEntityReleases(false));
  }

  @Test
  public void testGetAndResetMeteredTotalEntityInvocations() {
    collector.fireEvent(ENTITY_INVOKE, emptyMap());
    service.getMeteredTotalEntityInvocations(true);
    collector.fireEvent(ENTITY_INVOKE, emptyMap());
    collector.fireEvent(ENTITY_INVOKE, emptyMap());
    collector.fireEvent(ENTITY_INVOKE, emptyMap());
    assertEquals(3L, service.getMeteredTotalEntityInvocations(true));
    assertEquals(0L, service.getMeteredTotalEntityInvocations(false));
  }

  @Test
  public void testGetAndResetMeteredCacheEntityInvocations() {
    // CIC sums InternalClusterTierClientEntity + ClusterTierManagerClientEntity
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", CACHE_ENTITY));
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", CACHE_MANAGER_ENTITY));
    service.getMeteredCacheEntityInvocations(true);
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", CACHE_ENTITY));
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", CACHE_MANAGER_ENTITY));
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", CACHE_MANAGER_ENTITY));
    assertEquals(3L, service.getMeteredCacheEntityInvocations(true));
    assertEquals(0L, service.getMeteredCacheEntityInvocations(false));
  }

  @Test
  public void testGetAndResetMeteredStoreEntityInvocations() {
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", STORE_ENTITY));
    service.getMeteredStoreEntityInvocations(true);
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", STORE_ENTITY));
    assertEquals(1L, service.getMeteredStoreEntityInvocations(true));
    assertEquals(0L, service.getMeteredStoreEntityInvocations(false));
  }

  @Test
  public void testGetAndResetMeteredSequenceEntityInvocations() {
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", SEQ_ENTITY));
    service.getMeteredSequenceEntityInvocations(true);
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", SEQ_ENTITY));
    assertEquals(1L, service.getMeteredSequenceEntityInvocations(true));
    assertEquals(0L, service.getMeteredSequenceEntityInvocations(false));
  }

  // --- resetMeters ---

  @Test
  public void testResetMetersZeroesAllDeltas() {
    collector.fireEvent(CONNECTION_OPEN, emptyMap());
    collector.fireEvent(CLIENT_JOIN, emptyMap());
    collector.fireEvent(PEER_JOIN, emptyMap());
    collector.fireEvent(VOTER_JOIN, emptyMap());
    collector.fireEvent(ENTITY_FETCH, emptyMap());
    collector.fireEvent(ENTITY_INVOKE, emptyMap());

    // Initialise meters (first access creates the MeteredCounter objects)
    service.getMeteredConnectionOpened(false);
    service.getMeteredClientJoins(false);
    service.getMeteredServerJoins(false);
    service.getMeteredVoterJoins(false);
    service.getMeteredEntityFetches(false);
    service.getMeteredTotalEntityInvocations(false);

    service.resetMeters();

    assertEquals(0L, service.getMeteredConnectionOpened(false));
    assertEquals(0L, service.getMeteredClientJoins(false));
    assertEquals(0L, service.getMeteredServerJoins(false));
    assertEquals(0L, service.getMeteredVoterJoins(false));
    assertEquals(0L, service.getMeteredEntityFetches(false));
    assertEquals(0L, service.getMeteredTotalEntityInvocations(false));
  }

  @Test
  public void testGetAndResetAccumulatesNewEventsAfterReset() {
    collector.fireEvent(CONNECTION_OPEN, emptyMap());
    service.getMeteredConnectionOpened(true); // reset

    // Fire more events after reset
    collector.fireEvent(CONNECTION_OPEN, emptyMap());
    collector.fireEvent(CONNECTION_OPEN, emptyMap());

    assertEquals(2L, service.getMeteredConnectionOpened(true));
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
  public void testGetStripeStatesIncludesPeersFromPeerStateEvents() {
    // Fire two PEER_STATE events for two peers
    collector.fireEvent(PEER_STATE, Map.of("peer", "peer1:9410", "state", "PASSIVE-STANDBY"));
    collector.fireEvent(PEER_STATE, Map.of("peer", "peer2:9410", "state", "ACTIVE"));

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
    collector.fireEvent(PEER_STATE, Map.of("peer", "node2:9410", "state", "PASSIVE-STANDBY"));
    assertEquals(LogicalServerState.PASSIVE, service.getStripeStates().get("node2:9410"));

    // Peer promotes to active (e.g. after failover)
    collector.fireEvent(PEER_STATE, Map.of("peer", "node2:9410", "state", "ACTIVE"));
    assertEquals(LogicalServerState.ACTIVE, service.getStripeStates().get("node2:9410"));
  }

  @Test
  public void testGetStripeStatesIsTreeMapOrderedByKey() {
    collector.fireEvent(PEER_STATE, Map.of("peer", "zzz:9410", "state", "PASSIVE-STANDBY"));
    collector.fireEvent(PEER_STATE, Map.of("peer", "aaa:9410", "state", "ACTIVE"));

    TreeMap<String, LogicalServerState> states = service.getStripeStates();

    // TreeMap preserves natural (alphabetical) order
    String first = states.firstKey();
    String last = states.lastKey();
    assertTrue("'aaa:9410' must sort before 'zzz:9410'",
      first.compareTo(last) < 0);
  }

  @Test
  public void testGetStripeStatesIgnoresMemberStateEvents() {
    // MEMBER_STATE is the state of this server: it is read live from the Server, never stored as a peer
    collector.fireEvent(MEMBER_STATE, Map.of("state", "PASSIVE-STANDBY"));

    TreeMap<String, LogicalServerState> states = service.getStripeStates();

    assertEquals(1, states.size());
    assertEquals(LogicalServerState.ACTIVE, states.get("localhost:9410"));
  }

  @Test
  public void testGetStripeStatesRemovesStoppedPeer() {
    collector.fireEvent(PEER_STATE, Map.of("peer", "node2:9410", "state", "PASSIVE-STANDBY"));
    assertTrue(service.getStripeStates().containsKey("node2:9410"));

    collector.fireEvent(PEER_STATE, Map.of("peer", "node2:9410", "state", "STOP-STATE"));
    assertFalse(service.getStripeStates().containsKey("node2:9410"));
  }

  @Test
  public void testGetStripeStatesNoPeers() {
    // No PEER_STATE events fired — only this server should appear
    TreeMap<String, LogicalServerState> states = service.getStripeStates();
    assertEquals(1, states.size());
    assertTrue(states.containsKey("localhost:9410"));
  }
}
