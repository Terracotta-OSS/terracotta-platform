/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
package org.terracotta.inet;

import java.net.IDN;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;

public class HostAndIpValidator {

  private static final Pattern OCTET = compile("(?:25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9][0-9]|[0-9])");
  private static final Pattern IPv4_ADDRESS = compile("(?:" + OCTET + "\\.){3}" + OCTET);

  private static final Pattern IPv4_MASK = compile("(?:3[0-2]|2[0-9]|1[0-9]|[0-9])");
  private static final Pattern IPv6_MASK = compile("(?:[0-9]|[1-9][0-9]|1[0-1][0-9]|12[0-8])");

  private static final String IPv4_CIDR = IPv4_ADDRESS + "/" + IPv4_MASK;

  private static final String IPv4_CIDR_SUFFIX = "/32";
  private static final String IPv6_CIDR_SUFFIX = "/128";

  public static boolean isValidHost(String host) {
    try {
      return !IDN.toASCII(host, IDN.ALLOW_UNASSIGNED | IDN.USE_STD3_ASCII_RULES).isEmpty();
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  public static boolean isValidIPv4(String ipAddress) {
    return IPv4_ADDRESS.matcher(ipAddress).matches();
  }

  public static boolean isValidIPv4Cidr(String cidr) {
    return cidr.matches(IPv4_CIDR);
  }

  public static boolean isValidIPv6(String ipAddress) {
    if (ipAddress.startsWith("[")) {
      try {
        InetAddress.getAllByName(ipAddress);
        return true;
      } catch (UnknownHostException e) {
        return false;
      }
    } else {
      return isValidIPv6("[" + ipAddress + "]");
    }
  }

  public static boolean isValidIPv6(String ipAddress, boolean brackets) {
    if (brackets) {
      if (ipAddress.startsWith("[")) {
        try {
          InetAddress.getAllByName(ipAddress);
          return true;
        } catch (UnknownHostException e) {
          return false;
        }
      } else {
        return false;
      }
    } else {
      return isValidIPv6("[" + ipAddress + "]", true);
    }
  }

  public static boolean isValidIPv6Cidr(String cidr) {
    String[] parts = cidr.split("/", 2);
    if (parts.length == 2) {
      return isValidIPv6(parts[0]) && IPv6_MASK.matcher(parts[1]).matches();
    } else {
      return false;
    }
  }

  public static String getIPv4Suffix() {
    return IPv4_CIDR_SUFFIX;
  }

  public static String getIPv6Suffix() {
    return IPv6_CIDR_SUFFIX;
  }
}
