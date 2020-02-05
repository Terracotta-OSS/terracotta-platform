/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.system_tests;

import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.SystemErrRule;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.common.struct.Tuple2;
import org.terracotta.diagnostic.client.DiagnosticService;
import org.terracotta.diagnostic.client.DiagnosticServiceFactory;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.cli.config_tool.ConfigTool;
import org.terracotta.dynamic_config.server.service.ParameterSubstitutor;
import org.terracotta.dynamic_config.system_tests.util.ConfigRepositoryGenerator;
import org.terracotta.dynamic_config.system_tests.util.Env;
import org.terracotta.dynamic_config.system_tests.util.Kit;
import org.terracotta.dynamic_config.system_tests.util.NodeProcess;
import org.terracotta.dynamic_config.system_tests.util.PropertyResolver;
import org.terracotta.port_locking.PortLockingRule;
import org.terracotta.testing.TmpDir;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.nio.file.Files.walkFileTree;
import static java.util.Arrays.asList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.rangeClosed;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;
import static org.awaitility.Duration.FIVE_HUNDRED_MILLISECONDS;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.junit.Assert.assertThat;
import static org.terracotta.common.struct.Tuple2.tuple2;
import static org.terracotta.config.util.ParameterSubstitutor.getIpAddress;

public class DynamicConfigIT {
  private static final Logger LOGGER = LoggerFactory.getLogger(DynamicConfigIT.class);
  private static final boolean CI = System.getProperty("JOB_NAME") != null;
  static final IParameterSubstitutor PARAMETER_SUBSTITUTOR = new ParameterSubstitutor();

  @Rule
  public final SystemOutRule out = new SystemOutRule().enableLog();
  @Rule
  public final SystemErrRule err = new SystemErrRule().enableLog();
  @Rule
  public final PortLockingRule ports;
  @Rule
  public final TmpDir tmpDir = new TmpDir();

  protected int timeout = CI ? 90 : 30;

  private final int stripes;
  private final boolean autoStart;
  private final boolean autoActivate;
  private final int nodesPerStripe;
  private final Map<String, NodeProcess> nodeProcesses = new ConcurrentHashMap<>();

  public DynamicConfigIT() {
    ClusterDefinition clusterDefinition = getClass().getAnnotation(ClusterDefinition.class);
    this.stripes = clusterDefinition.stripes();
    this.nodesPerStripe = clusterDefinition.nodesPerStripe();
    this.autoStart = clusterDefinition.autoStart();
    this.autoActivate = clusterDefinition.autoActivate();
    this.ports = new PortLockingRule(2 * this.stripes * this.nodesPerStripe);
  }

  @Before
  public void before() throws Exception {
    if (autoStart) {
      startNodes();
    }
  }

  @After
  public void after() throws Exception {
    nodeProcesses.values().forEach(NodeProcess::close);
    nodeProcesses.clear();
    ensureNodesNotAccessingExternalFiles();
  }

