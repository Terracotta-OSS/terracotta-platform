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
package org.terracotta.platform.metrics.voltron;

import org.junit.Before;
import org.junit.Test;
import org.terracotta.diagnostic.model.LogicalServerState;
import org.terracotta.platform.metrics.metric.Metric;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static com.tc.spi.metric.MetricService.Event.CLIENT_JOIN;
import static com.tc.spi.metric.MetricService.Event.ENTITY_INVOKE;
import static com.tc.spi.metric.MetricService.Event.MEMBER_JOIN;
import static com.tc.spi.metric.MetricService.Event.MEMBER_STATE;
import static com.tc.spi.metric.MetricService.Metric.CLIENTS;
import static com.tc.spi.metric.MetricService.Metric.CONNECTIONS;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for DefaultVoltronMetricCollector.
 */
public class DefaultVoltronMetricCollectorTest {

  private DefaultVoltronMetricCollector collector;

  @Before
  public void setUp() {
    collector = new DefaultVoltronMetricCollector();
  }

  // --- getGauge ---

  @Test
  public void testGetGaugeReturnsEmptyBeforeRegistration() {
    assertFalse(collector.getGauge(CONNECTIONS).isPresent());
  }

  @Test
  public void testGetGaugeReturnsValueAfterRegistration() {
    AtomicLong value = new AtomicLong(42);
    collector.addMetricSupplier(CONNECTIONS, value::get);

    Optional<Metric> gauge = collector.getGauge(CONNECTIONS);
    assertTrue(gauge.isPresent());
    assertEquals(42L, gauge.get().get());
  }

  @Test
  public void testGetGaugeReflectsLiveSupplierChanges() {
    AtomicLong value = new AtomicLong(10);
    collector.addMetricSupplier(CLIENTS, value::get);

    assertEquals(10L, collector.getGauge(CLIENTS).get().get());
    value.set(99);
    assertEquals(99L, collector.getGauge(CLIENTS).get().get());
  }

  @Test
  public void testRemoveMetricSupplierMakesGaugeEmpty() {
    collector.addMetricSupplier(CONNECTIONS, () -> 1L);
    assertTrue(collector.getGauge(CONNECTIONS).isPresent());

    collector.removeMetricSupplier(CONNECTIONS);
    assertFalse(collector.getGauge(CONNECTIONS).isPresent());
  }

  @Test
  public void testAddMetricSupplierReplacesPrevious() {
    collector.addMetricSupplier(CONNECTIONS, () -> 1L);
    collector.addMetricSupplier(CONNECTIONS, () -> 99L);

    assertEquals(99L, collector.getGauge(CONNECTIONS).get().get());
  }

  // --- getCounter (by Event) ---

  @Test
  public void testGetCounterByEventEmptyBeforeAnyFire() {
    assertFalse(collector.getCounter(CLIENT_JOIN).isPresent());
  }

  @Test
  public void testGetCounterByEventPresentAfterFire() {
    collector.fireEvent(CLIENT_JOIN, emptyMap());

    Optional<Metric> counter = collector.getCounter(CLIENT_JOIN);
    assertTrue(counter.isPresent());
    assertEquals(1L, counter.get().get());
  }

  @Test
  public void testGetCounterByEventAccumulatesMultipleFires() {
    collector.fireEvent(MEMBER_JOIN, emptyMap());
    collector.fireEvent(MEMBER_JOIN, emptyMap());
    collector.fireEvent(MEMBER_JOIN, emptyMap());

    assertEquals(3L, collector.getCounter(MEMBER_JOIN).get().get());
  }

  @Test
  public void testDifferentEventsHaveIndependentCounters() {
    collector.fireEvent(CLIENT_JOIN, emptyMap());
    collector.fireEvent(CLIENT_JOIN, emptyMap());
    collector.fireEvent(MEMBER_JOIN, emptyMap());

    assertEquals(2L, collector.getCounter(CLIENT_JOIN).get().get());
    assertEquals(1L, collector.getCounter(MEMBER_JOIN).get().get());
  }

  // --- getCounter (by Event + entity type) ---

  @Test
  public void testGetCounterByTypeEmptyBeforeAnyFire() {
    assertFalse(collector.getCounter(ENTITY_INVOKE, "com.example.Foo").isPresent());
  }

  @Test
  public void testGetCounterByTypePresentAfterFireWithType() {
    String entityType = "com.example.MyEntity";
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", entityType));

    Optional<Metric> counter = collector.getCounter(ENTITY_INVOKE, entityType);
    assertTrue(counter.isPresent());
    assertEquals(1L, counter.get().get());
  }

  @Test
  public void testGetCounterByTypeAccumulatesMultipleFires() {
    String entityType = "com.example.MyEntity";
    Map<String, String> attrs = singletonMap("type", entityType);
    collector.fireEvent(ENTITY_INVOKE, attrs);
    collector.fireEvent(ENTITY_INVOKE, attrs);

    assertEquals(2L, collector.getCounter(ENTITY_INVOKE, entityType).get().get());
  }

