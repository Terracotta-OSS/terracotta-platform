/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.inet;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Objects;

import static org.terracotta.inet.HostAndIpValidator.isValidIPv6;

public class InetSocketAddressUtils {
  public static boolean areEqual(InetSocketAddress one, InetSocketAddress two) {
    return Objects.equals(encloseInBracketsIfIpv6(one), encloseInBracketsIfIpv6(two));
  }

  public static InetSocketAddress encloseInBracketsIfIpv6(InetSocketAddress address) {
    if (address != null && isValidIPv6(address.getHostName(), false)) {
      address = InetSocketAddress.createUnresolved("[" + address.getHostName() + "]", address.getPort());
    }
    return address;
  }

  public static boolean contains(Collection<InetSocketAddress> addresses, InetSocketAddress address) {
    for (InetSocketAddress addr : addresses) {
      if (areEqual(addr, address)) {
        return true;
      }
    }
    return false;
  }

  public static boolean containsAll(Collection<InetSocketAddress> addresses, Collection<InetSocketAddress> subset) {
    for (InetSocketAddress element : subset) {
      if (!contains(addresses, element)) {
        return false;
      }
    }
    return true;
  }
}
