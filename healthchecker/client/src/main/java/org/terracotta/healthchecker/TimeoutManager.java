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
package org.terracotta.healthchecker;

/**
 * The manager of the health check on a connection.
 *
 */
public interface TimeoutManager {
  /**
   * A listener which is fired when something is wrong on a connection.
   * @param timeout The listener to be advised when something is wrong on a connection
   * @return true if the listener was added.  If the return is false, no listener was added
   *   possibly because the connection is no longer valid.
   */
  boolean addTimeoutListener(TimeoutListener timeout);
  /**
   * Check if the connection is still valid
   * @return true if the connection is valid
   */
  boolean isConnected();
}
