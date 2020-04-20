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
package org.terracotta.testing.config;

import org.terracotta.port_locking.LockingPortChooser;
import org.terracotta.port_locking.LockingPortChoosers;
import org.terracotta.port_locking.MuxPortLock;

/**
 * @author Mathieu Carbou
 */
public class DefaultPortAllocator implements PortAllocator {

  private final LockingPortChooser lockingPortChooser;

  public DefaultPortAllocator() {this(LockingPortChoosers.getFileLockingPortChooser());}

  public DefaultPortAllocator(LockingPortChooser lockingPortChooser) {this.lockingPortChooser = lockingPortChooser;}

  @Override
  public PortAllocation reserve(int portCounts) {
    MuxPortLock muxPortLock = lockingPortChooser.choosePorts(portCounts);
    return new PortAllocation() {
      @Override
      public int getBasePort() {
        return muxPortLock.getPort();
      }

      @Override
      public int getPortCount() {
        return portCounts;
      }

      @Override
      public void close() {
        muxPortLock.close();
      }
    };
  }
}
