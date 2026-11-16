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

import org.terracotta.diagnostic.model.LogicalServerState;
import org.terracotta.event.EventService;
import org.terracotta.metrics.MetricService;
import org.terracotta.platform.metrics.metric.Counter;
import org.terracotta.platform.metrics.metric.MeteredCounter;
import org.terracotta.platform.metrics.metric.Metric;
import org.terracotta.platform.metrics.voltron.VoltronMetricCollector;
import org.terracotta.server.Server;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static org.terracotta.event.EventService.Event.CLIENT_JOIN;
import static org.terracotta.event.EventService.Event.CLIENT_LEAVE;
import static org.terracotta.event.EventService.Event.CONNECTION_CLOSE;
import static org.terracotta.event.EventService.Event.CONNECTION_OPEN;
import static org.terracotta.event.EventService.Event.ENTITY_FETCH;
import static org.terracotta.event.EventService.Event.ENTITY_INVOKE;
import static org.terracotta.event.EventService.Event.ENTITY_RELEASE;
import static org.terracotta.event.EventService.Event.PEER_JOIN;
import static org.terracotta.event.EventService.Event.PEER_LEAVE;
import static org.terracotta.event.EventService.Event.VOTER_JOIN;
import static org.terracotta.event.EventService.Event.VOTER_LEAVE;
import static org.terracotta.metrics.MetricService.Metric.CLIENTS;
import static org.terracotta.metrics.MetricService.Metric.CONNECTIONS;
import static org.terracotta.metrics.MetricService.Metric.ENTITY_FETCHES;
import static org.terracotta.metrics.MetricService.Metric.PEERS;
import static org.terracotta.metrics.MetricService.Metric.VOTERS;

/**
 * Default implementation of {@link PlatformMetricService}.
 *
 * <p>Translates the raw events and gauges from the Voltron layer (via {@link VoltronMetricCollector})
 * into the platform-level metrics exposed by {@link PlatformMetricService}.
 *
 * <p>Each meter is backed by a {@link org.terracotta.platform.metrics.metric.MeteredCounter} that
 * wraps a monotonically increasing {@link org.terracotta.platform.metrics.metric.Counter}.
 * Calling a {@code getAndReset*} method atomically captures the delta since the last reset and
 * advances the internal snapshot, so the next period starts accumulating from zero.
 *
 * <p>Entity-type invocations (Cache, Store, Sequence) are tracked via per-type counters keyed by
 * entity class name (the {@code "type"} attribute carried by {@code ENTITY_INVOKE} events).
 * The collector only counts the types it knows about, so the constructor registers every type
 * listed below with {@link VoltronMetricCollector#count(EventService.Event, String)}.
 * CIC sums {@code InternalClusterTierClientEntity} and {@code ClusterTierManagerClientEntity};
 * SIC sums {@code DatasetEntity}; QIC sums {@code SequenceEntity}.
 */
public class DefaultPlatformMetricService implements PlatformMetricService {
  // Cache Entity Types
  private static final String[] CIC = {
    "org.ehcache.clustered.client.internal.store.InternalClusterTierClientEntity",
    "org.ehcache.clustered.client.internal.ClusterTierManagerClientEntity",
  };
  // Store Entity Types
  private static final String[] SIC = {
    "com.terracottatech.store.client.DatasetEntity",
  };
  // Sequence Entity Types
  private static final String[] QIC = {
    "com.terracottatech.sequence.client.entity.SequenceEntity",
  };

  private final Server server;
  private final VoltronMetricCollector collector;
  private final Map<EventService.Event, MeteredCounter> meters = new ConcurrentHashMap<>();
  private final Map<EventService.Event, Map<String, MeteredCounter>> entityTypeMeters = new ConcurrentHashMap<>();

  /**
   * Creates the service and registers the entity types it reports on with the {@code collector}.
   * This must happen before the corresponding {@code ENTITY_INVOKE} events start flowing, since
   * events fired for a type before its registration are not counted for that type.
   *
   * @param server    the local server, used to report this server's own state
   * @param collector the raw metric store this service reads from
   */
  public DefaultPlatformMetricService(Server server, VoltronMetricCollector collector) {
    this.server = server;
    this.collector = collector;

    // register the entity types to count: the collector does not create type counters on the fire path
    Stream.of(CIC, SIC, QIC)
      .flatMap(Stream::of)
      .forEach(type -> collector.count(ENTITY_INVOKE, type));
  }

