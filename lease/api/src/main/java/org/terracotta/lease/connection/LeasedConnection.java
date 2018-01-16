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
package org.terracotta.lease.connection;

import org.terracotta.connection.Connection;
import org.terracotta.lease.LeaseMaintainer;

/**
 * Connection provided on lease by server
 */
public interface LeasedConnection extends Connection {

  /**
   * returns true if connection is still valid, else false
   * @return validity of connection
   */
  boolean isConnectionValid();

  /**
   * Sets a listener which will be called when disconnect happens
   * This can be used to let layers above know about disconnection and take corrective action
   * @param eventListener listener to be called when disconnect happens
   */
  void addDisconnectListener(DisconnectedEventListener eventListener);

  interface DisconnectedEventListener {
    void notifyDisconnected();
  }
}
