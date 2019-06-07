/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.nomad;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class NomadEnvironment {
  private static final String USER_NAME_PROPERTY = "user.name";

  public String getHost() {
    try {
      InetAddress localHost = InetAddress.getLocalHost();
      return localHost.getHostName();
    } catch (UnknownHostException e) {
      return "unknown";
    }
  }

  public String getUser() {
    String user = System.getProperty(USER_NAME_PROPERTY);

    if (user == null) {
      return "unknown";
    }

    return user;
  }
}
