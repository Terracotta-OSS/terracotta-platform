/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.inet;

public class HostAndIpValidator {
  private final static String VALID_HOSTNAME = "^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$";

  private static final String OCTET = "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";
  private static final String IPv4_ADDRESS = "(" + OCTET + "\\." + OCTET + "\\." + OCTET + "\\." + OCTET + ")";
  // from https://www.regexpal.com/93988
  private final static String IPv6_ADDRESS = "s*((([0-9A-Fa-f]{1,4}:){7}([0-9A-Fa-f]{1,4}|:))|" +
      "(([0-9A-Fa-f]{1,4}:){6}(:[0-9A-Fa-f]{1,4}|((25[0-5]|2[0-4]d|1dd|[1-9]?d)(.(25[0-5]|2[0-4]d|1dd|[1-9]?d)){3})|:))|" +
      "(([0-9A-Fa-f]{1,4}:){5}(((:[0-9A-Fa-f]{1,4}){1,2})|:((25[0-5]|2[0-4]d|1dd|[1-9]?d)(.(25[0-5]|2[0-4]d|1dd|[1-9]?d)){3})|:))|" +
      "(([0-9A-Fa-f]{1,4}:){4}(((:[0-9A-Fa-f]{1,4}){1,3})|((:[0-9A-Fa-f]{1,4})?:((25[0-5]|2[0-4]d|1dd|[1-9]?d)(.(25[0-5]|2[0-4]d|1dd|[1-9]?d)){3}))|:))|" +
      "(([0-9A-Fa-f]{1,4}:){3}(((:[0-9A-Fa-f]{1,4}){1,4})|((:[0-9A-Fa-f]{1,4}){0,2}:((25[0-5]|2[0-4]d|1dd|[1-9]?d)(.(25[0-5]|2[0-4]d|1dd|[1-9]?d)){3}))|:))|" +
      "(([0-9A-Fa-f]{1,4}:){2}(((:[0-9A-Fa-f]{1,4}){1,5})|((:[0-9A-Fa-f]{1,4}){0,3}:((25[0-5]|2[0-4]d|1dd|[1-9]?d)(.(25[0-5]|2[0-4]d|1dd|[1-9]?d)){3}))|:))|" +
      "(([0-9A-Fa-f]{1,4}:){1}(((:[0-9A-Fa-f]{1,4}){1,6})|((:[0-9A-Fa-f]{1,4}){0,4}:((25[0-5]|2[0-4]d|1dd|[1-9]?d)(.(25[0-5]|2[0-4]d|1dd|[1-9]?d)){3}))|:))|" +
      "(:(((:[0-9A-Fa-f]{1,4}){1,7})|((:[0-9A-Fa-f]{1,4}){0,5}:((25[0-5]|2[0-4]d|1dd|[1-9]?d)(.(25[0-5]|2[0-4]d|1dd|[1-9]?d)){3}))|:)))(%.+)?s*";

  private static final String IPv4_MASK = "(3[0-2]|2[0-9]|1[0-9]|[0-9])";
  private static final String IPv6_MASK = "([0-9]|[1-9][0-9]|1[0-1][0-9]|12[0-8])";

  private static final String IPv4_CIDR = IPv4_ADDRESS + "/" + IPv4_MASK;
  private static final String IPv6_CIDR = IPv6_ADDRESS + "/" + IPv6_MASK;

  private static final String IPv4_CIDR_SUFFIX = "/32";
  private static final String IPv6_CIDR_SUFFIX = "/128";

  public static boolean isValidHost(String host) {
    return host.matches(VALID_HOSTNAME);
  }

  public static boolean isValidIPv4(String ipAddress) {
    return ipAddress.matches(IPv4_ADDRESS);
  }

  public static boolean isValidIPv4Cidr(String cidr) {
    return cidr.matches(IPv4_CIDR);
  }

  public static boolean isValidIPv6(String ipAddress) {
    return isValidIPv6(ipAddress, false) || isValidIPv6(ipAddress, true);
  }

  public static boolean isValidIPv6(String ipAddress, boolean brackets) {
    if (brackets) {
      return ipAddress.matches("\\[" + IPv6_ADDRESS + "\\]");
    } else {
      return ipAddress.matches(IPv6_ADDRESS);
    }
  }

  public static boolean isValidIPv6Cidr(String cidr) {
    return cidr.matches(IPv6_CIDR);
  }

  public static String getIPv4Suffix() {
    return IPv4_CIDR_SUFFIX;
  }

  public static String getIPv6Suffix() {
    return IPv6_CIDR_SUFFIX;
  }
}
