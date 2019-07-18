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
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Pattern;

import static java.nio.file.Files.walkFileTree;
import static org.awaitility.pollinterval.IterativePollInterval.iterative;

public class BaseStartupIT {
  @Rule public final TemporaryFolder temporaryFolder = new TemporaryFolder();
  @Rule public final SystemOutRule out = new SystemOutRule().enableLog();
  @Rule public final SystemErrRule err = new SystemErrRule().enableLog();
  @Rule public final PortLockingRule ports = new PortLockingRule(4);
  @Rule public final ExpectedSystemExit systemExit = ExpectedSystemExit.none();

  static final int TIMEOUT = 30000;
  final Collection<NodeProcess> nodeProcesses = new ArrayList<>(ports.getPorts().length);

  @After
  public void tearDown() {
    nodeProcesses.forEach(NodeProcess::close);
  }

  InetSocketAddress getServerAddress() {
    return InetSocketAddress.createUnresolved("localhost", ports.getPort());
  }

  String configFilePath(String configFile) throws Exception {
    Path original = Paths.get(NewServerStartupScriptIT.class.getResource(configFile).toURI());
    String contents = new String(Files.readAllBytes(original));
    int[] ports = this.ports.getPorts();
    for (int i = 0, loopCounter = 1; i < ports.length; i += 2, loopCounter++) {
      contents = contents
          .replaceAll(Pattern.quote("${PORT-" + loopCounter + "}"), String.valueOf(ports[i]))
          .replaceAll(Pattern.quote("${GROUP-PORT-" + loopCounter + "}"), String.valueOf(ports[i + 1]));
    }

    Path newPath = temporaryFolder.newFile().toPath();
    Files.write(newPath, contents.getBytes(StandardCharsets.UTF_8));
    return newPath.toString();
  }

  String licensePath() throws Exception {
    return Paths.get(BaseStartupIT.class.getResource("/license.xml").toURI()).toString();
  }

  void waitedAssert(Callable<String> callable, Matcher<? super String> matcher) {
    Awaitility.await()
        .pollInterval(iterative(duration -> duration.multiply(2)).with().startDuration(Duration.TWO_HUNDRED_MILLISECONDS))
        .atMost(new Duration(TIMEOUT, TimeUnit.MILLISECONDS))
        .until(callable, matcher);
  }

  String configRepoPath(Function<String, Path> nomadRootFunction) throws Exception {
    Path configurationRepoPath = nomadRootFunction.apply("config-repositories");
    Path temporaryPath = temporaryFolder.newFolder().toPath();
    copyDirectory(configurationRepoPath, temporaryPath);
    return temporaryPath.toString();
  }

  Function<String, Path> singleStripeSingleNodeNomadRoot(int stripeId, String nodeName) {
    return (prefix) -> {
      try {
        String resourceName = "/" + prefix + "/single-stripe-single-node/stripe" + stripeId + "_" + nodeName;
        return Paths.get(NewServerStartupScriptIT.class.getResource(resourceName).toURI());
      } catch (URISyntaxException e) {
        throw new RuntimeException(e);
      }
    };
  }

  Function<String, Path> singleStripeMultiNodeNomadRoot(int stripeId, String nodeName) {
    return (prefix) -> {
      try {
        String resourceName = "/" + prefix + "/single-stripe-multi-node/stripe" + stripeId + "_" + nodeName;
        return Paths.get(NewServerStartupScriptIT.class.getResource(resourceName).toURI());
      } catch (URISyntaxException e) {
        throw new RuntimeException(e);
      }
    };
  }

  Function<String, Path> multiStripeNomadRoot(int stripeId, String nodeName) {
    return (prefix) -> {
      try {
        String resourceName = "/" + prefix + "/multi-stripe/stripe" + stripeId + "_" + nodeName;
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
