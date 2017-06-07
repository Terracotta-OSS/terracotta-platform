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

/**
 * Allows control over what TimeSource is used.
 */
public class TimeSourceProvider {
  private static TimeSource timeSource = new SystemTimeSource();

  private TimeSourceProvider() {
  }

  public static synchronized TimeSource getTimeSource() {
    return timeSource;
  }

  public static synchronized void setTimeSource(TimeSource timeSource) {
    TimeSourceProvider.timeSource = timeSource;
  }
}
