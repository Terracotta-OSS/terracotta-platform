/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2026
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.terracotta.platform.metrics.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.platform.metrics.platform.PlatformMetricService;
import org.terracotta.platform.metrics.util.DaemonThreadFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Schedules and executes metrics logging.
 * Uses non-fixed-rate scheduling: Execute → Collect → Reset → Log → Schedule Next
 * Also listens for topology/failover events to trigger immediate logging.
 */
public class MetricLogger {
  private static final Logger LOGGER = LoggerFactory.getLogger(MetricLogger.class);

  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory("metrics-logger"));
  private final MetricFormatter formatter = new MetricFormatter();

  private final PlatformMetricService platformMetricService;

  private volatile int periodSeconds;
  private volatile long lastLogTime;
  private volatile ScheduledFuture<?> periodicTask;

  public MetricLogger(PlatformMetricService platformMetricService) {
    this.platformMetricService = platformMetricService;
  }

  public synchronized void setLoggingPeriod(int periodSeconds) {
    if (periodSeconds <= 0) {
      throw new IllegalArgumentException("Invalid logging period: " + periodSeconds);
    }
    boolean changed = this.periodSeconds != periodSeconds;
    this.periodSeconds = periodSeconds;

    if (changed && periodicTask != null && !scheduler.isShutdown()) {
      reSchedulePeriodicLogging();
      LOGGER.info("Metrics logging period changed to: {} seconds", periodSeconds);
    }
  }

  /**
   * Start the metrics logger.
   */
  public synchronized void start() {
    if (periodicTask != null) {
      return;
    }
    if (periodSeconds <= 0) {
      throw new IllegalStateException("Invalid logging period: " + periodSeconds);
    }
    lastLogTime = System.currentTimeMillis() / 1000;
    reSchedulePeriodicLogging();
    LOGGER.info("Metrics logging started with period: {} seconds", periodSeconds);
  }

  private void reSchedulePeriodicLogging() {
    if (periodicTask != null && !periodicTask.isCancelled()) {
      periodicTask.cancel(false);
    }
    periodicTask = scheduler.scheduleWithFixedDelay(() -> logMetrics(MetricLoggingTrigger.PERIODIC), this.periodSeconds, this.periodSeconds, TimeUnit.SECONDS);
  }

  /**
   * Stop the metrics logger.
   */
  public synchronized void stop() {
    if (periodicTask == null) {
      return;
    }
    periodicTask.cancel(false);
    scheduler.shutdown();
    try {
      if (scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
        LOGGER.info("Metrics logging stopped");
      } else {
        LOGGER.error("Metrics logging failed to stop within 30 seconds");
        // Note: threads are daemon, so they won't stop process to shut down.
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Log metrics immediately (for events).
   * <p>
   * Note: logging is never done in the caller thread: instead, a request is queued for immediate execution
   *
   * @param trigger the trigger that caused this logging
   */
  public void logNow(MetricLoggingTrigger trigger) {
    if (scheduler.isShutdown()) {
      return;
    }
    try {
      scheduler.schedule(() -> logMetrics(trigger), 0, TimeUnit.SECONDS);
    } catch (RejectedExecutionException e) {
      // logger was stopped
    }
  }

  /**
   * Atomically collect metrics, reset deltas, and log.
   * No need to synchronized: only executed from this thread.
   */
  private void logMetrics(MetricLoggingTrigger trigger) {
    try {
      long currentTime = System.currentTimeMillis() / 1000;
      long secondsSinceLast = currentTime - lastLogTime;
      LOGGER.info(formatter.format(trigger, currentTime, secondsSinceLast, platformMetricService));
      lastLogTime = currentTime;
      if (trigger != MetricLoggingTrigger.PERIODIC) {
        LOGGER.debug("Metrics logged due to: {}", trigger);
      }
    } catch (Exception e) {
      LOGGER.error("Error logging metrics", e);
    }
  }
}

// Made with Bob
