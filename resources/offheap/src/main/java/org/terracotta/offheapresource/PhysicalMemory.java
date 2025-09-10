/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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

import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author cdennis
 */
class PhysicalMemory {

  private static final Logger LOGGER = LoggerFactory.getLogger(PhysicalMemory.class);

  private static final Method getTotalMemorySize = Optional.ofNullable(find("getTotalPhysicalMemorySize")).orElseGet(() -> find("getTotalMemorySize"));
  private static final Method getFreeMemorySize = Optional.ofNullable(find("getFreePhysicalMemorySize")).orElseGet(() -> find("getFreeMemorySize"));
  private static final Method getTotalSwapSpaceSize = find("getTotalSwapSpaceSize");
  private static final Method getFreeSwapSpaceSize = find("getFreeSwapSpaceSize");
  private static final Method getCommittedVirtualMemorySize = find("getCommittedVirtualMemorySize");

  public static Long totalMemory() {
    return invoke(getTotalMemorySize);
  }

  public static Long freeMemory() {
    return invoke(getFreeMemorySize);
  }

  public static Long totalSwapSpace() {
    return invoke(getTotalSwapSpaceSize);
  }

  public static Long freeSwapSpace() {
    return invoke(getFreeSwapSpaceSize);
  }

  public static Long ourCommittedVirtualMemory() {
    return invoke(getCommittedVirtualMemorySize);
  }

  private static Method find(String methodName) {
    Class<?> c = ManagementFactory.getOperatingSystemMXBean().getClass();
    try {
      Method method = c.getMethod(methodName);
      method.setAccessible(true);
      return method;
    } catch (NoSuchMethodException | RuntimeException e) {
      // Note: RuntimeException will catch InaccessibleObjectException for Java 9
      LOGGER.trace("Unable to find or access method '{}' from {}", methodName, c.toString());
      return null;
    }
  }

  private static Long invoke(Method method) {
    try {
      return method == null ? null : (Long) method.invoke(ManagementFactory.getOperatingSystemMXBean());
    } catch (IllegalAccessException e) {
      LOGGER.trace("Error invoking method '{}': {}", method, e.getMessage(), e);
      return null;
    } catch (InvocationTargetException e) {
      LOGGER.trace("Error invoking method '{}': {}", method, e.getTargetException().getMessage(), e.getTargetException());
      return null;
    }
  }
}
