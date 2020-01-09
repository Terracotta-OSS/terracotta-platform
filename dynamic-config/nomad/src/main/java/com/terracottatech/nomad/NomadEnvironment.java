/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Clock;

public class NomadEnvironment {
  private static final String USER_NAME_PROPERTY = "user.name";
  private static final String UNKNOWN = "unknown";

  public String getHost() {
    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      return UNKNOWN;
    }
  }

  public String getUser() {
    return System.getProperty(USER_NAME_PROPERTY, UNKNOWN);
  }

  public Clock getClock() {
    return Clock.systemUTC();
  }
}