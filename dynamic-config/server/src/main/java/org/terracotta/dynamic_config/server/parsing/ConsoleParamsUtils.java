/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.server.parsing;

import org.terracotta.dynamic_config.api.model.Setting;

public class ConsoleParamsUtils {
  public static String stripDashDash(String param) {
    return param.substring(2);
  }

  public static String addDashDash(String param) {
    return "--" + param;
  }

  public static String addDashDash(Setting param) {
    return addDashDash(param.toString());
  }
}
