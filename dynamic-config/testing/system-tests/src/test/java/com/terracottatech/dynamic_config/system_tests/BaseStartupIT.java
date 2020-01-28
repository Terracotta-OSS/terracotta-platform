/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.system_tests;

import com.terracottatech.diagnostic.client.DiagnosticService;
import com.terracottatech.diagnostic.client.DiagnosticServiceFactory;
import com.terracottatech.dynamic_config.api.model.Cluster;
import com.terracottatech.dynamic_config.api.service.IParameterSubstitutor;
import com.terracottatech.dynamic_config.api.service.TopologyService;
import com.terracottatech.dynamic_config.cli.config_tool.ConfigTool;
import com.terracottatech.dynamic_config.server.service.ParameterSubstitutor;
import com.terracottatech.dynamic_config.system_tests.util.ConfigRepositoryGenerator;
import com.terracottatech.dynamic_config.system_tests.util.Env;
import com.terracottatech.dynamic_config.system_tests.util.Kit;
import com.terracottatech.dynamic_config.system_tests.util.NodeProcess;
import com.terracottatech.dynamic_config.system_tests.util.PropertyResolver;
import com.terracottatech.testing.TmpDir;
import com.terracottatech.testing.lock.PortLockingRule;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.SystemErrRule;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.net.InetSocketAddress;
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
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.nio.file.Files.walkFileTree;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.rangeClosed;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;
import static org.awaitility.Duration.FIVE_HUNDRED_MILLISECONDS;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.terracotta.config.util.ParameterSubstitutor.getIpAddress;

public class BaseStartupIT {
  private static final Logger LOGGER = LoggerFactory.getLogger(BaseStartupIT.class);
  private static final boolean CI = System.getProperty("JOB_NAME") != null;
  static final int TIMEOUT = CI ? 90 : 20;
  static final IParameterSubstitutor PARAMETER_SUBSTITUTOR = new ParameterSubstitutor();

  @Rule
  public final SystemOutRule out = new SystemOutRule().enableLog();
  @Rule
  public final SystemErrRule err = new SystemErrRule().enableLog();
  @Rule
  public final PortLockingRule ports;

  @Rule
  public final TmpDir tmpDir = new TmpDir();

  private final int stripes;
  private final int nodesPerStripe;
  private final Collection<NodeProcess> nodeProcesses;

  public BaseStartupIT() {
    this(1, 1);
  }

  public BaseStartupIT(int nodesPerStripe, int stripes) {
    this.nodesPerStripe = nodesPerStripe;
    this.stripes = stripes;
    this.ports = new PortLockingRule(2 * this.stripes * this.nodesPerStripe);
    this.nodeProcesses = new ArrayList<>(ports.getPorts().length);
  }

  @After
  public void tearDown() {
    nodeProcesses.forEach(NodeProcess::close);
    ensureNodesNotAccessingExternalFiles();
  }

  final NodeProcess startNode(String... cli) {
    NodeProcess process = NodeProcess.startNode(Kit.getOrCreatePath(), getBaseDir(), cli);
    nodeProcesses.add(process);
    return process;
  }

  final NodeProcess startTcServer(String... cli) {
    NodeProcess process = NodeProcess.startTcServer(Kit.getOrCreatePath(), getBaseDir(), cli);
    nodeProcesses.add(process);
    return process;
  }

