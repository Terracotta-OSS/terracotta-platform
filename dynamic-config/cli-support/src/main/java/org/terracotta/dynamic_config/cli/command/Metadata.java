/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.cli.command;

import com.beust.jcommander.Parameters;

/**
 * @author Mathieu Carbou
 */
public class Metadata {

  public static String getName(Command command) {
    Parameters annotation = command.getClass().getAnnotation(Parameters.class);
    if (annotation != null && annotation.commandNames().length > 0) {
      return annotation.commandNames()[0];
    }
    return command.getClass().getSimpleName().toLowerCase().replace("command", "");
  }

  public static String getUsage(Command command) {
    Usage annotation = command.getClass().getAnnotation(Usage.class);
    if (annotation != null) {
      return annotation.value();
    }
    return "";
  }
}
