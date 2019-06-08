/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.test;

import com.terracottatech.dynamic_config.test.util.NodeProcess;
import com.terracottatech.testing.lock.PortLockingRule;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.SystemErrRule;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.regex.Pattern;

import static java.nio.file.Files.walkFileTree;
import static org.awaitility.pollinterval.IterativePollInterval.iterative;

public class BaseStartupIT {

  @Rule public final TemporaryFolder temporaryFolder = new TemporaryFolder();
  @Rule public final SystemOutRule out = new SystemOutRule().enableLog();
  @Rule public final SystemErrRule err = new SystemErrRule().enableLog();
  @Rule public final PortLockingRule ports = new PortLockingRule(2);

  volatile NodeProcess nodeProcess;

  @After
  public void tearDown() {
    if (nodeProcess != null) {
      nodeProcess.close();
    }
  }

  InetSocketAddress getServerAddress() {
    return InetSocketAddress.createUnresolved("localhost", ports.getPort());
  }

  Path configFilePath() throws Exception {
    return configFilePath("", String.valueOf(ports.getPort()));
  }

  Path configFilePath(String suffix, String port) throws Exception {
    String resourceName = "/config-property-files/single-stripe" + suffix + ".properties";
    Path original = Paths.get(NewServerStartupScriptIT.class.getResource(resourceName).toURI());
    String contents = new String(Files.readAllBytes(original));
    String replacedContents = contents.replaceAll(Pattern.quote("${PORT}"), port);
    Path newPath = temporaryFolder.newFile().toPath();
    Files.write(newPath, replacedContents.getBytes(StandardCharsets.UTF_8));
    return newPath;
  }

  void waitedAssert(Callable<String> callable, Matcher<? super String> matcher) {
    Awaitility.await()
        .pollInterval(iterative(duration -> duration.multiply(2)).with().startDuration(Duration.TWO_HUNDRED_MILLISECONDS))
        .atMost(Duration.ONE_MINUTE)
        .until(callable, matcher);
  }

  Path configRepoPath(Function<String, Path> nomadRootFunction) throws Exception {
    Path configurationRepoPath = nomadRootFunction.apply("config-repositories");
    Path temporaryPath = temporaryFolder.newFolder().toPath();
    copyDirectory(configurationRepoPath, temporaryPath);
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
