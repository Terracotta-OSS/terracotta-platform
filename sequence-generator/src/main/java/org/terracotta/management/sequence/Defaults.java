/*
 * Copyright Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.terracotta.management.sequence;

import java.lang.management.ManagementFactory;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.List;

/**
 * @author Mathieu Carbou
 */
public class Defaults {

  private static final int PID_BITLENGTH = 16;
  private static final long PID_BITMASK = (1L << PID_BITLENGTH) - 1;

  static final int SEQ_BITLENGTH = 18;
  static final long SEQ_BITMASK = (1L << SEQ_BITLENGTH) - 1;

  static final int INSTANCE_BITLENGTH = 18;
  static final int INSTANCE_BITMASK = (1 << INSTANCE_BITLENGTH) - 1;

  static final NodeIdSource DEFAULT_NODE_ID_SOURCE = new NodeIdSource() {
    @Override
    public long getNodeId() {
      byte[] mac = readMAC();
      long macId = 0;
      for (int i = 0; i < 6; i++) {
        macId = (macId << 8) | (mac[i] & 0XFF);
      }
      return (macId << PID_BITLENGTH) | (readPID() & PID_BITMASK);
    }
  };

  static final TimeSource SYSTEM_TIME_SOURCE = new TimeSource() {
    @Override
    public long getTimestamp() {
      return System.currentTimeMillis();
    }
  };

  static byte[] readMAC() {
    List<NetworkInterface> networkInterfaces;
    try {
      networkInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
    } catch (SocketException e) {
      throw new IllegalStateException("Machine has no network interfaces!", e);
    }
    for (NetworkInterface networkInterface : networkInterfaces) {
      // loopback check
      try {
        if (networkInterface.isLoopback()) {
          continue;
        }
      } catch (SocketException e) {
        throw new IllegalStateException("Unable to check if network interface " + networkInterface + " is loopback.", e);
      }
      try {
        byte[] mac = networkInterface.getHardwareAddress();
        if (mac != null) {
          return mac;
        }
      } catch (SocketException e) {
        throw new IllegalStateException("Unable to read mac address of network interface " + networkInterface, e);
      }
    }
    throw new IllegalStateException("Unable to read a MAC address");
  }

  static long readPID() {
    String name = ManagementFactory.getRuntimeMXBean().getName();
    long pid = 0;
    for (int i = 0; i < name.length() && Character.isDigit(name.charAt(i)); i++) {
      pid = pid * 10 + Character.getNumericValue(name.charAt(i));
    }
    return pid;
  }

}
