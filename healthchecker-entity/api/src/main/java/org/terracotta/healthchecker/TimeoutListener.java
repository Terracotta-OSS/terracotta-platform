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
package org.terracotta.healthchecker;

import org.terracotta.connection.Connection;

/**
 *
 */
public interface TimeoutListener {
  /**
   * Called by the TimeoutManager when a connection is closed due to a failure 
   * in the connection
   * @param target The connection that was closed
   */
  void connectionClosed(Connection target);
  /**
   * A probe has failed to return to the server. This is just advisory.  The timeout
   * of the health check on the connection has not been reached.
   * @param target The connection on which the probe failed
   */
  void probeFailed(Connection target);
}
