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

import com.tc.spi.metric.MetricService;
import org.terracotta.diagnostic.model.LogicalServerState;
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

import static com.tc.spi.metric.MetricService.Event.CLIENT_JOIN;
import static com.tc.spi.metric.MetricService.Event.CLIENT_LEAVE;
import static com.tc.spi.metric.MetricService.Event.CONNECTION_CLOSE;
import static com.tc.spi.metric.MetricService.Event.CONNECTION_OPEN;
import static com.tc.spi.metric.MetricService.Event.ENTITY_FETCH;
import static com.tc.spi.metric.MetricService.Event.ENTITY_INVOKE;
import static com.tc.spi.metric.MetricService.Event.ENTITY_RELEASE;
import static com.tc.spi.metric.MetricService.Event.MEMBER_JOIN;
import static com.tc.spi.metric.MetricService.Event.MEMBER_LEAVE;
import static com.tc.spi.metric.MetricService.Event.VOTER_JOIN;
import static com.tc.spi.metric.MetricService.Event.VOTER_LEAVE;
import static com.tc.spi.metric.MetricService.Metric.CLIENTS;
import static com.tc.spi.metric.MetricService.Metric.CONNECTIONS;
import static com.tc.spi.metric.MetricService.Metric.ENTITY_FETCHES;
import static com.tc.spi.metric.MetricService.Metric.MEMBERS;
import static com.tc.spi.metric.MetricService.Metric.VOTERS;

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
  private final Map<MetricService.Event, MeteredCounter> meters = new ConcurrentHashMap<>();
  private final Map<MetricService.Event, Map<String, MeteredCounter>> entityTypeMeters = new ConcurrentHashMap<>();

  public DefaultPlatformMetricService(Server server, VoltronMetricCollector collector) {
    this.server = server;
    this.collector = collector;
  }

  // read gauges
  @Override
  public long getCurrentConnectionCount() { return getCounterValue(CONNECTIONS); }
  @Override
  public long getCurrentServerCount() { return getCounterValue(MEMBERS); }
  @Override
  public long getCurrentVoterCount() { return getCounterValue(VOTERS); }
  @Override
  public long getCurrentClientCount() { return getCounterValue(CLIENTS); }
  @Override
  public long getCurrentEntityFetchCount() { return getCounterValue(ENTITY_FETCHES); }

  // read meters
  @Override
  public long getMeteredConnectionOpened() { return getMeter(CONNECTION_OPEN); }
  @Override
  public long getMeteredConnectionClosed() { return getMeter(CONNECTION_CLOSE); }
  @Override
  public long getMeteredServerJoins() { return getMeter(MEMBER_JOIN); }
  @Override
  public long getMeteredServerLeaves() { return getMeter(MEMBER_LEAVE); }
  @Override
  public long getMeteredVoterJoins() { return getMeter(VOTER_JOIN); }
  @Override
  public long getMeteredVoterLeaves() { return getMeter(VOTER_LEAVE); }
  @Override
  public long getMeteredClientJoins() { return getMeter(CLIENT_JOIN); }
  @Override
  public long getMeteredClientLeaves() { return getMeter(CLIENT_LEAVE); }
  @Override
  public long getMeteredEntityFetches() { return getMeter(ENTITY_FETCH); }
  @Override
  public long getMeteredEntityReleases() { return getMeter(ENTITY_RELEASE); }
  @Override
  public long getMeteredTotalEntityInvocations() { return getMeter(ENTITY_INVOKE); }
  @Override
  public long getMeteredCacheEntityInvocations() { return Stream.of(CIC).mapToLong(type -> getMeter(ENTITY_INVOKE, type)).sum(); }
  @Override
  public long getMeteredStoreEntityInvocations() { return Stream.of(SIC).mapToLong(type -> getMeter(ENTITY_INVOKE, type)).sum(); }
  @Override
  public long getMeteredSequenceEntityInvocations() { return Stream.of(QIC).mapToLong(type -> getMeter(ENTITY_INVOKE, type)).sum(); }

  // read and reset meters
  @Override
  public long getAndResetMeteredConnectionOpened() { return getAndResetMeter(CONNECTION_OPEN); }
  @Override
  public long getAndResetMeteredConnectionClosed() { return getAndResetMeter(CONNECTION_CLOSE); }
  @Override
  public long getAndResetMeteredServerJoins() { return getAndResetMeter(MEMBER_JOIN); }
  @Override
  public long getAndResetMeteredServerLeaves() { return getAndResetMeter(MEMBER_LEAVE); }
  @Override
  public long getAndResetMeteredVoterJoins() { return getAndResetMeter(VOTER_JOIN); }
  @Override
  public long getAndResetMeteredVoterLeaves() { return getAndResetMeter(VOTER_LEAVE); }
  @Override
  public long getAndResetMeteredClientJoins() { return getAndResetMeter(CLIENT_JOIN); }
  @Override
  public long getAndResetMeteredClientLeaves() { return getAndResetMeter(CLIENT_LEAVE); }
  @Override
  public long getAndResetMeteredEntityFetches() { return getAndResetMeter(ENTITY_FETCH); }
  @Override
  public long getAndResetMeteredEntityReleases() { return getAndResetMeter(ENTITY_RELEASE); }
  @Override
  public long getAndResetMeteredTotalEntityInvocations() { return getAndResetMeter(ENTITY_INVOKE); }
  @Override
  public long getAndResetMeteredCacheEntityInvocations() { return Stream.of(CIC).mapToLong(type -> getAndResetMeter(ENTITY_INVOKE, type)).sum(); }
  @Override
  public long getAndResetMeteredStoreEntityInvocations() { return Stream.of(SIC).mapToLong(type -> getAndResetMeter(ENTITY_INVOKE, type)).sum(); }
  @Override
  public long getAndResetMeteredSequenceEntityInvocations() { return Stream.of(QIC).mapToLong(type -> getAndResetMeter(ENTITY_INVOKE, type)).sum(); }

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

  private Optional<MeteredCounter> findMeter(MetricService.Event event) {
    return Optional.ofNullable(meters.computeIfAbsent(
      event,
      key -> collector.getCounter(key).map(Counter.class::cast).map(MeteredCounter::new).orElse(null)));
  }

  private Optional<MeteredCounter> findMeter(MetricService.Event event, String type) {
    return Optional.ofNullable(entityTypeMeters
      .computeIfAbsent(event, key -> new ConcurrentHashMap<>())
      .computeIfAbsent(type, key -> collector.getCounter(event, key).map(Counter.class::cast).map(MeteredCounter::new).orElse(null)));
  }

  private long getMeter(MetricService.Event event) { return findMeter(event).stream().mapToLong(MeteredCounter::get).findFirst().orElse(0); }
  private long getMeter(MetricService.Event event, String type) { return findMeter(event, type).stream().mapToLong(MeteredCounter::get).findFirst().orElse(0); }
  private long getAndResetMeter(MetricService.Event event) { return findMeter(event).stream().mapToLong(MeteredCounter::getAndReset).findFirst().orElse(0); }
  private long getAndResetMeter(MetricService.Event event, String type) { return findMeter(event, type).stream().mapToLong(MeteredCounter::getAndReset).findFirst().orElse(0); }
}
