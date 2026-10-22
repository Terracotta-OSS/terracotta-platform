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

import java.util.TreeMap;

/**
 * Provides access to platform-level server metrics collected from the Voltron layer.
 *
 * <p>Metrics are split into two categories:
 * <ul>
 *   <li><b>Gauges</b> – current snapshot values that can go up or down (e.g. current connection count).</li>
 *   <li><b>Meters</b> – deltas since the last reset (e.g. connections opened during a logging period).</li>
 * </ul>
 *
 * <p>The {@code getAndReset*} variants atomically return the accumulated delta and reset the meter's
 * snapshot to the current counter value, so the next call starts accumulating from zero again.
 */
public interface PlatformMetricService {

  // --- gauges (current snapshot values) ---

  /** Returns the current number of open physical connections to this server. */
  long getCurrentConnectionCount();

  /** Returns the current number of peer servers connected to this server. */
  long getCurrentServerCount();

  /** Returns the current number of voters connected to this server. */
  long getCurrentVoterCount();

  /** Returns the current number of clients connected to this server. */
  long getCurrentClientCount();

  /** Returns the current number of entity fetches (held references) on this server. */
  long getCurrentEntityFetchCount();

  // --- meters (delta since last reset, read-only) ---

  /** Returns the number of connection-open events since the last reset. */
  long getMeteredConnectionOpened();
  /** Returns the number of connection-close events since the last reset. */
  long getMeteredConnectionClosed();
  /** Returns the number of server-join events since the last reset. */
  long getMeteredServerJoins();
  /** Returns the number of server-leave events since the last reset. */
  long getMeteredServerLeaves();
  /** Returns the number of voter-join events since the last reset. */
  long getMeteredVoterJoins();
  /** Returns the number of voter-leave events since the last reset. */
  long getMeteredVoterLeaves();
  /** Returns the number of client-join events since the last reset. */
  long getMeteredClientJoins();
  /** Returns the number of client-leave events since the last reset. */
  long getMeteredClientLeaves();
  /** Returns the number of entity-fetch events since the last reset. */
  long getMeteredEntityFetches();
  /** Returns the number of entity-release events since the last reset. */
  long getMeteredEntityReleases();
  /** Returns the total number of entity-invocation events since the last reset. */
  long getMeteredTotalEntityInvocations();
  /** Returns the number of Ehcache entity-invocation events since the last reset. */
  long getMeteredCacheEntityInvocations();
  /** Returns the number of TC Store entity-invocation events since the last reset. */
  long getMeteredStoreEntityInvocations();
  /** Returns the number of TC Sequence entity-invocation events since the last reset. */
  long getMeteredSequenceEntityInvocations();

  // --- meters (atomic read-and-reset) ---

  /** Returns the number of connection-open events since the last reset, then resets the meter. */
  long getAndResetMeteredConnectionOpened();
  /** Returns the number of connection-close events since the last reset, then resets the meter. */
  long getAndResetMeteredConnectionClosed();
  /** Returns the number of server-join events since the last reset, then resets the meter. */
  long getAndResetMeteredServerJoins();
  /** Returns the number of server-leave events since the last reset, then resets the meter. */
  long getAndResetMeteredServerLeaves();
  /** Returns the number of voter-join events since the last reset, then resets the meter. */
  long getAndResetMeteredVoterJoins();
  /** Returns the number of voter-leave events since the last reset, then resets the meter. */
  long getAndResetMeteredVoterLeaves();
  /** Returns the number of client-join events since the last reset, then resets the meter. */
  long getAndResetMeteredClientJoins();
  /** Returns the number of client-leave events since the last reset, then resets the meter. */
  long getAndResetMeteredClientLeaves();
  /** Returns the number of entity-fetch events since the last reset, then resets the meter. */
  long getAndResetMeteredEntityFetches();
  /** Returns the number of entity-release events since the last reset, then resets the meter. */
  long getAndResetMeteredEntityReleases();
  /** Returns the total number of entity-invocation events since the last reset, then resets the meter. */
  long getAndResetMeteredTotalEntityInvocations();
  /** Returns the number of Ehcache entity-invocation events since the last reset, then resets the meter. */
  long getAndResetMeteredCacheEntityInvocations();
  /** Returns the number of TC Store entity-invocation events since the last reset, then resets the meter. */
  long getAndResetMeteredStoreEntityInvocations();
  /** Returns the number of TC Sequence entity-invocation events since the last reset, then resets the meter. */
  long getAndResetMeteredSequenceEntityInvocations();

  /**
   * Resets all meters to the current counter values without returning their deltas.
   * This is equivalent to calling every {@code getAndReset*} method and discarding the results.
   */
  void resetMeters();

  /**
   * Returns the current {@link LogicalServerState} for every server in the stripe, including
   * this server itself, as a {@link TreeMap} ordered alphabetically by {@code "hostname:port"}.
   *
   * <p>This server's own entry is always present; its state is read live from
   * {@link org.terracotta.server.Server#getState()} on every call.
   *
   * <p>Peer entries are populated from {@link com.tc.spi.metric.MetricService.Event#MEMBER_STATE}
   * events fired by the Voltron layer.  The map reflects the last reported state for each peer;
   * peers that have never sent a state event do not appear.
   *
   * @return a new {@link TreeMap} of server address → logical state (never {@code null})
   */
  TreeMap<String, LogicalServerState> getStripeStates();
}
