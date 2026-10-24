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

import com.tc.spi.metric.MetricService;
import com.tc.text.PrettyPrintable;
import org.terracotta.diagnostic.model.LogicalServerState;
import org.terracotta.entity.StateDumpCollector;
import org.terracotta.entity.StateDumpable;
import org.terracotta.platform.metrics.metric.Counter;
import org.terracotta.platform.metrics.metric.Gauge;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

/**
 * Implements Voltron's service: MetricService.
 * This class collects all registered voltron suppliers and events in gauges and counters.
 */
public class DefaultVoltronMetricCollector implements VoltronMetricCollector, MetricService, StateDumpable, PrettyPrintable {
  private final Map<Metric, Gauge> gauges = new ConcurrentHashMap<>(Metric.values().length);
  private final Map<Event, Counter> counters = new ConcurrentHashMap<>(Event.values().length);
  private final Map<Event, Map<String, Counter>> entityTypeCounters = new ConcurrentHashMap<>(3);
  private final Map<String, LogicalServerState> peers = new ConcurrentHashMap<>();

  @Override
  public void addMetricSupplier(Metric metric, LongSupplier longSupplier) {
    gauges.put(metric, new Gauge(longSupplier));
  }

  @Override
  public void removeMetricSupplier(Metric metric) {
    gauges.remove(metric);
  }

  @Override
  public void fireEvent(Event event, Map<String, String> attributes) {
    counters.computeIfAbsent(event, key -> new Counter()).increment();

    // voltron also sends some event information about entity types
    String type = attributes.get("type");
    if (type != null) {
      entityTypeCounters
        .computeIfAbsent(event, key -> new ConcurrentHashMap<>())
        .computeIfAbsent(type, key -> new Counter()).increment();
    }

    // handle peer's states
    if (event == Event.MEMBER_STATE) {
      String peer = attributes.get("peer");
      if (peer != null) {
        LogicalServerState state = LogicalServerState.parse(attributes.get("state"));
        if (state == LogicalServerState.STOPPING) {
          peers.remove(peer);
        } else {
          peers.put(peer, state);
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
