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

import com.tc.text.PrettyPrintable;
import org.terracotta.diagnostic.model.LogicalServerState;
import org.terracotta.entity.StateDumpCollector;
import org.terracotta.entity.StateDumpable;
import org.terracotta.event.EventService;
import org.terracotta.metrics.MetricService;
import org.terracotta.platform.metrics.metric.Counter;
import org.terracotta.platform.metrics.metric.Gauge;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

/**
 * Implements Voltron's services: {@link MetricService} and {@link EventService}.
 * This class collects all registered voltron suppliers in gauges and all events in counters.
 *
 * <p>{@link #fireEvent(Event, Map)} is invoked from core hot paths (e.g. on every entity invocation),
 * so it is designed to be lock-free and allocation-free:
 * <ul>
 *   <li>One global {@link Counter} per {@link Event} is created eagerly in the constructor and never
 *   removed, so the {@link EnumMap} holding them is never structurally modified afterwards and can be
 *   read concurrently without synchronization.</li>
 *   <li>Per-entity-type counters are <strong>not</strong> created on demand by {@code fireEvent}: only the
 *   types registered beforehand with {@link #count(Event, String)} are counted. Events carrying an
 *   unregistered {@code "type"} still increment the global counter but are otherwise ignored.</li>
 * </ul>
 */
public class DefaultVoltronMetricCollector implements VoltronMetricCollector, MetricService, EventService, StateDumpable, PrettyPrintable {
  private final Map<Metric, Gauge> gauges = new ConcurrentHashMap<>(Metric.values().length);
  // fully populated in the constructor, never structurally modified afterwards
  private final EnumMap<Event, Counter> counters = new EnumMap<>(Event.class);
  // the outer map is fully populated in the constructor; inner maps can grow through count()
  private final EnumMap<Event, Map<String, Counter>> entityTypeCounters = new EnumMap<>(Event.class);
  private final Map<String, LogicalServerState> peers = new ConcurrentHashMap<>();

  public DefaultVoltronMetricCollector() {
    Stream.of(Event.values()).forEach(event -> {
      counters.put(event, new Counter());
      entityTypeCounters.put(event, new ConcurrentHashMap<>());
    });
  }

  @Override
  public void addMetricSupplier(Metric metric, LongSupplier longSupplier) {
    gauges.put(metric, new Gauge(longSupplier));
  }

  @Override
  public void removeMetricSupplier(Metric metric) {
    gauges.remove(metric);
  }

  /**
   * {@inheritDoc}
   *
   * <p>This method is called concurrently from a core hot path (entity invocations), so it must
   * never block and must stay as cheap as possible. It increments the global counter of
   * {@code event}, and, if {@code attributes} carries a {@code "type"} that was previously
   * registered with {@link #count(Event, String)}, the counter of that event and type.
   *
   * <p>{@link Event#PEER_STATE} is the exception: it is a state report, not an occurrence (the core
   * sends the same state several times), so it only updates the peers map and is not counted.
   */
  @Override
  public void fireEvent(Event event, Map<String, String> attributes) {
    if (event == Event.PEER_STATE) {
      String peer = attributes.get("peer");
      if (peer != null) {
        LogicalServerState state = LogicalServerState.parse(attributes.get("state"));
        if (state == LogicalServerState.STOPPING) {
          peers.remove(peer);
        } else {
          peers.put(peer, state);
        }
      }
    } else {
      // global counter (always present: created in the constructor for every event)
      counters.get(event).increment();

      // voltron also sends some event information about entity types.
      // Only the types registered with count() are tracked.
      String type = attributes.get("type");
      if (type != null) {
        Counter typeCounter = entityTypeCounters.get(event).get(type);
        if (typeCounter != null) {
          typeCounter.increment();
        }
      }
    }
  }

  @Override
  public Map<String, ?> getStateMap() {
    return Stream.of(gauges, counters).map(Map::entrySet).flatMap(Collection::stream).collect(toMap(e -> e.getKey().toString(), e -> e.getValue().get()));
  }

  @Override
  public void addStateTo(StateDumpCollector stateDumpCollector) {
    getStateMap().forEach(stateDumpCollector::addState);
  }

  @Override
  public void count(Event event, String type) {
    entityTypeCounters.get(event).computeIfAbsent(type, key -> new Counter());
  }

  @Override
  public Optional<org.terracotta.platform.metrics.metric.Metric> getGauge(Metric metric) {
    return Optional.ofNullable(gauges.get(metric));
  }

  @Override
  public Optional<org.terracotta.platform.metrics.metric.Metric> getCounter(Event event) {
    return Optional.ofNullable(counters.get(event));
  }

  @Override
  public Optional<org.terracotta.platform.metrics.metric.Metric> getCounter(Event event, String type) {
    return Optional.ofNullable(entityTypeCounters.get(event)).map(map -> map.get(type));
  }

  @Override
  public Map<String, LogicalServerState> getPeers() {
    return Collections.unmodifiableMap(peers);
  }
}
