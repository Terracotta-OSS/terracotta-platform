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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MuxPortLock implements PortLock {
  private final int port;
  private final List<PortLock> portLocks;

  MuxPortLock(int port) {
    this.port = port;
    this.portLocks = new ArrayList<>();
  }

  private MuxPortLock(int port, List<PortLock> portLocks1, List<PortLock> portLocks2) {
    this.port = port;
    this.portLocks = new ArrayList<>(portLocks1.size() + portLocks2.size());
    portLocks.addAll(portLocks1);
    portLocks.addAll(portLocks2);
  }

  void addPortLock(PortLock portLock) {
    portLocks.add(portLock);
  }

  public MuxPortLock combine(MuxPortLock other) {
    return new MuxPortLock(port, portLocks, other.portLocks);
  }

  @Override
  public int getPort() {
    return port;
  }

  @Override
  public void close() {
    PortLockingException closeError = new PortLockingException("Error closing MuxPortLock");

    Collections.reverse(portLocks);

    portLocks.forEach(portLock -> {
      try {
        portLock.close();
      } catch (PortLockingException e) {
        closeError.addSuppressed(e);
      }
    });

    portLocks.clear();

    if (closeError.getSuppressed().length > 0) {
      throw closeError;
    }
  }
}
