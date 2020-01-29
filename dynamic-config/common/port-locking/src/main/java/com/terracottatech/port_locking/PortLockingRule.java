/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.port_locking;

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
    this.portLock = LOCKING_PORT_CHOOSER.choosePorts(count);
    this.ports = IntStream.range(portLock.getPort(), portLock.getPort() + count).toArray();
  }

  @Override
  protected void after() {
    if (portLock != null) {
      portLock.close();
    }
  }
}
