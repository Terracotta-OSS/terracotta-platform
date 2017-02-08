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
package org.terracotta.management.sequence;

import java.lang.management.ManagementFactory;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

/**
 * @author Mathieu Carbou
 */
class Defaults {

  private static final int PID_BITLENGTH = 16;
  private static final long PID_BITMASK = (1L << PID_BITLENGTH) - 1;

  private static final byte[] NO_MAC = new byte[6];

  static final int SEQ_BITLENGTH = 18;
  static final long SEQ_BITMASK = (1L << SEQ_BITLENGTH) - 1;

  static final int INSTANCE_BITLENGTH = 18;
  static final int INSTANCE_BITMASK = (1 << INSTANCE_BITLENGTH) - 1;

  static {
    byte b = 0;
    Arrays.fill(NO_MAC, b);
  }

  static final NodeIdSource MAC_PID_NODE_ID_SOURCE = new NodeIdSource() {
    @Override
    public long getNodeId() {
      // at least 6 bytes
      byte[] mac = readMacAddress();
      long nodeId = 0;
      // we consider the 6 last bytes only,
      for (int i = Math.max(0, mac.length - 6); i < mac.length; i++) {
        nodeId = (nodeId << 8) | (mac[i] & 0XFF);
      }
      // keeps a positive node id with the 0x7fffffffffffffffL mask
      return ((nodeId << PID_BITLENGTH) & Long.MAX_VALUE) | (readPID() & PID_BITMASK);
    }
  };

  static final NodeIdSource BEST_NODE_ID_SOURCE = new NodeIdSource() {
    final NodeIdSource delegate = findBest(NodeIdSource.class, MAC_PID_NODE_ID_SOURCE);

    @Override
    public long getNodeId() {
      return delegate.getNodeId();
    }
  };

  static final TimeSource SYSTEM_TIME_SOURCE = new TimeSource() {
    @Override
    public long getTimestamp() {
      return System.currentTimeMillis();
    }
  };

  static final TimeSource BEST_TIME_SOURCE = new TimeSource() {
    final TimeSource delegate = findBest(TimeSource.class, SYSTEM_TIME_SOURCE);

    @Override
    public long getTimestamp() {
      return delegate.getTimestamp();
    }
  };

  private static final Comparator<NetworkInterface> NETWORK_INTERFACE_COMPARATOR = new Comparator<NetworkInterface>() {
    @Override
    public int compare(NetworkInterface o1, NetworkInterface o2) {
      return o1.getName().compareTo(o2.getName());
    }
  };

  static byte[] readMacAddress() {
    List<NetworkInterface> networkInterfaces;
    try {
      networkInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
    } catch (SocketException e) {
      // this can only occur while testing in a computer locally with no network.
      // So mac address will be irrelevant because the system time will be the same across all started servers
      return NO_MAC;
    }

    // order interfaces by name
    Collections.sort(networkInterfaces, NETWORK_INTERFACE_COMPARATOR);

    // find the first accessible non-loopback interface having a mac address
    // we try first to skip all virtual interfaces since they can be easily dynamically created on-demand
    // the goal of this method is to try return the same value each time as possible
    // we consider only "parent" interfaces
    for (NetworkInterface networkInterface : networkInterfaces) {
      try {
        byte[] mac;
        if (!networkInterface.isLoopback()
            && !networkInterface.isPointToPoint()
            && !networkInterface.isVirtual()
            && networkInterface.getParent() == null
            && (mac = networkInterface.getHardwareAddress()) != null
            && mac.length >= 6) {
          return mac;
        }
      } catch (SocketException ignored) {
      }
    }

    // if we do not succeed, we enlarge our search
    for (NetworkInterface networkInterface : networkInterfaces) {
      try {
        byte[] mac;
        if (!networkInterface.isLoopback()
            && (mac = networkInterface.getHardwareAddress()) != null
            && mac.length >= 6) {
          return mac;
        }
      } catch (SocketException ignored) {
      }
    }

    // this can only occur while testing in a computer locally with no network.
    // So mac address will be irrelevant because the system time will be the same across all started servers
    return NO_MAC;
  }

  static long readPID() {
    String name = ManagementFactory.getRuntimeMXBean().getName();
    long pid = 0;
    for (int i = 0; i < name.length() && Character.isDigit(name.charAt(i)); i++) {
      pid = pid * 10 + Character.getNumericValue(name.charAt(i));
    }
    return pid;
  }

  private static <T> T findBest(Class<T> type, T def) {
    // try first a JVM sysprop
    String cName = System.getProperty(type.getName());
    if (cName != null) {
      try {
        return type.cast(Defaults.class.getClassLoader().loadClass(cName).newInstance());
      } catch (Exception e) {
        throw new IllegalArgumentException("Unable to instantiate  " + type.getSimpleName() + " " + cName + " set from system property");
      }
    } else {
      // otherwise, try a service implementation
      try {
        return ServiceLoader.load(type).iterator().next();
      } catch (Exception ignored) {
      }
    }
    // or returns default
    return def;
  }

}
