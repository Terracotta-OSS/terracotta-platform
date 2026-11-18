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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.terracotta.event.EventService;

/**
 * Unit tests for DefaultVoltronMetricCollector.
 */
public class MonitoringMetricCollectorTest {

  private MonitoringMetricCollector collector;

  @Before
  public void setUp() {
    collector = new MonitoringMetricCollector(null, null);
  }

  @Test
  public void testCounterExists() {
    Assert.assertNotNull(collector.getCounter(EventService.Event.CONNECTION_OPEN));
  }
}