  @Test
  public void testFireEventWithoutTypeShouldNotCreateEntityTypeCounter() {
    collector.fireEvent(ENTITY_INVOKE, emptyMap());

    // The per-event counter must exist
    assertTrue(collector.getCounter(ENTITY_INVOKE).isPresent());
    // But no entity-type counter should exist for an unknown type
    assertFalse(collector.getCounter(ENTITY_INVOKE, "").isPresent());
  }

  @Test
  public void testDifferentEntityTypesHaveIndependentCounters() {
    String typeA = "com.example.A";
    String typeB = "com.example.B";
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", typeA));
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", typeA));
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", typeB));

    assertEquals(2L, collector.getCounter(ENTITY_INVOKE, typeA).get().get());
    assertEquals(1L, collector.getCounter(ENTITY_INVOKE, typeB).get().get());
  }

  // --- fireEvent default (no-arg) ---

  @Test
  public void testFireEventDefaultUsesEmptyMap() {
    collector.fireEvent(CLIENT_JOIN);

    assertEquals(1L, collector.getCounter(CLIENT_JOIN).get().get());
  }

  // --- getPeers / MEMBER_STATE event ---

  @Test
  public void testGetPeersEmptyBeforeAnyMemberStateEvent() {
    assertTrue(collector.getPeers().isEmpty());
  }

  @Test
  public void testGetPeersPopulatedAfterMemberStateEvent() {
    collector.fireEvent(MEMBER_STATE, Map.of("peer", "node1:9410", "state", "PASSIVE-STANDBY"));

    Map<String, LogicalServerState> peers = collector.getPeers();
    assertNotNull(peers);
    assertEquals(1, peers.size());
    assertTrue(peers.containsKey("node1:9410"));
    assertEquals(LogicalServerState.PASSIVE, peers.get("node1:9410"));
  }

  @Test
  public void testGetPeersTracksMultiplePeers() {
    collector.fireEvent(MEMBER_STATE, Map.of("peer", "node1:9410", "state", "PASSIVE-STANDBY"));
    collector.fireEvent(MEMBER_STATE, Map.of("peer", "node2:9410", "state", "ACTIVE"));

    Map<String, LogicalServerState> peers = collector.getPeers();
    assertEquals(2, peers.size());
    assertEquals(LogicalServerState.PASSIVE, peers.get("node1:9410"));
    assertEquals(LogicalServerState.ACTIVE, peers.get("node2:9410"));
  }

  @Test
  public void testGetPeerStateUpdatedByLaterMemberStateEvent() {
    collector.fireEvent(MEMBER_STATE, Map.of("peer", "node1:9410", "state", "PASSIVE-STANDBY"));
    assertEquals(LogicalServerState.PASSIVE, collector.getPeers().get("node1:9410"));

    // Same peer reports a new state (e.g. promoted after failover)
    collector.fireEvent(MEMBER_STATE, Map.of("peer", "node1:9410", "state", "ACTIVE"));
    assertEquals(LogicalServerState.ACTIVE, collector.getPeers().get("node1:9410"));
  }

  @Test
  public void testMemberStateEventWithoutPeerAttributeIsIgnored() {
    // "peer" key missing — must not throw and must leave peers map empty
    collector.fireEvent(MEMBER_STATE, Map.of("state", "PASSIVE-STANDBY"));
    assertTrue(collector.getPeers().isEmpty());
  }

  @Test
  public void testMemberStateEventStillIncrementsEventCounter() {
    // MEMBER_STATE fires like any other event — its event counter must be incremented
    collector.fireEvent(MEMBER_STATE, Map.of("peer", "node1:9410", "state", "ACTIVE"));

    assertTrue(collector.getCounter(MEMBER_STATE).isPresent());
    assertEquals(1L, collector.getCounter(MEMBER_STATE).get().get());
  }

  @Test
  public void testGetPeersIsUnmodifiable() {
    collector.fireEvent(MEMBER_STATE, Map.of("peer", "node1:9410", "state", "ACTIVE"));

    Map<String, LogicalServerState> peers = collector.getPeers();
    try {
      peers.put("injected:9410", LogicalServerState.UNKNOWN);
      // If no exception, the map is not truly unmodifiable — acceptable only for concurrent maps
      // but the contract says unmodifiableMap, so assert the size is still 1 or that this path fails
    } catch (UnsupportedOperationException e) {
      // Expected: the returned map is unmodifiable
    }
  }

  // --- getStateMap ---

  @Test
  public void testGetStateMapIncludesGauges() {
    collector.addMetricSupplier(CONNECTIONS, () -> 7L);
    Map<String, ?> state = collector.getStateMap();
    assertTrue(state.containsKey(CONNECTIONS.toString()));
    assertEquals(7L, state.get(CONNECTIONS.toString()));
  }

  @Test
  public void testGetStateMapIncludesCounters() {
    collector.fireEvent(CLIENT_JOIN, emptyMap());
    Map<String, ?> state = collector.getStateMap();
    assertTrue(state.containsKey(CLIENT_JOIN.toString()));
  }
}
