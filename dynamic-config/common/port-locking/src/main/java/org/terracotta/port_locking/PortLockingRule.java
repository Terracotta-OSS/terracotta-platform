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

import org.junit.rules.ExternalResource;

import java.util.stream.IntStream;

/**
 * @author Mathieu Carbou
 */
public class PortLockingRule extends ExternalResource {

  private static final LockingPortChooser LOCKING_PORT_CHOOSER = LockingPortChoosers.getFileLockingPortChooser();

  private final int count;

  private MuxPortLock portLock;
  private int[] ports = new int[0];

  public PortLockingRule(int count) {
    this.count = count;
  }

  public int getPort() {
    return getPorts()[0];
  }

  public int[] getPorts() {
    return ports.clone();
  }

  @Override
  protected void before() {
    if (count > 0) {
      this.portLock = LOCKING_PORT_CHOOSER.choosePorts(count);
      this.ports = IntStream.range(portLock.getPort(), portLock.getPort() + count).toArray();
    } else {
      this.ports = new int[0];
    }
  }

  @Override
  protected void after() {
    if (portLock != null) {
      portLock.close();
    }
  }
}
