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

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.fail;

public class LeaseTestUtil {
  public static void waitForValidLease(LeaseMaintainer leaseMaintainer) throws Exception {
    long start = System.nanoTime();
    while (true) {
      long now = System.nanoTime();
      if (now - start > TimeUnit.NANOSECONDS.convert(60, TimeUnit.SECONDS)) {
        fail("Never got a valid lease");
      }

      Lease lease = leaseMaintainer.getCurrentLease();
      if (lease.isValidAndContiguous(lease)) {
        break;
      }

      Thread.sleep(100);
    }
  }
}
