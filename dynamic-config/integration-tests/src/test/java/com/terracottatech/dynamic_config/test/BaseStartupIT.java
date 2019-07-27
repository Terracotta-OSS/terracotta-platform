/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.test;

import com.terracottatech.dynamic_config.test.util.NodeProcess;
import com.terracottatech.dynamic_config.test.util.TmpDir;
import com.terracottatech.testing.lock.PortLockingRule;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.SystemErrRule;
import org.junit.contrib.java.lang.system.SystemOutRule;

import java.io.File;
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
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Pattern;

import static java.nio.file.Files.walkFileTree;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.rangeClosed;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;
import static org.awaitility.pollinterval.IterativePollInterval.iterative;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

public class BaseStartupIT {

  static final int TIMEOUT = 30000;

  @Rule public final SystemOutRule out = new SystemOutRule().enableLog();
  @Rule public final SystemErrRule err = new SystemErrRule().enableLog();
  @Rule public final PortLockingRule ports = new PortLockingRule(4);
  @Rule public final ExpectedSystemExit systemExit = ExpectedSystemExit.none();
  @Rule public final TmpDir tmpDir = new TmpDir(new File("build/test-data").getAbsoluteFile().toPath());

  final Collection<NodeProcess> nodeProcesses = new ArrayList<>(ports.getPorts().length);

  @Before
  public void setUp() throws Exception {
    // ensure the test is not leaving some files in a common folder such as ~/terracotta
    assertFalse(new File(System.getProperty("user.home"), "terracotta").exists());
  }

  @After
  public void tearDown() throws IOException {
    nodeProcesses.forEach(NodeProcess::close);

    // ensure the test is not leaving some files in a common folder such as ~/terracotta
    assertFalse(new File(System.getProperty("user.home"), "terracotta").exists());

    // ensure that created server folders are correctly configured and only contain expected folders
    List<String> allowed = concat(
        of("", "backup", "logs", "metadata", "repository", "user-data", "user-data/main"),
        rangeClosed(1, 4).mapToObj(idx -> of(
            "logs/node-" + idx,
            "metadata/node-" + idx,
            "metadata/node-" + idx + "/platform-data",
            "metadata/node-" + idx + "/platform-data/entityData",
            "metadata/node-" + idx + "/platform-data/transactionsData",
            "repository/node-" + idx,
            "repository/node-" + idx + "/config",
            "repository/node-" + idx + "/license",
            "repository/node-" + idx + "/sanskrit",
            "user-data/main/node-" + idx
        )).flatMap(identity())
    ).collect(toList());
    List<String> structure = Files.walk(getBaseDir())
        .filter(path -> Files.isDirectory(path))
        .map(path -> getBaseDir().relativize(path))
        .map(Path::toString)
        .collect(toList());
    structure.removeAll(allowed);
    assertThat(structure.toString(), structure, hasSize(0));
  }

  protected Path getBaseDir() {
    return tmpDir.getRoot();
  }

  InetSocketAddress getServerAddress() {
    return InetSocketAddress.createUnresolved("localhost", ports.getPort());
  }

  Path copyConfigProperty(String configFile) throws Exception {
    Path original = Paths.get(NewServerStartupScriptIT.class.getResource(configFile).toURI());
    String contents = new String(Files.readAllBytes(original));
    int[] ports = this.ports.getPorts();
    for (int i = 0, loopCounter = 1; i < ports.length; i += 2, loopCounter++) {
      contents = contents
          .replaceAll(Pattern.quote("${PORT-" + loopCounter + "}"), String.valueOf(ports[i]))
          .replaceAll(Pattern.quote("${GROUP-PORT-" + loopCounter + "}"), String.valueOf(ports[i + 1]))
          .replaceAll(Pattern.quote("${CONFIG-REPO-" + loopCounter + "}"), configRepositoryPath(loopCounter).toString());
    }

    Path newPath = getBaseDir().resolve(original.getFileName());
    Files.write(newPath, contents.getBytes(StandardCharsets.UTF_8));
    newPath.toFile().deleteOnExit();
    return newPath;
  }

  Path licensePath() throws Exception {
    return Paths.get(BaseStartupIT.class.getResource("/license.xml").toURI());
  }

  Path configRepositoryPath() {
    return configRepositoryPath(1);
  }

  Path configRepositoryPath(int nodeIdx) {
    return getBaseDir().resolve("repository").resolve("node-" + nodeIdx);
  }

  void waitedAssert(Callable<String> callable, Matcher<? super String> matcher) {
    Awaitility.await()
        .pollInterval(iterative(duration -> duration.multiply(2)).with().startDuration(Duration.TWO_HUNDRED_MILLISECONDS))
        .atMost(new Duration(TIMEOUT, TimeUnit.MILLISECONDS))
        .until(callable, matcher);
  }

  Path copyServerConfigFiles(Function<String, Path> nomadRootFunction) throws Exception {
    Path root = nomadRootFunction.apply("config-repositories");
    Path dest = configRepositoryPath();
    copyDirectory(root, dest);
    return dest;
  }

  Function<String, Path> singleStripeSingleNode(int stripeId, String nodeName) {
    return (prefix) -> {
      try {
        String resourceName = "/" + prefix + "/single-stripe-single-node/stripe" + stripeId + "_" + nodeName;
        return Paths.get(NewServerStartupScriptIT.class.getResource(resourceName).toURI());
      } catch (URISyntaxException e) {
        throw new RuntimeException(e);
      }
    };
  }

  Function<String, Path> singleStripeMultiNode(int stripeId, String nodeName) {
    return (prefix) -> {
      try {
        String resourceName = "/" + prefix + "/single-stripe-multi-node/stripe" + stripeId + "_" + nodeName;
        return Paths.get(NewServerStartupScriptIT.class.getResource(resourceName).toURI());
      } catch (URISyntaxException e) {
        throw new RuntimeException(e);
      }
    };
  }

  Function<String, Path> multiStripe(int stripeId, String nodeName) {
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
    Files.createDirectories(destination);
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
