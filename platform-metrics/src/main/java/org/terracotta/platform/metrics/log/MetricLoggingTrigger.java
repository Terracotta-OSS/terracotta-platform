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

/**
 * Identifies what caused a metrics log line to be emitted.
 *
 * <p>The {@link #getCode()} single-character mnemonic appears in the {@code |T:} field of every
 * log line so that log processors can tell apart periodic statistics from event-triggered snapshots:
 * <pre>
 *   STATS:1700000000:300|T:P|...|P:...|S:...   ← PERIODIC trigger
 *   STATS:1700000123:123|T:T|...|P:...|S:...   ← TOPOLOGY_CHANGE trigger
 * </pre>
 */
public enum MetricLoggingTrigger {

  /** Normal timed emission at the configured period interval. */
  PERIODIC("P"),
  /** Triggered immediately when this server or a peer changes state ({@code MEMBER_STATE} / {@code PEER_STATE}). */
  TOPOLOGY_CHANGE("T"),
  /** Triggered on demand (e.g. via a management operation). */
  ON_DEMAND("D"),
  ;

  private final String code;

  MetricLoggingTrigger(String code) {
    this.code = code;
  }

  /**
   * Returns the single-character mnemonic embedded in log lines to identify this trigger.
   *
   * @return single-character trigger code
   */
  public String getCode() {
    return code;
  }
}

// Made with Bob
