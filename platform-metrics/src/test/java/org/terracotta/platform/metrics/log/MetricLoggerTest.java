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
package org.terracotta.platform.metrics.log;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.terracotta.platform.metrics.platform.PlatformMetricService;

import static org.mockito.Mockito.mock;

/**
 * Unit tests for MetricLogger.
 */
public class MetricLoggerTest {

  private PlatformMetricService service;
  private MetricLogger logger;

  @Before
  public void setUp() {
    service = mock(PlatformMetricService.class);
    logger = new MetricLogger(service);
  }

  @After
  public void tearDown() {
    logger.stop();
  }

  // --- start/stop lifecycle ---

  @Test
  public void testStartAndStop() {
    logger.setLoggingPeriod(300);
    logger.start();
    logger.stop();
    // No exception expected
  }

  @Test
  public void testStartIsIdempotent() {
    logger.setLoggingPeriod(300);
    logger.start();
    logger.start(); // second call must not throw
    logger.stop();
  }

  @Test
  public void testStopBeforeStartIsIdempotent() {
    logger.setLoggingPeriod(300);
    logger.stop(); // should not throw
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSetLoggingPeriodZeroThrows() {
    logger.setLoggingPeriod(0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSetLoggingPeriodNegativeThrows() {
    logger.setLoggingPeriod(-1);
  }

  @Test(expected = IllegalStateException.class)
  public void testStartWithoutPeriodThrows() {
    // period not set (stays 0)
    logger.start();
  }

  // --- logNow ---

  @Test
  public void testLogNowBeforeStartDoesNotThrow() {
    logger.setLoggingPeriod(300);
    // logNow before start: scheduler not running yet; should not throw but is a no-op
    // (scheduler created eagerly in field init, so the task is just queued on the idle executor)
    logger.logNow(MetricLoggingTrigger.ON_DEMAND);
  }

  @Test
  public void testLogNowAfterStopDoesNotThrow() {
    logger.setLoggingPeriod(300);
    logger.start();
    logger.stop();
    // after stop the scheduler is shutdown; logNow must not throw
    logger.logNow(MetricLoggingTrigger.ON_DEMAND);
  }

  @Test
  public void testLogNowWithAllTriggers() throws InterruptedException {
    logger.setLoggingPeriod(300);
    logger.start();

    for (MetricLoggingTrigger trigger : MetricLoggingTrigger.values()) {
      logger.logNow(trigger); // must not throw
    }

    logger.stop();
  }

  // --- period setting ---

  @Test
  public void testSetLoggingPeriodPositive() {
    logger.setLoggingPeriod(1);
    logger.setLoggingPeriod(300);
    logger.setLoggingPeriod(Integer.MAX_VALUE);
    // No exception expected
  }

  @Test
  public void testSetLoggingPeriodWhileRunningReschedulesWithoutThrow() {
    logger.setLoggingPeriod(300);
    logger.start();
    // Changing the period while the logger is running must cancel the old task
    // and schedule a new one — even if the old task is already cancelled.
    logger.setLoggingPeriod(60);
    logger.setLoggingPeriod(120);
    logger.stop();
  }

  // --- stop terminates scheduler ---

  @Test(timeout = 20000)
  public void testStopTerminatesScheduler() {
    logger.setLoggingPeriod(300);
    logger.start();
    logger.stop();
    // If awaitTermination blocks forever the test timeout will catch it
  }

  // --- double stop ---

  @Test
  public void testDoubleStopIsIdempotent() {
    logger.setLoggingPeriod(300);
    logger.start();
    logger.stop();
    logger.stop(); // must not throw
  }
}

// Made with Bob
