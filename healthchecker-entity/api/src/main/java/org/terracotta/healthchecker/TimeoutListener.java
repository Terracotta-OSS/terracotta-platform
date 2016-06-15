/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
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
