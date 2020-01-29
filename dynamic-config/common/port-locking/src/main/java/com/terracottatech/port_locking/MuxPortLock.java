/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.port_locking;

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
