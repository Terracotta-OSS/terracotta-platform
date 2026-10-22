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

import org.terracotta.diagnostic.model.LogicalServerState;
import org.terracotta.event.EventService;
import org.terracotta.metrics.MetricService;
import org.terracotta.platform.metrics.metric.Metric;

import java.util.Map;
import java.util.Optional;

/**
 * Low-level interface used by {@link org.terracotta.platform.metrics.platform.DefaultPlatformMetricService}
 * to retrieve raw metrics from the Voltron event/supplier layer.
 *
 * <p>{@link org.terracotta.platform.metrics.voltron.DefaultVoltronMetricCollector} is the default
 * implementation, which also implements {@link MetricService} (gauges) and {@link EventService}
 * (events) so it can be registered directly with the Voltron service provider registry.
 */
public interface VoltronMetricCollector {

  /**
   * Registers an entity {@code type} to be counted for the given {@code event}.
   *
   * <p>Per-type counters are not created lazily when events are fired (the fire path must stay
   * lock-free). Only the types registered through this method are counted by
   * {@link EventService#fireEvent(EventService.Event, java.util.Map)} and returned by
   * {@link #getCounter(EventService.Event, String)}. Events carrying an unregistered type
   * still increment the global counter of the event.
   *
   * <p>This method is idempotent and thread-safe: registering an already registered type keeps
   * the existing counter and its value. It should be called before the events start flowing,
   * since events fired before the registration are not counted for that type.
   *
   * @param event the event type
   * @param type  the entity class name (value of the {@code "type"} attribute passed to {@code fireEvent})
   */
  void count(EventService.Event event, String type);

  /**
   * Returns the {@link Metric} (a {@link org.terracotta.platform.metrics.metric.Gauge}) currently
   * registered for {@code metric}, or an empty optional if no supplier has been registered yet.
   *
   * @param metric the gauge type to look up
   * @return current gauge, or empty
   */
  Optional<Metric> getGauge(MetricService.Metric metric);

  /**
   * Returns the {@link Metric} (a {@link org.terracotta.platform.metrics.metric.Counter}) that
   * accumulates occurrences of {@code event}, or an empty optional if the event is not tracked.
   *
   * <p>{@link org.terracotta.platform.metrics.voltron.DefaultVoltronMetricCollector} tracks every
   * event from construction, so its counters are always present and start at zero.
   *
   * @param event the event type to look up
   * @return accumulated counter, or empty
   */
  Optional<Metric> getCounter(EventService.Event event);

  /**
   * Returns the {@link Metric} (a {@link org.terracotta.platform.metrics.metric.Counter}) that
   * accumulates occurrences of {@code event} fired with the given entity {@code type} attribute,
   * or an empty optional if that type was not registered with {@link #count(EventService.Event, String)}.
   *
   * <p>Entity type counters are incremented by {@link EventService#fireEvent(EventService.Event, java.util.Map)}
   * calls that carry a {@code "type"} attribute (e.g. {@code "org.ehcache.clustered.client.internal.store.InternalClusterTierClientEntity"})
   * matching a registered type.
   * The {@code type} parameter here must match that attribute value exactly.
   *
   * @param event the event type to look up
   * @param type  the entity class name (value of the {@code "type"} attribute passed to {@code fireEvent})
   * @return accumulated counter for that event+type combination, or empty
   */
  Optional<Metric> getCounter(EventService.Event event, String type);

  /**
   * Returns a snapshot of the last known {@link LogicalServerState} for each peer server in
   * the stripe, keyed by {@code "hostname:port"}.
   *
   * <p>The map is populated by {@link EventService.Event#PEER_STATE} events: each event
   * carries a {@code "peer"} attribute (the remote server's {@code hostname:port}) and a
   * {@code "state"} attribute (a raw state string parseable by
   * {@link LogicalServerState#parse(String)}).  A later event for the same peer overwrites
   * the previous entry, so the map always reflects the most recent reported state. A peer
   * reporting {@code STOP-STATE} (see {@link LogicalServerState#STOPPING}) is removed.
   * {@link EventService.Event#MEMBER_STATE} is not used here: it describes this server only.
   *
   * <p>The core fires {@code PEER_STATE} on the active only, so this map is empty on a passive.
   *
   * <p>The returned map is unmodifiable.
   *
   * @return unmodifiable map of peer address → last known logical state (never {@code null})
   */
  Map<String, LogicalServerState> getPeers();
}
