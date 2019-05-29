/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.test.util;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

/**
 * @author Mathieu Carbou
 */
public class Env {

  public static boolean isWindows() {
    return System.getProperty("os.name").toLowerCase().startsWith("windows");
  }

  public static Path getProjectRootPath() {
    try {
      return new File("../..").getCanonicalFile().toPath();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static Path getModulePath() {
    return getProjectRootPath().resolve("dynamic-config").resolve("integration-tests");
  }

}
