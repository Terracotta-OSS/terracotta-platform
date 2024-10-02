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

package org.terracotta.management.service.monitoring;

import org.slf4j.Logger;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

import static java.lang.invoke.MethodType.methodType;

/**
 * @author Mathieu Carbou
 */
class Utils {

  private static final boolean ASSERT = Boolean.getBoolean("terracotta.management.assert");
  private static final MethodHandle WARN;

  static {
    MethodHandles.Lookup lookup = MethodHandles.lookup();
    try {
      WARN = lookup.findVirtual(Logger.class, "warn", methodType(void.class, String.class, Object[].class));
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new AssertionError();
    }
  }

  static void warnOrAssert(Logger logger, String message, Object... args) {
    if (ASSERT) {
      FormattingTuple detailMessage = MessageFormatter.arrayFormat(message, args);
      Throwable throwable = detailMessage.getThrowable();
      throw throwable == null ?
          new AssertionError(detailMessage.getMessage()) :
          new AssertionError(detailMessage.getMessage(), throwable);
    } else {
      try {
        WARN.invokeExact(logger, message, args);
      } catch (Throwable throwable) {
        throw new AssertionError();
      }
    }

  }

}
