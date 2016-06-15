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