  protected final void startNodes() throws Exception {
    final int nodeCount = stripes * nodesPerStripe;

    ExecutorService executorService = Executors.newFixedThreadPool(nodeCount);
    try {
      CompletionService<NodeProcess> completionService = new ExecutorCompletionService<>(executorService);

      out.clearLog();
      nodeDefinitions().forEach(tuple -> completionService.submit(() -> startNode(tuple.t1, tuple.t2)));

      for (int i = 0; i < nodeCount; i++) {
        completionService.take().get();
      }
    } finally {
      executorService.shutdown();
    }

    // if not 1-node cluster activation, we need the activate CLI
    if (autoActivate && (stripes > 1 || nodesPerStripe > 1)) {
      for (int nodeId = 2; nodeId <= nodesPerStripe; nodeId++) {
        ConfigTool.start("attach", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(1, nodeId));
        assertCommandSuccessful();
      }
      for (int stripeId = 2; stripeId <= stripes; stripeId++) {
        ConfigTool.start("attach", "-t", "stripe", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(stripeId, 1));
        assertCommandSuccessful();
        for (int nodeId = 2; nodeId <= nodesPerStripe; nodeId++) {
          ConfigTool.start("attach", "-d", "localhost:" + getNodePort(stripeId, 1), "-s", "localhost:" + getNodePort(stripeId, nodeId));
          assertCommandSuccessful();
        }
      }
      activateCluster(() -> {
        // we are waiting for activation
        String[] actives = new String[stripes];
        Arrays.fill(actives, "Moved to State[ ACTIVE-COORDINATOR ]");
        waitUntil(out::getLog, stringContainsInOrder(asList(actives)));
        String[] passives = new String[nodeCount - stripes];
        if (passives.length > 0) {
          Arrays.fill(passives, "Moved to State[ PASSIVE-STANDBY ]");
          waitUntil(out::getLog, stringContainsInOrder(asList(passives)));
        }
      });
    } else if (autoActivate) {
      // stripes == 1 && nodesPerStripe == 1
      waitUntil(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));
    } else {
      // we wait for diagnostic mode
      String[] status = new String[nodeCount];
      Arrays.fill(status, "Started the server in diagnostic mode");
      waitUntil(out::getLog, stringContainsInOrder(asList(status)));
    }
  }

  protected final NodeProcess startNode(int stripeId, int nodeId) throws Exception {
    int nodePort = getNodePort(stripeId, nodeId);
    int groupPort = nodePort + 1;
    return startNode(stripeId, nodeId, nodePort, groupPort);
  }

  protected NodeProcess startNode(int stripeId, int nodeId, int port, int groupPort) throws Exception {
    String licensePath = licensePath();
    return autoActivate && stripes == 1 && nodesPerStripe == 1 && licensePath == null ?
        startNode(
            stripeId, nodeId,
            "--cluster-name", "tc-cluster",
            "--node-name", "node-" + nodeId,
            "--node-hostname", "localhost",
            "--node-port", String.valueOf(port),
            "--node-group-port", String.valueOf(groupPort),
            "--node-log-dir", "logs/stripe" + stripeId + "/node-" + nodeId,
            "--node-backup-dir", "backup/stripe" + stripeId,
            "--node-metadata-dir", "metadata/stripe" + stripeId,
            "--node-repository-dir", "repository/stripe" + stripeId + "/node-" + nodeId,
            "--data-dirs", "main:user-data/main/stripe" + stripeId) :
        autoActivate && stripes == 1 && nodesPerStripe == 1 ?
            startNode(
                stripeId, nodeId,
                "--cluster-name", "tc-cluster",
                "--license-file", licensePath,
                "--node-name", "node-" + nodeId,
                "--node-hostname", "localhost",
                "--node-port", String.valueOf(port),
                "--node-group-port", String.valueOf(groupPort),
                "--node-log-dir", "logs/stripe" + stripeId + "/node-" + nodeId,
                "--node-backup-dir", "backup/stripe" + stripeId,
                "--node-metadata-dir", "metadata/stripe" + stripeId,
                "--node-repository-dir", "repository/stripe" + stripeId + "/node-" + nodeId,
                "--data-dirs", "main:user-data/main/stripe" + stripeId) :
            startNode(
                stripeId, nodeId,
                "--node-name", "node-" + nodeId,
                "--node-hostname", "localhost",
                "--node-port", String.valueOf(port),
                "--node-group-port", String.valueOf(groupPort),
                "--node-log-dir", "logs/stripe" + stripeId + "/node-" + nodeId,
                "--node-backup-dir", "backup/stripe" + stripeId,
                "--node-metadata-dir", "metadata/stripe" + stripeId,
                "--node-repository-dir", "repository/stripe" + stripeId + "/node-" + nodeId,
                "--data-dirs", "main:user-data/main/stripe" + stripeId);
  }

  protected final NodeProcess startNode(int stripeId, int nodeId, String... cli) {
    String key = stripeId + "-" + nodeId;
    NodeProcess oldProcess = nodeProcesses.remove(key);
    if (oldProcess != null) {
      oldProcess.close();
    }
    NodeProcess newProcess = NodeProcess.startNode(stripeId, nodeId, Kit.getOrCreatePath(), getBaseDir(), cli);
    nodeProcesses.put(key, newProcess);
    return newProcess;
  }

  protected final NodeProcess getNodeProcess(int stripeId, int nodeId) {
    String key = stripeId + "-" + nodeId;
    NodeProcess process = nodeProcesses.get(key);
    if (process == null) {
      throw new IllegalArgumentException("No process for node " + key);
    }
    return process;
  }

  protected final NodeProcess getNodeProcess() {
    return getNodeProcess(1, 1);
  }

  protected final OptionalInt findActive(int stripeId) {
    return IntStream.rangeClosed(1, nodesPerStripe)
        .filter(nodeId -> getNodeProcess(stripeId, nodeId).getServerState().isActive())
        .findFirst();
  }

  protected final int[] findPassives(int stripeId) {
    return IntStream.rangeClosed(1, nodesPerStripe)
        .filter(nodeId -> getNodeProcess(stripeId, nodeId).getServerState().isPassive())
        .toArray();
  }

  protected final int getNodePort(int stripeId, int nodeId) {
    return ports.getPorts()[2 * nodesPerStripe * (stripeId - 1) + nodesPerStripe * (nodeId - 1)];
  }

  protected final int getNodePort() {
    return getNodePort(1, 1);
  }

  protected final Path getBaseDir() {
    return tmpDir.getRoot();
  }

  protected final InetSocketAddress getNodeAddress() {
    return InetSocketAddress.createUnresolved("localhost", getNodePort());
  }

  protected final Path copyConfigProperty(String configFile) throws Exception {
    Path src = Paths.get(getClass().getResource(configFile).toURI());
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

  protected String licensePath() {
    try {
      final URL url = getClass().getResource("/license.xml");
      return url == null ? null : Paths.get(url.toURI()).toString();
    } catch (URISyntaxException e) {
      throw new IllegalStateException(e);
    }
  }

  protected final Path getNodeRepositoryDir() {
    return getNodeRepositoryDir(1, 1);
  }

  protected final Path getNodeRepositoryDir(int stripeId, int nodeId) {
    return getBaseDir().resolve("repository").resolve("stripe" + stripeId).resolve("node-" + nodeId);
  }

  protected final void waitUntil(Callable<String> callable, Matcher<? super String> matcher) {
    waitUntil(callable, matcher, timeout);
  }

  protected final void waitUntil(Callable<String> callable, Matcher<? super String> matcher, int timeout) {
    Awaitility.await()
        // do not use iterative because it slows down the whole test suite considerably, especially in case of a failing process causing a timeout
        .pollInterval(FIVE_HUNDRED_MILLISECONDS)
        .atMost(new Duration(timeout, TimeUnit.SECONDS))
        .until(callable, matcher);
  }

  protected final void assertCommandSuccessful() {
    assertCommandSuccessful(() -> {
    });
  }

  protected final void assertCommandSuccessful(Runnable verifications) {
    waitUntil(out::getLog, containsString("Command successful"));
    verifications.run();
    out.clearLog();
  }

  protected final Path generateNodeRepositoryDir(int stripeId, int nodeId, Consumer<ConfigRepositoryGenerator> fn) throws Exception {
    Path nodeRepositoryDir = getNodeRepositoryDir(stripeId, nodeId);
    Path repositoriesDir = getBaseDir().resolve("repositories");
    ConfigRepositoryGenerator clusterGenerator = new ConfigRepositoryGenerator(repositoriesDir, ports.getPorts());
    LOGGER.debug("Generating cluster node repositories into: {}", repositoriesDir);
    fn.accept(clusterGenerator);
    copyDirectory(repositoriesDir.resolve("stripe" + stripeId + "_node-" + nodeId), nodeRepositoryDir);
    LOGGER.debug("Created node repository into: {}", nodeRepositoryDir);
    return nodeRepositoryDir;
  }

  protected final Cluster getUpcomingCluster(String host, int port) throws Exception {
    try (DiagnosticService diagnosticService = DiagnosticServiceFactory.fetch(
        InetSocketAddress.createUnresolved(host, port),
        getClass().getSimpleName(),
        java.time.Duration.ofSeconds(5),
        java.time.Duration.ofSeconds(5),
        null)) {
      return diagnosticService.getProxy(TopologyService.class).getUpcomingNodeContext().getCluster();
    }
  }

  protected final Cluster getRuntimeCluster(String host, int port) throws Exception {
    try (DiagnosticService diagnosticService = DiagnosticServiceFactory.fetch(
        InetSocketAddress.createUnresolved(host, port),
        getClass().getSimpleName(),
        java.time.Duration.ofSeconds(5),
        java.time.Duration.ofSeconds(5),
        null)) {
      return diagnosticService.getProxy(TopologyService.class).getRuntimeNodeContext().getCluster();
    }
  }

  protected final void activateCluster() throws Exception {
    activateCluster("tc-cluster");
  }

  protected final void activateCluster(String name) throws Exception {
    activateCluster(name, () -> {
    });
  }

  protected final void activateCluster(Runnable verifications) throws Exception {
    activateCluster("tc-cluster", verifications);
  }

  protected final void activateCluster(String name, Runnable verifications) throws Exception {
    String licensePath = licensePath();
    if (licensePath == null) {
      ConfigTool.start("activate", "-s", "localhost:" + getNodePort(), "-n", name);
    } else {
      ConfigTool.start("activate", "-s", "localhost:" + getNodePort(), "-n", name, "-l", licensePath);
    }
    assertCommandSuccessful(verifications);
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

  private Stream<Tuple2<Integer, Integer>> nodeDefinitions() {
    return rangeClosed(1, stripes).mapToObj(stripeId -> rangeClosed(1, nodesPerStripe).mapToObj(nodeId -> tuple2(stripeId, nodeId))).flatMap(identity());
  }

  private Properties variables() {
    return nodeDefinitions().reduce(new Properties(), (props, tuple) -> {
      int stripeId = tuple.t1;
      int nodeId = tuple.t2;
      int nodePort = getNodePort(stripeId, nodeId);
      int groupPort = nodePort + 1;
      String configRepoPath = getNodeRepositoryDir(stripeId, nodeId).toString();
      props.setProperty(("PORT-" + stripeId + "-" + nodeId), String.valueOf(nodePort));
      props.setProperty(("GROUP-PORT-" + stripeId + "-" + nodeId), String.valueOf(groupPort));
      props.setProperty(("CONFIG-REPO-" + stripeId + "-" + nodeId), configRepoPath);
      return props;
    }, (p1, p2) -> {
      throw new UnsupportedOperationException();
    });
  }
}