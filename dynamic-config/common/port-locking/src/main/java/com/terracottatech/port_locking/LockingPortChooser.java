/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.port_locking;

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
