/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.port_locking;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;

public class SocketPortLocker implements PortLocker {
  @Override
  public PortLock tryLockPort(int port) {
    ServerSocket serverSocket = null;

    try {
      serverSocket = new ServerSocket(port);
      return new EmptyPortLock(port);
    } catch (BindException be) {
      return null;
    } catch (IOException e) {
      throw new PortLockingException("Error detecting whether port " + port + " is available to bind", e);
    } finally {
      if (serverSocket != null) {
        try {
          serverSocket.close();
        } catch (IOException e) {
          throw new PortLockingException("Failed to close socket", e);
        }
      }
    }
  }
}
