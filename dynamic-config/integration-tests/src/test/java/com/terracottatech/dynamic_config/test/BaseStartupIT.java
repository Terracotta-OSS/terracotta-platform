/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.test;

import com.terracottatech.diagnostic.client.DiagnosticService;
import com.terracottatech.diagnostic.client.DiagnosticServiceFactory;
import com.terracottatech.dynamic_config.diagnostic.TopologyService;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.test.util.NodeProcess;
import com.terracottatech.testing.lock.PortLockingRule;
import com.terracottatech.utilities.PropertyResolver;
import com.terracottatech.utilities.fn.IntFn.IntTriConsumer;
import com.terracottatech.utilities.junit.TmpDir;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.SystemErrRule;
import org.junit.contrib.java.lang.system.SystemOutRule;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
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
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.terracottatech.dynamic_config.util.ParameterSubstitutor.getIpAddress;
import static java.nio.file.Files.walkFileTree;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.rangeClosed;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;
import static org.awaitility.Duration.FIVE_HUNDRED_MILLISECONDS;
import static org.awaitility.pollinterval.IterativePollInterval.iterative;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

public class BaseStartupIT {

  private static final int STRIPES = 2;
  private static final int NODES_PER_STRIPE = 2;

  static final int TIMEOUT = 30000;

  @Rule public final SystemOutRule out = new SystemOutRule().enableLog();
  @Rule public final SystemErrRule err = new SystemErrRule().enableLog();
  @Rule public final PortLockingRule ports = new PortLockingRule(STRIPES * NODES_PER_STRIPE);
  @Rule public final ExpectedSystemExit systemExit = ExpectedSystemExit.none();
  @Rule public final TmpDir tmpDir = new TmpDir();

  final Collection<NodeProcess> nodeProcesses = new ArrayList<>(ports.getPorts().length);

  @After
  public void tearDown() throws IOException {
    nodeProcesses.forEach(NodeProcess::close);
    ensureNodesNotAccessingExternalFiles();
  }

  Path getBaseDir() {
    return tmpDir.getRoot();
  }

  InetSocketAddress getServerAddress() {
    return InetSocketAddress.createUnresolved("localhost", ports.getPort());
  }

  Path copyConfigProperty(String configFile) throws Exception {
    Path src = Paths.get(NewServerStartupScriptIT.class.getResource(configFile).toURI());
    Path dest = getBaseDir().resolve(src.getFileName());
    Properties properties = new Properties();
    try (Reader reader = new InputStreamReader(Files.newInputStream(src), StandardCharsets.UTF_8)) {
      properties.load(reader);
    }
    Properties variables = variables();
    properties = new PropertyResolver(variables).resolveAll(properties);
    Files.createDirectories(getBaseDir());
    try (Writer writer = new OutputStreamWriter(Files.newOutputStream(dest), StandardCharsets.UTF_8)) {
      properties.store(writer, "");
    }
    return dest;
  }

  void forEachNode(IntTriConsumer accept) {
    combinations().forEach(vals -> accept.accept(
        vals[0], // stripeId
        vals[1], // nodeId
        vals[2] // port
    ));
  }

  Path licensePath() throws Exception {
    return Paths.get(BaseStartupIT.class.getResource("/license.xml").toURI());
  }

  Path configRepositoryPath() {
    return configRepositoryPath(1, 1);
  }

  Path configRepositoryPath(int stripeId, int nodeId) {
    return getBaseDir().resolve("repository").resolve("stripe" + stripeId).resolve("node-" + nodeId);
  }

  void waitedAssert(Callable<String> callable, Matcher<? super String> matcher) {
    Awaitility.await()
        .pollInterval(iterative(duration -> duration.plus(FIVE_HUNDRED_MILLISECONDS)).with().startDuration(FIVE_HUNDRED_MILLISECONDS))
        .atMost(new Duration(TIMEOUT, TimeUnit.MILLISECONDS))
        .until(callable, matcher);
  }

  Path copyServerConfigFiles(int stripeId, int nodeId, BiFunction<Integer, Integer, Function<String, Path>> fn) throws Exception {
    Path root = fn.apply(stripeId, nodeId).apply("config-repositories");
    Path dest = configRepositoryPath(stripeId, nodeId);
    copyDirectory(root, dest);
    return dest;
  }

  Function<String, Path> singleStripeSingleNode(int stripeId, int nodeId) {
    return (prefix) -> {
      try {
        String resourceName = "/" + prefix + "/single-stripe-single-node/stripe" + stripeId + "_node-" + nodeId;
        return Paths.get(NewServerStartupScriptIT.class.getResource(resourceName).toURI());
      } catch (URISyntaxException e) {
        throw new RuntimeException(e);
      }
    };
  }

  Function<String, Path> singleStripeMultiNode(int stripeId, int nodeId) {
    return (prefix) -> {
      try {
        String resourceName = "/" + prefix + "/single-stripe-multi-node/stripe" + stripeId + "_node-" + nodeId;
        return Paths.get(NewServerStartupScriptIT.class.getResource(resourceName).toURI());
      } catch (URISyntaxException e) {
        throw new RuntimeException(e);
      }
    };
  }

