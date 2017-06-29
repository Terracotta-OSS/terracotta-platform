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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

class ThreadCleaningLeaseMaintainer implements LeaseMaintainer {
  private static final Logger LOGGER = LoggerFactory.getLogger(ThreadCleaningLeaseMaintainer.class);

  private final LeaseMaintainer delegate;
  private final List<Thread> threads;

  ThreadCleaningLeaseMaintainer(LeaseMaintainer delegate, Thread... threads) {
    this(delegate, Arrays.asList(threads));
  }

  private ThreadCleaningLeaseMaintainer(LeaseMaintainer delegate, List<Thread> threads) {
    this.delegate = delegate;
    this.threads = threads;
  }

  @Override
  public Lease getCurrentLease() {
    return delegate.getCurrentLease();
  }

  @Override
  public void close() throws IOException {
    for (Thread thread : threads) {
      thread.interrupt();
    }

    for (Thread thread : threads) {
      try {
        thread.join();
      } catch (InterruptedException e) {
        LOGGER.error("Interrupted while shutting down Thread: " + thread, e);
      }
    }

    delegate.close();
  }
}
