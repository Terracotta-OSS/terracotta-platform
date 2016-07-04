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
package org.terracotta.management.model.cluster;

import org.terracotta.management.model.Objects;

import javax.xml.bind.DatatypeConverter;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Client identifier: {@code <PID>@<ADDRESS>:<NAME>:<UUID>}. Where:
 * <ul>
 * <li>PID is the JVM PID</li>
 * <li>ADDRESS is the JVM host address used to connect to the cluster</li>
 * <li>NAME is a user-defined string passed when opening a connection to a cluster (connection.name property) such as EHCACHE:my-cache-manager</li>
 * <li>UUID is the logical connection ID set by the platform, used to regroup several physical connections when in a cluster with several stripes</li>
 * </ul>
 *
 * @author Mathieu Carbou
 */
public final class ClientIdentifier implements Serializable {

  private static final long serialVersionUID = 1;

  private static final Logger LOGGER = Logger.getLogger(ClientIdentifier.class.getName());

  private final long pid;
  private final String name;
  private final String connectionUid;
  private final String hostAddress;

  private ClientIdentifier(long pid, String hostAddress, String name, String connectionUid) {
    this.hostAddress = Objects.requireNonNull(hostAddress);
    this.pid = pid;
    this.connectionUid = Objects.requireNonNull(connectionUid);
    this.name = Objects.requireNonNull(name);
    if (hostAddress.isEmpty()) {
      throw new IllegalArgumentException("Empty host address");
    }
    if (name.isEmpty()) {
      throw new IllegalArgumentException("Empty name");
    }
  }

  public String getConnectionUid() {
    return connectionUid;
  }

  public String getHostAddress() {
    return hostAddress;
  }

  public long getPid() {
    return pid;
  }

  public String getName() {
    return name;
  }

  public String getVmId() {
    return pid + "@" + hostAddress;
  }

  public String getClientId() {
    return getVmId() + ":" + name + ":" + connectionUid;
  }

  @Override
  public String toString() {
    return getClientId();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ClientIdentifier that = (ClientIdentifier) o;
    return pid == that.pid
        && name.equals(that.name)
        && connectionUid.equals(that.connectionUid)
        && hostAddress.equals(that.hostAddress);
  }

  @Override
  public int hashCode() {
    int result = (int) (pid ^ (pid >>> 32));
    result = 31 * result + name.hashCode();
    result = 31 * result + connectionUid.hashCode();
    result = 31 * result + hostAddress.hashCode();
    return result;
  }

  public static ClientIdentifier create(long pid, String hostAddress, String name, String uuid) {
    return new ClientIdentifier(pid, hostAddress, name, uuid);
  }

  public static ClientIdentifier create(String name, String logicalConnectionUid) {
    try {
      InetAddress inetAddress = discoverLANAddress();
      return new ClientIdentifier(discoverPID(), inetAddress.getHostAddress(), name, logicalConnectionUid);
    } catch (UnknownHostException e) {
      return new ClientIdentifier(discoverPID(), "127.0.0.1", name, logicalConnectionUid);
    }

  }

  public static ClientIdentifier valueOf(String identifier) {
    try {
      int copy = identifier.indexOf('@');
      int firstColon = identifier.indexOf(':', copy + 1);
      int lastColon = identifier.lastIndexOf(':');
      return new ClientIdentifier(
          Long.parseLong(identifier.substring(0, copy)),
          identifier.substring(copy + 1, firstColon),
          identifier.substring(firstColon + 1, lastColon),
          identifier.substring(lastColon + 1));
    } catch (RuntimeException e) {
      throw new IllegalArgumentException(identifier);
    }
  }

