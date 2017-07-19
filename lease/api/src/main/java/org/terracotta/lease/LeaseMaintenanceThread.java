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
package org.terracotta.lease;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.exception.ConnectionClosedException;

class LeaseMaintenanceThread extends Thread {
  private static Logger LOGGER = LoggerFactory.getLogger(LeaseMaintenanceThread.class);

  private final LeaseMaintainerImpl leaseMaintainer;
  private final TimeSource timeSource;

  LeaseMaintenanceThread(LeaseMaintainerImpl leaseMaintainer) {
    this.leaseMaintainer = leaseMaintainer;
    this.timeSource = TimeSourceProvider.getTimeSource();
    setName("LeaseMaintenanceThread");
    setDaemon(true);
  }

  public void run() {
    while (!Thread.interrupted()) {
      try {
        long waitLength = leaseMaintainer.refreshLease();

        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Calculated lease refresh wait: " + waitLength);
        }

        if (waitLength > 0) {
          timeSource.sleep(waitLength);
        }
      } catch (ConnectionClosedException e) {
        return;
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      } catch (LeaseException e) {
        LOGGER.error("Error obtaining lease", e);
      }
    }
  }
}
