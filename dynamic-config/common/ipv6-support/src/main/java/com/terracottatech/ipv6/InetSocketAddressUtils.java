/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.ipv6;

import java.net.InetSocketAddress;
import java.util.Objects;

import static com.terracottatech.ipv6.HostAndIpValidator.isValidIPv6;

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
}
