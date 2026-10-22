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
import org.terracotta.diagnostic.model.LogicalServerState;
import org.terracotta.platform.metrics.metric.Metric;

import java.util.Map;
import java.util.Optional;

/**
 * Low-level interface used by {@link org.terracotta.platform.metrics.platform.DefaultPlatformMetricService}
 * to retrieve raw metrics from the Voltron event/supplier layer.
 *
 * <p>{@link org.terracotta.platform.metrics.voltron.DefaultVoltronMetricCollector} is the default
 * implementation, which also implements {@link MetricService} so it can be registered directly
 * with the Voltron service provider registry.
 */
public interface VoltronMetricCollector {

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
   * accumulates occurrences of {@code event}, or an empty optional if no event has been fired yet.
   *
   * @param event the event type to look up
   * @return accumulated counter, or empty
   */
  Optional<Metric> getCounter(MetricService.Event event);

  /**
   * Returns the {@link Metric} (a {@link org.terracotta.platform.metrics.metric.Counter}) that
   * accumulates occurrences of {@code event} fired with the given entity {@code type} attribute,
   * or an empty optional if no such event has been fired yet.
   *
   * <p>Entity type counters are populated by {@link MetricService#fireEvent(MetricService.Event, java.util.Map)}
   * calls that carry a {@code "type"} attribute (e.g. {@code "org.ehcache.clustered.client.internal.store.InternalClusterTierClientEntity"}).
   * The {@code type} parameter here must match that attribute value exactly.
   *
   * @param event the event type to look up
   * @param type  the entity class name (value of the {@code "type"} attribute passed to {@code fireEvent})
   * @return accumulated counter for that event+type combination, or empty
   */
  Optional<Metric> getCounter(MetricService.Event event, String type);

  /**
   * Returns a snapshot of the last known {@link LogicalServerState} for each peer server in
   * the stripe, keyed by {@code "hostname:port"}.
   *
   * <p>The map is populated by {@link MetricService.Event#MEMBER_STATE} events: each event
   * carries a {@code "peer"} attribute (the remote server's {@code hostname:port}) and a
   * {@code "state"} attribute (a raw state string parseable by
   * {@link LogicalServerState#parse(String)}).  A later event for the same peer overwrites
   * the previous entry, so the map always reflects the most recent reported state.
   *
   * <p>The returned map is unmodifiable.
   *
   * @return unmodifiable map of peer address → last known logical state (never {@code null})
   */
  Map<String, LogicalServerState> getPeers();
}
