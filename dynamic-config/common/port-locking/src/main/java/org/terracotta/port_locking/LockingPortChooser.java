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

public class LockingPortChooser {
  private final PortAllocator portAllocator;
  private final PortLocker portLocker;

  public LockingPortChooser(PortAllocator portAllocator, PortLocker portLocker) {
    this.portAllocator = portAllocator;
    this.portLocker = portLocker;
  }

  public MuxPortLock choosePorts(int portCount) {
    while (true) {
      MuxPortLock muxPortLock = tryChoosePorts(portCount);

      if (muxPortLock != null) {
        return muxPortLock;
      }
    }
  }

  private MuxPortLock tryChoosePorts(int portCount) {
    int portBase = portAllocator.allocatePorts(portCount);

    MuxPortLock muxPortLock = new MuxPortLock(portBase);

    for (int i = 0; i < portCount; i++) {
      int port = portBase + i;

      PortLock portLock = portLocker.tryLockPort(port);

      if (portLock == null) {
        muxPortLock.close();
        return null;
      }

      muxPortLock.addPortLock(portLock);
    }

    return muxPortLock;
  }
}
