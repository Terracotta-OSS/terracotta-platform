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

import org.terracotta.diagnostic.model.LogicalServerState;
import org.terracotta.platform.metrics.platform.PlatformMetricService;

import java.util.stream.Collectors;

/**
 * Formats metrics into the pipe-delimited log format.
 *
 * <p>Output format:
 * <pre>
 *   STATS:{unix}:{seconds}|T:{trigger}|{topology}|P:xxxx,+xx,-xx|S:xxxx,+xx,-xx|V:xxxx,+xx,-xx|C:xxxx,+xx,-xx|F:xxxx,+xx,-xx|EIC:+xx|CIC:+xx|SIC:+xx|QIC:+xx
 * </pre>
 *
 * <p>Field layout (pipe-split index):
 * <ul>
 *   <li>[0] {@code STATS:{unix}:{seconds}} — Unix timestamp and seconds since the last line</li>
 *   <li>[1] {@code T:{code}} — trigger code (P, T, F, D)</li>
 *   <li>[2] topology — comma-separated server-state codes ordered by hostname:port</li>
 *   <li>[3] {@code P} — physical connections: gauge / +opened / -closed</li>
 *   <li>[4] {@code S} — peer servers: gauge / +joined / -left</li>
 *   <li>[5] {@code V} — voters: gauge / +joined / -left</li>
 *   <li>[6] {@code C} — clients: gauge / +joined / -left</li>
 *   <li>[7] {@code F} — entity fetches: gauge / +fetched / -released</li>
 *   <li>[8] {@code EIC} — total entity invocations delta</li>
 *   <li>[9] {@code CIC} — cache entity invocations delta</li>
 *   <li>[10] {@code SIC} — store entity invocations delta</li>
 *   <li>[11] {@code QIC} — sequence entity invocations delta</li>
 * </ul>
 *
 * <p>Number widths: gauges minimum 4 digits; deltas minimum 3 characters total (sign + 2 digits).
 * Fields grow automatically when values exceed the minimum digit count.
 */
class MetricFormatter {
  private static final int MIN_GAUGE_WIDTH = 4;
  private static final int MIN_DELTA_WIDTH = 3;

  /**
   * Format metrics into a log line.
   *
   * @return formatted log line
   */
  String format(MetricLoggingTrigger trigger, long timestamp, long secondsSinceLast, PlatformMetricService platformMetricService) {
    StringBuilder sb = new StringBuilder();

    // Header: STATS:<unix>:<seconds>
    sb.append("STATS")
      .append(":").append(timestamp)
      .append(":").append(secondsSinceLast);

    // Trigger:
    sb.append("|T:").append(trigger.getCode());

    // Topology
    sb.append("|").append(platformMetricService.getStripeStates()
      .values()
      .stream()
      .map(MetricFormatter::toCode)
      .collect(Collectors.joining(",")));

    // Meters are only reset by periodic lines: other triggers must not alter the periodic deltas
    boolean reset = trigger == MetricLoggingTrigger.PERIODIC;

    // Metrics
    appendMetric(sb, "P",
      platformMetricService.getCurrentConnectionCount(),
      platformMetricService.getMeteredConnectionOpened(reset),
      platformMetricService.getMeteredConnectionClosed(reset));

    appendMetric(sb, "S",
      platformMetricService.getCurrentPeerCount(),
      platformMetricService.getMeteredServerJoins(reset),
      platformMetricService.getMeteredServerLeaves(reset));

    appendMetric(sb, "V",
      platformMetricService.getCurrentVoterCount(),
      platformMetricService.getMeteredVoterJoins(reset),
      platformMetricService.getMeteredVoterLeaves(reset));

    appendMetric(sb, "C",
      platformMetricService.getCurrentClientCount(),
      platformMetricService.getMeteredClientJoins(reset),
      platformMetricService.getMeteredClientLeaves(reset));

    appendMetric(sb, "F",
      platformMetricService.getCurrentEntityFetchCount(),
      platformMetricService.getMeteredEntityFetches(reset),
      platformMetricService.getMeteredEntityReleases(reset));

    // Meter-only metrics
    appendMeter(sb, "EIC", platformMetricService.getMeteredTotalEntityInvocations(reset));
    appendMeter(sb, "CIC", platformMetricService.getMeteredCacheEntityInvocations(reset));
    appendMeter(sb, "SIC", platformMetricService.getMeteredStoreEntityInvocations(reset));
    appendMeter(sb, "QIC", platformMetricService.getMeteredSequenceEntityInvocations(reset));

    return sb.toString();
  }

  /**
   * Append a gauge metric with current value and deltas.
   */
  private void appendMetric(StringBuilder sb, String label, long current, long plus, long minus) {
    sb.append("|").append(label).append(":");

    // Determine width based on largest value
    int width = Math.max(MIN_GAUGE_WIDTH, String.valueOf(current).length());
    int deltaWidth = Math.max(MIN_DELTA_WIDTH,
      Math.max(String.valueOf(plus).length() + 1, String.valueOf(minus).length() + 1));

    sb.append(formatNumber(current, width, false))
      .append(",")
      .append(formatNumber(plus, deltaWidth, true))
      .append(",")
      .append(formatNumber(minus, deltaWidth, true, true));
  }

  /**
   * Append a meter-only metric (delta value).
   */
  private void appendMeter(StringBuilder sb, String label, long value) {
    sb.append("|").append(label).append(":");
    int width = Math.max(MIN_DELTA_WIDTH, String.valueOf(value).length() + 1);
    sb.append(formatNumber(value, width, true));
  }

  /**
   * Format a number with optional sign and zero-padding.
   */
  private String formatNumber(long value, int width, boolean withSign) {
    return formatNumber(value, width, withSign, false);
  }

  /**
   * Format a number with optional sign and zero-padding.
   *
   * @param value    the value to format
   * @param width    the minimum width
   * @param withSign whether to include a sign
   * @param negative whether this is a negative delta (use - instead of +)
   */
  private String formatNumber(long value, int width, boolean withSign, boolean negative) {
    String sign = "";
    if (withSign) {
      sign = negative ? "-" : "+";
    }
    String numStr = String.valueOf(value);
    int padding = width - sign.length() - numStr.length();
    return sign + "0".repeat(Math.max(0, padding)) + numStr;
  }

  private static String toCode(LogicalServerState state) {
    return switch (state) {
      case STARTING -> "S";
      case DIAGNOSTIC -> "D";
      case ACTIVE -> "A";
      case PASSIVE -> "P";
      case RELAY -> "T";
      case REPLICA -> "R";
      case ACTIVE_RECONNECTING -> "AR"; // note: not observable in reality by this system since reconnect state comes from a mBean
      case SYNCHRONIZING -> "PS";
      case START_SUSPENDED -> "SB"; // note: not observable in reality by this system since blocking state comes from consistency mBean
      case ACTIVE_SUSPENDED -> "AB"; // note: not observable in reality by this system since blocking state comes from consistency mBean
      case PASSIVE_SUSPENDED -> "PB"; // note: not observable in reality by this system since blocking state comes from consistency mBean
      case REPLICA_SUSPENDED -> "RB";
      case BOOTSTRAP -> "B";
      case RECOVERING -> "C";
      case STOPPING, UNREACHABLE, UNKNOWN, UNINITIALIZED -> "?";
    };
  }
}

// Made with Bob
