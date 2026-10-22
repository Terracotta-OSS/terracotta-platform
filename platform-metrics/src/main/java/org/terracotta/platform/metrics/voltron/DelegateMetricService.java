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

import java.util.Map;
import java.util.function.LongSupplier;

/**
 * A delegating wrapper around a {@link MetricService}.
 *
 * <p>Subclasses can override individual methods to intercept or augment specific events
 * while forwarding everything else to the wrapped service.
 */
public class DelegateMetricService implements MetricService {
  /** The underlying {@link MetricService} that all calls are forwarded to. */
  protected final MetricService metricService;

  /**
   * Creates a delegate that forwards all calls to {@code metricService}.
   *
   * @param metricService the service to delegate to (must not be {@code null})
   */
  public DelegateMetricService(MetricService metricService) {
    this.metricService = metricService;
  }

  @Override
  public void fireEvent(Event event, Map<String, String> map) {
    metricService.fireEvent(event, map);
  }

  @Override
  public void addMetricSupplier(Metric metric, LongSupplier longSupplier) {
    metricService.addMetricSupplier(metric, longSupplier);
  }

  @Override
  public void removeMetricSupplier(Metric metric) {
    metricService.removeMetricSupplier(metric);
  }

  @Override
  public String toString() {
    return "Delegate of: " + metricService;
  }
}