  final NodeProcess getNodeProcess() {
    return nodeProcesses.stream().findFirst().get();
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
    Properties loaded = new Properties();
    try (Reader reader = new InputStreamReader(Files.newInputStream(src), StandardCharsets.UTF_8)) {
      loaded.load(reader);
    }
    Properties variables = variables();
    Properties resolved = new PropertyResolver(variables).resolveAll(loaded);
    if (Env.isWindows()) {
      // Convert all / to \\ for Windows.
      // This assumes we only use / and \\ for paths in property values
      resolved.stringPropertyNames()
          .stream()
          .filter(key -> resolved.getProperty(key).contains("/"))
          .forEach(key -> resolved.setProperty(key, resolved.getProperty(key).replace("/", "\\")));
    }
    Files.createDirectories(getBaseDir());
    try (Writer writer = new OutputStreamWriter(Files.newOutputStream(dest), StandardCharsets.UTF_8)) {
      resolved.store(writer, "");
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

  Path getNodeRepositoryDir() {
    return getNodeRepositoryDir(1, 1);
  }

  Path getNodeRepositoryDir(int stripeId, int nodeId) {
    return getBaseDir().resolve("repository").resolve("stripe" + stripeId).resolve("node-" + nodeId);
  }

  void waitedAssert(Callable<String> callable, Matcher<? super String> matcher) {
    waitedAssert(callable, matcher, TIMEOUT);
  }

  void waitedAssert(Callable<String> callable, Matcher<? super String> matcher, int timeout) {
    Awaitility.await()
        // do not use iterative because it slows down the whole test suite considerably, especially in case of a failing process causing a timeout
        .pollInterval(FIVE_HUNDRED_MILLISECONDS)
        .atMost(new Duration(timeout, TimeUnit.SECONDS))
        .until(callable, matcher);
  }

  void assertCommandSuccessful() {
    waitedAssert(out::getLog, containsString("Command successful"));
    out.clearLog();
  }

  Path generateNodeRepositoryDir(int stripeId, int nodeId, Consumer<ConfigRepositoryGenerator> fn) throws Exception {
    Path nodeRepositoryDir = getNodeRepositoryDir(stripeId, nodeId);
    Path repositoriesDir = getBaseDir().resolve("repositories");
    ConfigRepositoryGenerator clusterGenerator = new ConfigRepositoryGenerator(repositoriesDir, ports.getPorts());
    LOGGER.debug("Generating cluster node repositories into: {}", repositoriesDir);
    fn.accept(clusterGenerator);
    copyDirectory(repositoriesDir.resolve("stripe" + stripeId + "_node-" + nodeId), nodeRepositoryDir);
    LOGGER.debug("Created node repository into: {}", nodeRepositoryDir);
    return nodeRepositoryDir;
  }

  Cluster getUpcomingCluster(String host, int port) throws Exception {
    try (DiagnosticService diagnosticService = DiagnosticServiceFactory.fetch(
        InetSocketAddress.createUnresolved(host, port),
        getClass().getSimpleName(),
        java.time.Duration.ofSeconds(5),
        java.time.Duration.ofSeconds(5),
        null)) {
      return diagnosticService.getProxy(TopologyService.class).getUpcomingNodeContext().getCluster();
    }
  }

  Cluster getRuntimeCluster(String host, int port) throws Exception {
    try (DiagnosticService diagnosticService = DiagnosticServiceFactory.fetch(
        InetSocketAddress.createUnresolved(host, port),
        getClass().getSimpleName(),
        java.time.Duration.ofSeconds(5),
        java.time.Duration.ofSeconds(5),
        null)) {
      return diagnosticService.getProxy(TopologyService.class).getRuntimeNodeContext().getCluster();
    }
  }

  void activateCluster() throws Exception {
    ConfigTool.start("activate", "-s", "localhost:" + ports.getPort(), "-n", "tc-cluster", "-l", licensePath().toString());
    assertCommandSuccessful();
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
  private void ensureNodesNotAccessingExternalFiles() {
    Stream<Path> s1 = of(
        getBaseDir(),
        Paths.get("backup"),
        Paths.get("logs"),
        Paths.get("metadata"),
        Paths.get("repository"),
        Paths.get("user-data"),
        Paths.get("user-data", "main"),
        Paths.get("repositories")
    );
    // Do not call `getIpAddress()` sevral times because on MacOs each call can last up to 5 sec
    String ipAddress = getIpAddress();
    // we use STRIPES * NODES_PER_STRIPE because we could support 1 stripe of 4 nodes
    Stream<Path> s2 = rangeClosed(1, stripes).mapToObj(stripeId -> rangeClosed(1, stripes * nodesPerStripe).mapToObj(nodeId -> of(
        Paths.get("metadata", "stripe" + stripeId),
        Paths.get("backup", "stripe" + stripeId),
        Paths.get("logs", "stripe" + stripeId),
        Paths.get("logs", "stripe" + stripeId, "node-" + nodeId),
        Paths.get("logs", "stripe" + stripeId, ipAddress),
        Paths.get("user-data", "main", "stripe" + stripeId),
        Paths.get("user-data", "main", "stripe" + stripeId, "node-" + nodeId),
        Paths.get("user-data", "main", "stripe" + stripeId + "-node" + nodeId + "-data-dir-1"),
        Paths.get("user-data", "main", "stripe" + stripeId + "-node" + nodeId + "-data-dir-2"),
        Paths.get("user-data", "main", "stripe" + stripeId, ipAddress),
        Paths.get("metadata", "stripe" + stripeId),
        Paths.get("metadata", "stripe" + stripeId, "node-" + nodeId),
        Paths.get("metadata", "stripe" + stripeId, "node-" + nodeId, "platform-data"),
        Paths.get("metadata", "stripe" + stripeId, "node-" + nodeId, "platform-data", "entityData"),
        Paths.get("metadata", "stripe" + stripeId, "node-" + nodeId, "platform-data", "transactionsData"),
        Paths.get("metadata", "stripe" + stripeId, ipAddress),
        Paths.get("metadata", "stripe" + stripeId, ipAddress, "platform-data"),
        Paths.get("metadata", "stripe" + stripeId, ipAddress, "platform-data", "entityData"),
        Paths.get("metadata", "stripe" + stripeId, ipAddress, "platform-data", "transactionsData"),
        Paths.get("repository", "stripe" + stripeId),
        Paths.get("repository", "stripe" + stripeId, "node-" + nodeId),
        Paths.get("repository", "stripe" + stripeId, "node-" + nodeId, "config"),
        Paths.get("repository", "stripe" + stripeId, "node-" + nodeId, "license"),
        Paths.get("repository", "stripe" + stripeId, "node-" + nodeId, "sanskrit"),
        Paths.get("repository", "stripe" + stripeId, "node-" + nodeId, "sanskrit", "tmp"),
        Paths.get("repositories", "stripe" + stripeId + "_node-" + nodeId),
        Paths.get("repositories", "stripe" + stripeId + "_node-" + nodeId, "config"),
        Paths.get("repositories", "stripe" + stripeId + "_node-" + nodeId, "license"),
        Paths.get("repositories", "stripe" + stripeId + "_node-" + nodeId, "sanskrit"),
        Paths.get("repositories", "stripe" + stripeId + "_node-" + nodeId, "sanskrit", "tmp")
    )).flatMap(identity())).flatMap(identity());

    List<Path> expected = concat(s1, s2).collect(toList());
    try (Stream<Path> stream = Files.walk(getBaseDir())) {
      List<Path> unexpected = stream.filter(p -> Files.isDirectory(p))
          .filter(p -> !p.toString().contains("backup-platform-data-"))
          .filter(p -> expected.stream().noneMatch(p::endsWith))
          .collect(toList());
      assertThat(unexpected.toString(), unexpected, hasSize(0));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private Stream<int[]> combinations() {
    int[] ports = this.ports.getPorts();
    return IntStream.rangeClosed(1, stripes)
        .mapToObj(stripeId -> IntStream.rangeClosed(1, nodesPerStripe)
            .mapToObj(nodeId -> new int[]{stripeId, nodeId, ports[stripes * (stripeId - 1) + (nodeId - 1)]}))
        .flatMap(identity());
  }

  private Properties variables() {
    return combinations().reduce(new Properties(), (props, vals) -> {
      int stripeId = vals[0];
      int nodeId = vals[1];
      int port = vals[2];
      String configRepoPath = getNodeRepositoryDir(stripeId, nodeId).toString();
      props.setProperty(("PORT-" + stripeId + "-" + nodeId), String.valueOf(port));
      props.setProperty(("GROUP-PORT-" + stripeId + "-" + nodeId), String.valueOf(port + 10));
      props.setProperty(("CONFIG-REPO-" + stripeId + "-" + nodeId), configRepoPath);
      return props;
    }, (p1, p2) -> {
      throw new UnsupportedOperationException();
    });
  }

  @FunctionalInterface
  public interface IntTriConsumer {
    void accept(int a, int b, int c);
  }
}