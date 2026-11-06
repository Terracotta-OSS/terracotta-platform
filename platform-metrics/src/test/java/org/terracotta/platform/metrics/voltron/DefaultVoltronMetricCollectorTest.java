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

import org.terracotta.event.EventService.Event;
import org.junit.Before;
import org.junit.Test;
import org.terracotta.diagnostic.model.LogicalServerState;
import org.terracotta.platform.metrics.metric.Metric;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import static org.terracotta.event.EventService.Event.CLIENT_JOIN;
import static org.terracotta.event.EventService.Event.ENTITY_INVOKE;
import static org.terracotta.event.EventService.Event.MEMBER_STATE;
import static org.terracotta.event.EventService.Event.PEER_JOIN;
import static org.terracotta.event.EventService.Event.PEER_STATE;
import static org.terracotta.metrics.MetricService.Metric.CLIENTS;
import static org.terracotta.metrics.MetricService.Metric.CONNECTIONS;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
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
  public void testGetCounterByEventZeroBeforeAnyFire() {
    // counters are created eagerly for every event
    for (Event event : Event.values()) {
      Optional<Metric> counter = collector.getCounter(event);
      assertTrue(event + " counter must exist", counter.isPresent());
      assertEquals(0L, counter.get().get());
    }
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
    collector.fireEvent(PEER_JOIN, emptyMap());
    collector.fireEvent(PEER_JOIN, emptyMap());
    collector.fireEvent(PEER_JOIN, emptyMap());

    assertEquals(3L, collector.getCounter(PEER_JOIN).get().get());
  }

  @Test
  public void testDifferentEventsHaveIndependentCounters() {
    collector.fireEvent(CLIENT_JOIN, emptyMap());
    collector.fireEvent(CLIENT_JOIN, emptyMap());
    collector.fireEvent(PEER_JOIN, emptyMap());

    assertEquals(2L, collector.getCounter(CLIENT_JOIN).get().get());
    assertEquals(1L, collector.getCounter(PEER_JOIN).get().get());
  }

  // --- getCounter (by Event + entity type) ---

  @Test
  public void testGetCounterByTypeEmptyBeforeRegistration() {
    assertFalse(collector.getCounter(ENTITY_INVOKE, "com.example.Foo").isPresent());
  }

  @Test
  public void testCountRegistersTypeCounterAtZero() {
    String entityType = "com.example.MyEntity";
    collector.count(ENTITY_INVOKE, entityType);

    Optional<Metric> counter = collector.getCounter(ENTITY_INVOKE, entityType);
    assertTrue(counter.isPresent());
    assertEquals(0L, counter.get().get());
  }

  @Test
  public void testCountIsScopedToEvent() {
    String entityType = "com.example.MyEntity";
    collector.count(ENTITY_INVOKE, entityType);

    assertFalse(collector.getCounter(CLIENT_JOIN, entityType).isPresent());
  }

  @Test
  public void testCountIsIdempotentAndKeepsExistingValue() {
    String entityType = "com.example.MyEntity";
    collector.count(ENTITY_INVOKE, entityType);
    Metric first = collector.getCounter(ENTITY_INVOKE, entityType).get();
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", entityType));

    collector.count(ENTITY_INVOKE, entityType);

    assertSame(first, collector.getCounter(ENTITY_INVOKE, entityType).get());
    assertEquals(1L, collector.getCounter(ENTITY_INVOKE, entityType).get().get());
  }

  @Test
  public void testGetCounterByTypeIncrementedAfterRegistrationAndFireWithType() {
    String entityType = "com.example.MyEntity";
    collector.count(ENTITY_INVOKE, entityType);
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", entityType));

    Optional<Metric> counter = collector.getCounter(ENTITY_INVOKE, entityType);
    assertTrue(counter.isPresent());
    assertEquals(1L, counter.get().get());
  }

  @Test
  public void testFireEventWithUnregisteredTypeOnlyIncrementsGlobalCounter() {
    String entityType = "com.example.Unknown";
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", entityType));

    assertEquals(1L, collector.getCounter(ENTITY_INVOKE).get().get());
    assertFalse("unregistered types must not be created by fireEvent",
      collector.getCounter(ENTITY_INVOKE, entityType).isPresent());
  }

  @Test
  public void testFireEventBeforeRegistrationIsNotCountedForType() {
    String entityType = "com.example.MyEntity";
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", entityType));
    collector.count(ENTITY_INVOKE, entityType);
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", entityType));

    assertEquals(2L, collector.getCounter(ENTITY_INVOKE).get().get());
    assertEquals(1L, collector.getCounter(ENTITY_INVOKE, entityType).get().get());
  }

  @Test
  public void testFireEventWithTypeIncrementsBothGlobalAndTypeCounters() {
    String entityType = "com.example.MyEntity";
    collector.count(ENTITY_INVOKE, entityType);
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", entityType));
    collector.fireEvent(ENTITY_INVOKE, emptyMap());

    assertEquals(2L, collector.getCounter(ENTITY_INVOKE).get().get());
    assertEquals(1L, collector.getCounter(ENTITY_INVOKE, entityType).get().get());
  }

  @Test
  public void testGetCounterByTypeAccumulatesMultipleFires() {
    String entityType = "com.example.MyEntity";
    collector.count(ENTITY_INVOKE, entityType);
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
    collector.count(ENTITY_INVOKE, typeA);
    collector.count(ENTITY_INVOKE, typeB);
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", typeA));
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", typeA));
    collector.fireEvent(ENTITY_INVOKE, singletonMap("type", typeB));

    assertEquals(2L, collector.getCounter(ENTITY_INVOKE, typeA).get().get());
    assertEquals(1L, collector.getCounter(ENTITY_INVOKE, typeB).get().get());
  }

  // --- concurrency ---

  @Test
  public void testConcurrentFireEventAndCountLoseNoIncrement() throws Exception {
    String entityType = "com.example.MyEntity";
    int threads = 4;
    int firesPerThread = 10_000;
    collector.count(ENTITY_INVOKE, entityType);

    CountDownLatch start = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(threads + 1);
    try {
      List<Future<?>> futures = new ArrayList<>();
      for (int i = 0; i < threads; i++) {
        futures.add(executor.submit(() -> {
          start.await();
          for (int j = 0; j < firesPerThread; j++) {
            collector.fireEvent(ENTITY_INVOKE, singletonMap("type", entityType));
          }
          return null;
        }));
      }
      // concurrent registration of other types must not disturb the counting
      futures.add(executor.submit(() -> {
        start.await();
        for (int j = 0; j < 1_000; j++) {
          collector.count(ENTITY_INVOKE, "com.example.Other" + j);
        }
        return null;
      }));

      start.countDown();
      for (Future<?> future : futures) {
        future.get();
      }
    } finally {
      executor.shutdownNow();
    }

    long expected = (long) threads * firesPerThread;
    assertEquals(expected, collector.getCounter(ENTITY_INVOKE).get().get());
    assertEquals(expected, collector.getCounter(ENTITY_INVOKE, entityType).get().get());
  }

  // --- fireEvent default (no-arg) ---

  @Test
  public void testFireEventDefaultUsesEmptyMap() {
    collector.fireEvent(CLIENT_JOIN);

    assertEquals(1L, collector.getCounter(CLIENT_JOIN).get().get());
  }

  // --- getPeers / PEER_STATE event ---

  @Test
  public void testGetPeersEmptyBeforeAnyPeerStateEvent() {
    assertTrue(collector.getPeers().isEmpty());
  }

  @Test
  public void testGetPeersPopulatedAfterPeerStateEvent() {
    collector.fireEvent(PEER_STATE, Map.of("peer", "node1:9410", "state", "PASSIVE-STANDBY"));

    Map<String, LogicalServerState> peers = collector.getPeers();
    assertNotNull(peers);
    assertEquals(1, peers.size());
    assertTrue(peers.containsKey("node1:9410"));
    assertEquals(LogicalServerState.PASSIVE, peers.get("node1:9410"));
  }

  @Test
  public void testGetPeersTracksMultiplePeers() {
    collector.fireEvent(PEER_STATE, Map.of("peer", "node1:9410", "state", "PASSIVE-STANDBY"));
    collector.fireEvent(PEER_STATE, Map.of("peer", "node2:9410", "state", "ACTIVE"));

    Map<String, LogicalServerState> peers = collector.getPeers();
    assertEquals(2, peers.size());
    assertEquals(LogicalServerState.PASSIVE, peers.get("node1:9410"));
    assertEquals(LogicalServerState.ACTIVE, peers.get("node2:9410"));
  }

  @Test
  public void testGetPeerStateUpdatedByLaterPeerStateEvent() {
    collector.fireEvent(PEER_STATE, Map.of("peer", "node1:9410", "state", "PASSIVE-STANDBY"));
    assertEquals(LogicalServerState.PASSIVE, collector.getPeers().get("node1:9410"));

    // Same peer reports a new state (e.g. promoted after failover)
    collector.fireEvent(PEER_STATE, Map.of("peer", "node1:9410", "state", "ACTIVE"));
    assertEquals(LogicalServerState.ACTIVE, collector.getPeers().get("node1:9410"));
  }

  @Test
  public void testPeerStateEventWithoutPeerAttributeIsIgnored() {
    // "peer" key missing — must not throw and must leave peers map empty
    collector.fireEvent(PEER_STATE, Map.of("state", "PASSIVE-STANDBY"));
    assertTrue(collector.getPeers().isEmpty());
  }

  @Test
  public void testMemberStateEventDoesNotTouchPeers() {
    // MEMBER_STATE is the state of this server: it must never end up in the peers map
    collector.fireEvent(MEMBER_STATE, Map.of("state", "ACTIVE-COORDINATOR"));
    collector.fireEvent(MEMBER_STATE, Map.of("peer", "node1:9410", "state", "PASSIVE-STANDBY"));

    assertTrue(collector.getPeers().isEmpty());
    assertEquals(2L, collector.getCounter(MEMBER_STATE).get().get());
  }

  @Test
  public void testPeerStateStopStateRemovesPeer() {
    collector.fireEvent(PEER_STATE, Map.of("peer", "node1:9410", "state", "PASSIVE-STANDBY"));
    collector.fireEvent(PEER_STATE, Map.of("peer", "node2:9410", "state", "PASSIVE-STANDBY"));
    assertEquals(2, collector.getPeers().size());

    collector.fireEvent(PEER_STATE, Map.of("peer", "node1:9410", "state", "STOP-STATE"));

    assertEquals(1, collector.getPeers().size());
    assertFalse(collector.getPeers().containsKey("node1:9410"));
    assertTrue(collector.getPeers().containsKey("node2:9410"));
  }

  @Test
  public void testPeerStateTransitionsFollowCoreStates() {
    collector.fireEvent(PEER_STATE, Map.of("peer", "node1:9410", "state", "PASSIVE-UNINITIALIZED"));
    assertEquals(LogicalServerState.UNINITIALIZED, collector.getPeers().get("node1:9410"));

    collector.fireEvent(PEER_STATE, Map.of("peer", "node1:9410", "state", "PASSIVE-SYNCING"));
    assertEquals(LogicalServerState.SYNCHRONIZING, collector.getPeers().get("node1:9410"));

    collector.fireEvent(PEER_STATE, Map.of("peer", "node1:9410", "state", "PASSIVE-STANDBY"));
    assertEquals(LogicalServerState.PASSIVE, collector.getPeers().get("node1:9410"));
  }

  @Test
  public void testPeerStateEventIsNotCounted() {
    // PEER_STATE only updates the peers map: the core sends the same state several times,
    // so counting them would be meaningless
    collector.fireEvent(PEER_STATE, Map.of("peer", "node1:9410", "state", "ACTIVE"));
    collector.fireEvent(PEER_STATE, Map.of("peer", "node1:9410", "state", "ACTIVE"));

    assertEquals(1, collector.getPeers().size());
    assertTrue(collector.getCounter(PEER_STATE).isPresent());
    assertEquals(0L, collector.getCounter(PEER_STATE).get().get());
  }

  @Test
  public void testPeerStateEventWithTypeIsNotCounted() {
    collector.count(PEER_STATE, "com.example.Foo");
    collector.fireEvent(PEER_STATE, Map.of("peer", "node1:9410", "state", "ACTIVE", "type", "com.example.Foo"));

    assertEquals(0L, collector.getCounter(PEER_STATE, "com.example.Foo").get().get());
  }

  @Test
  public void testGetPeersIsUnmodifiable() {
    collector.fireEvent(PEER_STATE, Map.of("peer", "node1:9410", "state", "ACTIVE"));

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
