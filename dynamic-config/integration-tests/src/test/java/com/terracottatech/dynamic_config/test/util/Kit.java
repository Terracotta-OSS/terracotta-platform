/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.test.util;

import org.terracotta.ipceventbus.proc.AnyProcess;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * @author Mathieu Carbou
 */
public class Kit {

  // memoize results for subsequent test execution to fasten build feedback in case of error
  private static Path KIT_PATH;
  private static RuntimeException ERROR;

  public static Optional<Path> getPath() {
    return Optional.ofNullable(System.getProperty("kitInstallationPath")).map(s -> Paths.get(s));
  }

  public static Path getOrCreatePath() {
    return getPath().orElseGet(() -> {
      Path path = build();
      System.setProperty("kitInstallationPath", path.toAbsolutePath().toString());
      return path;
    });
  }

  public static synchronized Path build() {
    if (KIT_PATH != null) {
      return KIT_PATH;
    }
    if (ERROR != null) {
      throw ERROR;
    }
    try {
      Path rootPath = Env.getProjectRootPath();
      System.out.println("Building KIT...");
      AnyProcess process = AnyProcess.newBuilder()
          .workingDir(rootPath.toFile())
          .command(rootPath.resolve(Env.isWindows() ? "gradlew.bat" : "gradlew").toString(), ":dynamic-config-integration-tests:unzipKit")
          .recordStdout()
          .redirectStderr()
          .build();
      if (process.waitFor() != 0) {
        throw new IllegalStateException("Failed building KIT. See logs below...\n" + process.getRecordedStdoutText());
      }
      File parent = Env.getModulePath().resolve("build").resolve("tc-db-kit").toFile();
      File[] children = parent.listFiles();
      if (children == null || children.length != 1) {
        throw new IllegalStateException("Kit directory not found in " + parent);
      }
      KIT_PATH = children[0].toPath();
      return KIT_PATH;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      ERROR = new IllegalStateException(e);
      throw ERROR;
    } catch (RuntimeException e) {
      ERROR = e;
      throw ERROR;
    }
  }

}
