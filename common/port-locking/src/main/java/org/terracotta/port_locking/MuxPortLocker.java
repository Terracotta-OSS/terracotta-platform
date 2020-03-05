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

import java.util.Arrays;
import java.util.List;

public class MuxPortLocker implements PortLocker {
  private List<PortLocker> portLockers;

  public MuxPortLocker(PortLocker... portLockers) {
    this(Arrays.asList(portLockers));
  }

  private MuxPortLocker(List<PortLocker> portLockers) {
    this.portLockers = portLockers;
  }

  @Override
  public PortLock tryLockPort(int port) {
    MuxPortLock muxPortLock = new MuxPortLock(port);

    for (PortLocker portLocker : portLockers) {
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
