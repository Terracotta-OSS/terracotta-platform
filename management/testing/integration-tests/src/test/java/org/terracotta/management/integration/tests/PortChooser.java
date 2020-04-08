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
package org.terracotta.management.integration.tests;

import com.tc.net.EphemeralPorts;
import com.tc.net.EphemeralPorts.Range;
import com.tc.util.Assert;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public final class PortChooser {
  public static final int           MAX          = 65535;

  private static final Object       VM_WIDE_LOCK = (com.tc.util.PortChooser.class.getName() + "LOCK").intern();
  private static final Set<Integer> chosen       = new HashSet<Integer>();
  private static final Random       random       = new Random();
  private static final Range exclude      = EphemeralPorts.getRange();

  public int chooseRandomPort() {
    synchronized (VM_WIDE_LOCK) {
      int portNum = choose();
      Assert.assertTrue(chosen.add(Integer.valueOf(portNum)));
      return portNum;
    }
  }

  public int chooseRandom2Port() {
    int port;
    synchronized (VM_WIDE_LOCK) {
      do {
        port = choose();
        if (port + 1 >= MAX) continue;
        if (!isPortUsed(port + 1)) {
          Assert.assertTrue(chosen.add(Integer.valueOf(port)));
          Assert.assertTrue(chosen.add(Integer.valueOf(port + 1)));
          break;
        }
      } while (true);
    }
    return port;
  }

  public int chooseRandomPorts(int numOfPorts) {
    Assert.assertTrue(numOfPorts > 0);
    int port = 0;
    synchronized (VM_WIDE_LOCK) {
      System.out.println("===> exclude range: " + exclude);
      do {
        port = choose();
        if (port + numOfPorts > MAX) continue;
        boolean isChosen = true;
        for (int i = 1; i < numOfPorts; i++) {
          if (isPortUsed(port + i)) {
            isChosen = false;
            break;
          }
        }
        if (isChosen && (port + numOfPorts <= MAX)) {
          break;
        }
      } while (true);

      for (int i = 0; i < numOfPorts; i++) {
        Assert.assertTrue(chosen.add(Integer.valueOf(port + i)));
      }
    }
    return port;
  }

  public boolean isPortUsed(int portNum) {
    final Integer port = Integer.valueOf(portNum);
    if (chosen.contains(port)) return true;
    return !canBind(portNum) && !canConnect(portNum);
  }

  private boolean canConnect(int portNumber) {
    Socket sock = null;
    boolean isFree = false;

    long startTime = System.nanoTime();
    try {
      System.out.println("===> canConnect connecting to " + portNumber);
      sock = new Socket("localhost", portNumber);
      isFree = false;
    } catch (IOException e) {
      isFree = true;
    } finally {
      if (sock != null) {
        try {
          sock.close();
        } catch (IOException e) {
          // ignore
        }
      }
      System.out.println("===> canConnect done " + isFree + " " + (System.nanoTime() - startTime));
    }

    return isFree;
  }

  private boolean canBind(int portNum) {
    if (exclude.isInRange(portNum)) { return false; }
    long startTime = System.nanoTime();
    ServerSocket ss = null;
    boolean isFree = false;
    try {
      System.out.println("===> canBind binding to " + portNum);
      ss = new ServerSocket(portNum);
      isFree = true;
    } catch (BindException be) {
      isFree = false; // port in use,
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      if (ss != null) {
        try {
          ss.close();
        } catch (IOException e) {
          // ignore
        }
      }
      System.out.println("===> canBind done " + isFree + " " + (System.nanoTime() - startTime));
    }

    return isFree;
  }

  private synchronized int choose() {
    while (true) {
      final int attempt = getNonEphemeralPort();
      System.out.println("===> choose " + attempt);
      if (chosen.contains(Integer.valueOf(attempt))) {
        System.out.println("===> already chosen contains " + attempt);
        continue; // already picked at some point, try again
      }
      if (canBind(attempt) && canConnect(attempt)) return attempt;
    }
  }

  private static int getNonEphemeralPort() {
    while (true) {
      int p = random.nextInt(MAX - 1024) + 1024;
      System.out.println("===> getNonEphemeralPort " + p);
      if (!exclude.isInRange(p)) { return p; }
    }
  }

}
