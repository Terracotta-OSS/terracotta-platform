/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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

package org.terracotta.offheapresource;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Mathieu Carbou
 */
public class PhysicalMemoryTest {
  @Test
  public void invocations_do_not_fail() {
    int version = getVersion();
    if (version < 9) {
      assertNotNull(PhysicalMemory.totalPhysicalMemory());
      assertNotNull(PhysicalMemory.freePhysicalMemory());
      assertNotNull(PhysicalMemory.totalSwapSpace());
      assertNotNull(PhysicalMemory.freeSwapSpace());
      assertNotNull(PhysicalMemory.ourCommittedVirtualMemory());
    } else if (version == 17) {
      assertNotNull(PhysicalMemory.totalPhysicalMemory());
      assertNotNull(PhysicalMemory.freePhysicalMemory());
      assertNull(PhysicalMemory.totalSwapSpace());
      assertNull(PhysicalMemory.freeSwapSpace());
      assertNull(PhysicalMemory.ourCommittedVirtualMemory());
    } else {
      assertNull(PhysicalMemory.totalPhysicalMemory());
      assertNull(PhysicalMemory.freePhysicalMemory());
      assertNull(PhysicalMemory.totalSwapSpace());
      assertNull(PhysicalMemory.freeSwapSpace());
      assertNull(PhysicalMemory.ourCommittedVirtualMemory());
    }
  }

  private static int getVersion() {
    String version = System.getProperty("java.version");
    if (version.startsWith("1.")) {
      version = version.substring(2, 3);
    } else {
      int dot = version.indexOf(".");
      if (dot != -1) { version = version.substring(0, dot); }
    } return Integer.parseInt(version);
  }
}
