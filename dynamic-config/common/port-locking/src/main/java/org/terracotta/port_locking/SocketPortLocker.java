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
package org.terracotta.port_locking;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;

public class SocketPortLocker implements PortLocker {
  @Override
  public PortLock tryLockPort(int port) {
    ServerSocket serverSocket = null;

    try {
      serverSocket = new ServerSocket(port);
      return new EmptyPortLock(port);
    } catch (BindException be) {
      return null;
    } catch (IOException e) {
      throw new PortLockingException("Error detecting whether port " + port + " is available to bind", e);
    } finally {
      if (serverSocket != null) {
        try {
          serverSocket.close();
        } catch (IOException e) {
          throw new PortLockingException("Failed to close socket", e);
        }
      }
    }
  }
}