  static String discoverHostName() {
    String hostname = null;

    try {
      String procname = "hostname";
      if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
        procname += ".exe";
      }
      Process process = Runtime.getRuntime().exec(procname);
      if (process.waitFor() == 0) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream in = process.getInputStream();
        int r;
        while ((r = in.read()) != -1) {
          baos.write(r);
        }
        in.close();
        hostname = new String(baos.toByteArray(), "UTF-8");
      }
    } catch (Exception e) {
      // if anything goes wrong, just ignore
      if (LOGGER.isLoggable(Level.FINEST)) {
        LOGGER.log(Level.FINEST, "ERR getHostName(): " + e.getMessage(), e);
      }
    }

    if (hostname != null) {
      return hostname;
    }

    try {
      InetAddress address = discoverLANAddress();
      String resolved = address.getCanonicalHostName();
      if (!address.getHostAddress().equals(resolved)) {
        // this check is ok, getCanonicalHostName() does return getHostAddress() in case of failure
        hostname = resolved;
      }
    } catch (Exception ignored) {
    }

    return hostname;
  }

  static String generateNewUUID() {
    UUID j = UUID.randomUUID();
    byte[] data = new byte[16];
    long msb = j.getMostSignificantBits();
    long lsb = j.getLeastSignificantBits();
    for (int i = 0; i < 8; i++) {
      data[i] = (byte) (msb & 0xff);
      msb >>>= 8;
    }
    for (int i = 8; i < 16; i++) {
      data[i] = (byte) (lsb & 0xff);
      lsb >>>= 8;
    }
    return DatatypeConverter.printBase64Binary(data)
        // java-8 and otehr - compatible B64 url decoder using - and _ instead of + and /
        // padding can be ignored to shorter the UUID
        .replace('+', '-').replace('/', '_').replace("=", "");
  }

  static long discoverPID() {
    String name = ManagementFactory.getRuntimeMXBean().getName();
    long pid = 0;
    for (int i = 0; i < name.length() && Character.isDigit(name.charAt(i)); i++) {
      pid = pid * 10 + Character.getNumericValue(name.charAt(i));
    }
    return pid;
  }

  /**
   * http://stackoverflow.com/questions/9481865/getting-the-ip-address-of-the-current-machine-using-java
   * <p>
   * Returns an <code>InetAddress</code> object encapsulating what is most likely the machine's LAN IP address.
   * <p>
   * This method is intended for use as a replacement of JDK method <code>InetAddress.getLocalHost</code>, because
   * that method is ambiguous on Linux systems. Linux systems enumerate the loopback network interface the same
   * way as regular LAN network interfaces, but the JDK <code>InetAddress.getLocalHost</code> method does not
   * specify the algorithm used to select the address returned under such circumstances, and will often return the
   * loopback address, which is not valid for network communication. Details
   * <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4665037">here</a>.
   * <p>
   * This method will scan all IP addresses on all network interfaces on the host machine to determine the IP address
   * most likely to be the machine's LAN address. If the machine has multiple IP addresses, this method will prefer
   * a site-local IP address (e.g. 192.168.x.x or 10.10.x.x, usually IPv4) if the machine has one (and will return the
   * first site-local address if the machine has more than one), but if the machine does not hold a site-local
   * address, this method will return simply the first non-loopback address found (IPv4 or IPv6).
   * <p>
   * If this method cannot find a non-loopback address using this selection algorithm, it will fall back to
   * calling and returning the result of JDK method <code>InetAddress.getLocalHost</code>.
   * <p>
   *
   * @throws UnknownHostException If the LAN address of the machine cannot be found.
   */
  static InetAddress discoverLANAddress() throws UnknownHostException {
    InetAddress inetAddress = InetAddress.getLocalHost();
    if (!inetAddress.isLoopbackAddress() && inetAddress.isSiteLocalAddress()) {
      return inetAddress;
    }

    try {
      InetAddress candidateAddress = null;
      // Iterate all NICs (network interface cards)...
      for (Enumeration ifaces = NetworkInterface.getNetworkInterfaces(); ifaces.hasMoreElements(); ) {
        NetworkInterface iface = (NetworkInterface) ifaces.nextElement();
        // Iterate all IP addresses assigned to each card...
        for (Enumeration inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements(); ) {
          InetAddress inetAddr = (InetAddress) inetAddrs.nextElement();
          if (!inetAddr.isLoopbackAddress()) {

            if (inetAddr.isSiteLocalAddress()) {
              // Found non-loopback site-local address. Return it immediately...
              return inetAddr;
            } else if (candidateAddress == null) {
              // Found non-loopback address, but not necessarily site-local.
              // Store it as a candidate to be returned if site-local address is not subsequently found...
              candidateAddress = inetAddr;
              // Note that we don't repeatedly assign non-loopback non-site-local addresses as candidates,
              // only the first. For subsequent iterations, candidate will be non-null.
            }
          }
        }
      }
      if (candidateAddress != null) {
        // We did not find a site-local address, but we found some other non-loopback address.
        // Server might have a non-site-local address assigned to its NIC (or it might be running
        // IPv6 which deprecates the "site-local" concept).
        // Return this non-loopback candidate address...
        return candidateAddress;
      }
      // At this point, we did not find a non-loopback address.
      // Fall back to returning whatever InetAddress.getLocalHost() returns...
      InetAddress jdkSuppliedAddress = InetAddress.getLocalHost();
      if (jdkSuppliedAddress == null) {
        throw new UnknownHostException("The JDK InetAddress.getLocalHost() method unexpectedly returned null.");
      }
      return jdkSuppliedAddress;
    } catch (Exception e) {
      UnknownHostException unknownHostException = new UnknownHostException("Failed to determine LAN address: " + e);
      unknownHostException.initCause(e);
      throw unknownHostException;
    }
  }
}
