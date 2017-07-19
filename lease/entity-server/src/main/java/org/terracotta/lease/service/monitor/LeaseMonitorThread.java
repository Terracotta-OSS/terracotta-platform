/*
 * Copyright Terracotta, Inc.
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
package org.terracotta.lease.service.monitor;

import org.terracotta.lease.TimeSource;

/**
 * A thread that periodically triggers a check for expired leases. Interrupting the thread permanently stops the checks
 * and the thread dies.
 */
public class LeaseMonitorThread extends Thread {
  private static final long LEASE_CHECK_INTERVAL_MILLIS = 200L;

  private final TimeSource timeSource;
  private final LeaseState leaseState;

  public LeaseMonitorThread(TimeSource timeSource, LeaseState leaseState) {
    this.timeSource = timeSource;
    this.leaseState = leaseState;
    setName("LeaseMonitorThread");
    setDaemon(true);
  }

  @Override
  public void run() {
    while (!Thread.interrupted()) {
      leaseState.checkLeases();

      try {
        timeSource.sleep(LEASE_CHECK_INTERVAL_MILLIS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      }
    }
  }
}
