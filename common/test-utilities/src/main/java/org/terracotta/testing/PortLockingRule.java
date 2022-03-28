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
package org.terracotta.testing;

import org.junit.runner.Description;
import org.terracotta.port_locking.LockingPortChooser;
import org.terracotta.port_locking.LockingPortChoosers;
import org.terracotta.port_locking.MuxPortLock;

import java.util.stream.IntStream;

/**
 * @author Mathieu Carbou
 */
public class PortLockingRule extends ExtendedTestRule {

  private final LockingPortChooser lockingPortChooser;

  private final int count;

  private MuxPortLock portLock;
  private int[] ports = new int[0];

  public PortLockingRule(int count) {
    this(LockingPortChoosers.getFileLockingPortChooser(), count);
  }

  public PortLockingRule(LockingPortChooser lockingPortChooser, int count) {
    this.lockingPortChooser = lockingPortChooser;
    this.count = count;
  }

  public int getPort() {
    return getPorts()[0];
  }

  public int[] getPorts() {
    return ports.clone();
  }

  @Override
  protected void before(Description description) {
    if (count > 0) {
      this.portLock = lockingPortChooser.choosePorts(count);
      this.ports = IntStream.range(portLock.getPort(), portLock.getPort() + count).toArray();
    } else {
      this.ports = new int[0];
    }
  }

  @Override
  protected void after(Description description) {
    if (portLock != null) {
      portLock.close();
    }
  }
}
