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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.BitSet;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LockingPortChooserTest {
  @Mock
  private PortAllocator portAllocator;

  @Mock
  private PortLocker portLocker;

  private BitSet locks = new BitSet(16);
  private LockingPortChooser portChooser;

  @Before
  public void before() {
    portChooser = new LockingPortChooser(portAllocator, portLocker);
    when(portLocker.tryLockPort(anyInt())).thenAnswer((Answer<PortLock>) invocation -> {
      int port = invocation.getArgument(0);
      if (locks.get(port)) {
        return null;
      }

      locks.set(port);

      return new TestPortLock(port);
    });
  }

  @Test
  public void happyPath() {
    when(portAllocator.allocatePorts(4)).thenReturn(2, 6);

    try (MuxPortLock portLock = portChooser.choosePorts(4)) {
      assertEquals(2, portLock.getPort());
    }

    try (MuxPortLock portLock = portChooser.choosePorts(4)) {
      assertEquals(6, portLock.getPort());
    }
  }

  @Test
  public void reattempt() {
    when(portAllocator.allocatePorts(4)).thenReturn(2, 2, 6);

    try (
        MuxPortLock portLock1 = portChooser.choosePorts(4);
        MuxPortLock portLock2 = portChooser.choosePorts(4)
    ) {
      assertEquals(2, portLock1.getPort());
      assertEquals(6, portLock2.getPort());
    }
  }

  @Test
  public void overlap() {
    when(portAllocator.allocatePorts(4)).thenReturn(2, 0, 6, 0);

    try (
        MuxPortLock portLock1 = portChooser.choosePorts(4);
        MuxPortLock portLock2 = portChooser.choosePorts(4)
    ) {
      assertEquals(2, portLock1.getPort());
      assertEquals(6, portLock2.getPort());
    }

    try (MuxPortLock portLock = portChooser.choosePorts(4)) {
      assertEquals(0, portLock.getPort());
    }
  }

  private class TestPortLock implements PortLock {
    private final int port;

    TestPortLock(int port) {
      this.port = port;
    }

    @Override
    public int getPort() {
      return port;
    }

    @Override
    public void close() {
      if (!locks.get(port)) {
        throw new AssertionError("Lock not locked");
      }

      locks.clear(port);
    }
  }
}
