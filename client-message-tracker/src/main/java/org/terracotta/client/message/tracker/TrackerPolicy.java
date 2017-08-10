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
package org.terracotta.client.message.tracker;

import com.tc.classloader.CommonComponent;

/**
 * A {@link TrackerPolicy} for an entity defines which objects need once and only once invocation guarantee.
 * This will be used by the {@link ClientTracker} to track such messages with their response cached.
 */
@CommonComponent
@FunctionalInterface
public interface TrackerPolicy {

  /**
   * Indicates whether the specified object needs to be tracked by {@link ClientTracker} or not.
   *
   * @param object an entity object
   * @return true for objects requiring tracking, false otherwise
   */
  boolean trackable(Object object);
}