  Function<String, Path> multiStripe(int stripeId, int nodeId) {
    return (prefix) -> {
      try {
        String resourceName = "/" + prefix + "/multi-stripe/stripe" + stripeId + "_node-" + nodeId;
        return Paths.get(NewServerStartupScriptIT.class.getResource(resourceName).toURI());
      } catch (URISyntaxException e) {
        throw new RuntimeException(e);
      }
    };
  }

  Cluster getCluster(String host, int port) throws Exception {
    try (DiagnosticService diagnosticService = DiagnosticServiceFactory.fetch(
        InetSocketAddress.createUnresolved(host, port),
        getClass().getSimpleName(),
        5, SECONDS,
        5, SECONDS,
        null)) {
      return diagnosticService.getProxy(TopologyService.class).getCluster();
    }
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

  /**
   * Verify that all the configured path for the nodes are all enclosed and relative to the working
   * directory (user.dir) which is set for the tests to be inside build/test-data.
   * <p>
   * This ensures that the nodes will read and write files in an isolated manner and will not impact
   * other processes
   * <p>
   * All the path on the nodes should be relative to the working directory and also configured
   * accordingly to support the different stripes / node IDs.
   */
  private void ensureNodesNotAccessingExternalFiles() throws IOException {
    Stream<Path> s1 = of(
        getBaseDir(),
        Paths.get("backup"),
        Paths.get("logs"),
        Paths.get("metadata"),
        Paths.get("repository"),
        Paths.get("user-data"),
        Paths.get("user-data", "main"));
    // we use STRIPES * NODES_PER_STRIPE because we could support 1 stripe of 4 nodes
    Stream<Path> s2 = rangeClosed(1, STRIPES).mapToObj(stripeId -> rangeClosed(1, STRIPES * NODES_PER_STRIPE).mapToObj(nodeId -> of(
        Paths.get("metadata", "stripe" + stripeId),
        Paths.get("backup", "stripe" + stripeId),
        Paths.get("logs", "stripe" + stripeId),
        Paths.get("repository", "stripe" + stripeId),
        Paths.get("user-data", "main", "stripe" + stripeId),
        Paths.get("logs", "stripe" + stripeId, "node-" + nodeId),
        Paths.get("logs", "stripe" + stripeId, getIpAddress()),
        Paths.get("metadata", "stripe" + stripeId, "node-" + nodeId),
        Paths.get("metadata", "stripe" + stripeId, "node-" + nodeId, "platform-data"),
        Paths.get("metadata", "stripe" + stripeId, "node-" + nodeId, "platform-data", "entityData"),
        Paths.get("metadata", "stripe" + stripeId, "node-" + nodeId, "platform-data", "transactionsData"),
        Paths.get("metadata", "stripe" + stripeId, getIpAddress()),
        Paths.get("metadata", "stripe" + stripeId, getIpAddress(), "platform-data"),
        Paths.get("metadata", "stripe" + stripeId, getIpAddress(), "platform-data", "entityData"),
        Paths.get("metadata", "stripe" + stripeId, getIpAddress(), "platform-data", "transactionsData"),
        Paths.get("repository", "stripe" + stripeId, "node-" + nodeId),
        Paths.get("repository", "stripe" + stripeId, "node-" + nodeId, "config"),
        Paths.get("repository", "stripe" + stripeId, "node-" + nodeId, "license"),
        Paths.get("repository", "stripe" + stripeId, "node-" + nodeId, "sanskrit"),
        Paths.get("repository", "stripe" + stripeId, "node-" + nodeId, "sanskrit", "tmp"),
        Paths.get("user-data", "main", "stripe" + stripeId, "node-" + nodeId),
        Paths.get("user-data", "main", "stripe" + stripeId, getIpAddress())
    )).flatMap(identity())).flatMap(identity());
    List<Path> expected = concat(s1, s2).collect(toList());
    List<Path> unexpected = Files.walk(getBaseDir())
        .filter(p -> Files.isDirectory(p))
        .filter(p -> expected.stream().noneMatch(p::endsWith))
        .collect(toList());
    assertThat(unexpected.toString(), unexpected, hasSize(0));
  }

  private Stream<int[]> combinations() {
    int[] ports = this.ports.getPorts();
    return IntStream.rangeClosed(1, STRIPES)
        .mapToObj(stripeId -> IntStream.rangeClosed(1, NODES_PER_STRIPE)
            .mapToObj(nodeId -> new int[]{stripeId, nodeId, ports[STRIPES * (stripeId - 1) + (nodeId - 1)]}))
        .flatMap(identity());
  }

  private Properties variables() {
    return combinations().reduce(new Properties(), (props, vals) -> {
      int stripeId = vals[0];
      int nodeId = vals[1];
      int port = vals[2];
      String configRepoPath = configRepositoryPath(stripeId, nodeId).toString();
      props.setProperty(("PORT-" + stripeId + "-" + nodeId), String.valueOf(port));
      props.setProperty(("GROUP-PORT-" + stripeId + "-" + nodeId), String.valueOf(port + 10));
      props.setProperty(("CONFIG-REPO-" + stripeId + "-" + nodeId), configRepoPath);
      return props;
    }, (p1, p2) -> {
      throw new UnsupportedOperationException();
    });
  }
}