  // read gauges
  @Override
  public long getCurrentConnectionCount() { return getCounterValue(CONNECTIONS); }
  @Override
  public long getCurrentPeerCount() { return getCounterValue(PEERS); }
  @Override
  public long getCurrentVoterCount() { return getCounterValue(VOTERS); }
  @Override
  public long getCurrentClientCount() { return getCounterValue(CLIENTS); }
  @Override
  public long getCurrentEntityFetchCount() { return getCounterValue(ENTITY_FETCHES); }

  // read meters, optionally resetting them
  @Override
  public long getMeteredConnectionOpened(boolean reset) { return getMeter(CONNECTION_OPEN, reset); }
  @Override
  public long getMeteredConnectionClosed(boolean reset) { return getMeter(CONNECTION_CLOSE, reset); }
  @Override
  public long getMeteredServerJoins(boolean reset) { return getMeter(PEER_JOIN, reset); }
  @Override
  public long getMeteredServerLeaves(boolean reset) { return getMeter(PEER_LEAVE, reset); }
  @Override
  public long getMeteredVoterJoins(boolean reset) { return getMeter(VOTER_JOIN, reset); }
  @Override
  public long getMeteredVoterLeaves(boolean reset) { return getMeter(VOTER_LEAVE, reset); }
  @Override
  public long getMeteredClientJoins(boolean reset) { return getMeter(CLIENT_JOIN, reset); }
  @Override
  public long getMeteredClientLeaves(boolean reset) { return getMeter(CLIENT_LEAVE, reset); }
  @Override
  public long getMeteredEntityFetches(boolean reset) { return getMeter(ENTITY_FETCH, reset); }
  @Override
  public long getMeteredEntityReleases(boolean reset) { return getMeter(ENTITY_RELEASE, reset); }
  @Override
  public long getMeteredTotalEntityInvocations(boolean reset) { return getMeter(ENTITY_INVOKE, reset); }
  @Override
  public long getMeteredCacheEntityInvocations(boolean reset) { return Stream.of(CIC).mapToLong(type -> getMeter(ENTITY_INVOKE, type, reset)).sum(); }
  @Override
  public long getMeteredStoreEntityInvocations(boolean reset) { return Stream.of(SIC).mapToLong(type -> getMeter(ENTITY_INVOKE, type, reset)).sum(); }
  @Override
  public long getMeteredSequenceEntityInvocations(boolean reset) { return Stream.of(QIC).mapToLong(type -> getMeter(ENTITY_INVOKE, type, reset)).sum(); }

  // reset meters (they will be reset at their backed counter's current value)
  @Override
  public void resetMeters() {
    Stream.concat(
        meters.values().stream(),
        entityTypeMeters.values().stream().flatMap(m -> m.values().stream()))
      .forEach(MeteredCounter::getAndReset);
  }

  // topology
  @Override
  public TreeMap<String, LogicalServerState> getStripeStates() {
    TreeMap<String, LogicalServerState> servers = new TreeMap<>(collector.getPeers());
    servers.put(server.getServerHostName() + ":" + server.getServerPort(), LogicalServerState.parse(server.getState()));
    return servers;
  }

  private long getCounterValue(MetricService.Metric metric) {
    return collector.getGauge(metric).stream().mapToLong(Metric::get).findFirst().orElse(0);
  }

  private Optional<MeteredCounter> findMeter(EventService.Event event) {
    return Optional.ofNullable(meters.computeIfAbsent(
      event,
      key -> collector.getCounter(key).map(Counter.class::cast).map(MeteredCounter::new).orElse(null)));
  }

  private Optional<MeteredCounter> findMeter(EventService.Event event, String type) {
    return Optional.ofNullable(entityTypeMeters
      .computeIfAbsent(event, key -> new ConcurrentHashMap<>())
      .computeIfAbsent(type, key -> collector.getCounter(event, key).map(Counter.class::cast).map(MeteredCounter::new).orElse(null)));
  }

  private long getMeter(EventService.Event event, boolean reset) { return findMeter(event).stream().mapToLong(meter -> meter.get(reset)).findFirst().orElse(0); }
  private long getMeter(EventService.Event event, String type, boolean reset) { return findMeter(event, type).stream().mapToLong(meter -> meter.get(reset)).findFirst().orElse(0); }
}
