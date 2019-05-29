/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.test;

import com.terracottatech.testing.lock.LockingPortChooser;
import com.terracottatech.testing.lock.LockingPortChoosers;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.readAllLines;
import static java.nio.file.Files.walkFileTree;
import static java.nio.file.Files.write;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.pollinterval.IterativePollInterval.iterative;

public class BaseStartupIT {
  final LockingPortChooser portChooser = LockingPortChoosers.getFileLockingPortChooser();
  private final Pattern PID_PATTERN = Pattern.compile(".*PID is (\\d+)");
  private volatile int serverPid = -1;

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule
  public final SystemOutRule systemOutRule = new SystemOutRule().enableLog();

  @After
  public void tearDown() throws Exception {
    if (this.serverPid != -1) {
      System.out.println("Killing the server with pid: " + this.serverPid);
      if (isWindows()) {
        Runtime.getRuntime().exec("taskkill /F /t /pid " + this.serverPid);
      } else {
        Runtime.getRuntime().exec("kill " + this.serverPid);
      }
    }
  }

  static boolean isWindows() {
    return System.getProperty("os.name").toLowerCase().startsWith("windows");
  }

  void waitedAssert(Callable<String> callable, Matcher<? super String> matcher) {
    Awaitility.await()
        .pollInterval(iterative(duration -> duration.multiply(2)).with().startDuration(Duration.TWO_HUNDRED_MILLISECONDS))
        .atMost(Duration.ONE_MINUTE)
        .until(callable, matcher);
  }

  void startServer(Path scriptPath, String... args) throws Exception {
    if (!Files.exists(scriptPath)) {
      fail("Terracotta server start script does not exist: " + scriptPath);
    }

    List<String> processArgs = new ArrayList<>();
    processArgs.add(scriptPath.toString());
    processArgs.addAll(asList(args));

    Process process = new ProcessBuilder(processArgs).start();
    CompletableFuture.runAsync(() -> printServerLogs(process.getInputStream()));
  }

  Path configRepoPath(Function<String, Path> nomadRootFunction, String serverName) throws Exception {
    String directory = "config-repositories";
    String platform = isWindows() ? "windows" : "linux";
    Path configurationRepoPath = nomadRootFunction.apply(directory + "/" + platform);
    Path temporaryPath = temporaryFolder.newFolder().toPath();

    copyDirectory(configurationRepoPath, temporaryPath);

    if (isWindows()) {
      changeLineSeparator(Paths.get("sanskrit").resolve("append.log"), configurationRepoPath, temporaryPath);
      changeLineSeparator(Paths.get("config").resolve("cluster-config." + serverName + ".1.xml"), configurationRepoPath, temporaryPath);
    }

    return temporaryPath;
  }

  Function<String, Path> singleStripeSingleNodeNomadRoot(String stripeName, String nodeName) {
    return (prefix) -> {
      try {
        String resourceName = "/" + prefix + "/single-stripe-single-node/" + stripeName + "_" + nodeName;
        return Paths.get(NewServerStartupScriptIT.class.getResource(resourceName).toURI());
      } catch (URISyntaxException e) {
        throw new RuntimeException(e);
      }
    };
  }

  Function<String, Path> singleStripeMultiNodeNomadRoot(String stripeName, String nodeName) {
    return (prefix) -> {
      try {
        String resourceName = "/" + prefix + "/single-stripe-multi-node/" + stripeName + "_" + nodeName;
        return Paths.get(NewServerStartupScriptIT.class.getResource(resourceName).toURI());
      } catch (URISyntaxException e) {
        throw new RuntimeException(e);
      }
    };
  }

  Function<String, Path> multiStripeNomadRoot(String stripeName, String nodeName) {
    return (prefix) -> {
      try {
        String resourceName = "/" + prefix + "/multi-stripe/" + stripeName + "_" + nodeName;
        return Paths.get(NewServerStartupScriptIT.class.getResource(resourceName).toURI());
      } catch (URISyntaxException e) {
        throw new RuntimeException(e);
      }
    };
  }

  private void printServerLogs(InputStream inputStream) {
    try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
      String line;
      while ((line = bufferedReader.readLine()) != null) {
        System.out.println(line);
        java.util.regex.Matcher matcher = PID_PATTERN.matcher(line);
        if (matcher.matches()) {
          this.serverPid = Integer.parseInt(matcher.group(1));
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static void changeLineSeparator(Path file, Path configurationRepoPath, Path temporaryPath) throws IOException {
    List<String> lines = readAllLines(configurationRepoPath.resolve(file), UTF_8);
    String modifiedContent = String.join(System.lineSeparator(), lines) + System.lineSeparator();
    write(temporaryPath.resolve(file), modifiedContent.getBytes(UTF_8));
  }

  private static void copyDirectory(Path source, Path destination) throws IOException {
    walkFileTree(source, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        Files.createDirectories(destination.resolve(source.relativize(dir)));
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Files.copy(file, destination.resolve(source.relativize(file)));
        return FileVisitResult.CONTINUE;
      }
    });
  }
}
