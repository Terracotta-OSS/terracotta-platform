/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.test.util;

import org.terracotta.ipceventbus.proc.AnyProcess;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

import static java.lang.System.lineSeparator;

/**
 * @author Mathieu Carbou
 */
public class Kit {

  // memoize results for subsequent test execution to fasten build feedback in case of error
  private static Path kitPath;
  private static RuntimeException error;

  public static Optional<Path> getPath() {
    return Optional.ofNullable(System.getProperty("kitInstallationPath")).map(s -> Paths.get(s));
  }

  public static Path getOrCreatePath() {
    Path kitPath = getPath().orElseGet(() -> {
      Path path = build();
      System.setProperty("kitInstallationPath", path.toAbsolutePath().toString());
      return path;
    });
    // copy custom logback-ext
    try {
      Path logConfg = Paths.get("src", "test", "resources", "logback-ext.xml");
      if (Files.exists(logConfg)) {
        Files.copy(logConfg, kitPath.resolve("server").resolve("lib").resolve("logback-ext.xml"), StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return kitPath;
  }

  private static synchronized Path build() {
    if (kitPath != null) {
      return kitPath;
    }
    if (error != null) {
      throw error;
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
        throw new IllegalStateException("Failed building KIT. See logs below..." + lineSeparator() + process.getRecordedStdoutText());
      }
      File parent = Env.getModulePath().resolve("build").resolve("tc-db-kit").toFile();
      File[] children = parent.listFiles();
      if (children == null || children.length != 1) {
        throw new IllegalStateException("Kit directory not found in " + parent);
      }
      kitPath = children[0].toPath();
      return kitPath;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      error = new IllegalStateException(e);
      throw error;
    } catch (RuntimeException e) {
      error = e;
      throw error;
    }
  }

}
