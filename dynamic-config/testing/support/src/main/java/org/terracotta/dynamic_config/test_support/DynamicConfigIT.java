/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.dynamic_config.test_support;

import org.awaitility.Awaitility;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.angela.client.ClusterFactory;
import org.terracotta.angela.client.Tsa;
import org.terracotta.angela.client.config.custom.CustomConfigurationContext;
import org.terracotta.angela.client.config.custom.CustomTsaConfigurationContext;
import org.terracotta.angela.common.ConfigToolExecutionResult;
import org.terracotta.angela.common.distribution.Distribution;
import org.terracotta.angela.common.dynamic_cluster.Stripe;
import org.terracotta.angela.common.tcconfig.License;
import org.terracotta.angela.common.tcconfig.TerracottaServer;
import org.terracotta.angela.common.topology.Topology;
import org.terracotta.common.struct.Tuple2;
import org.terracotta.diagnostic.client.DiagnosticService;
import org.terracotta.diagnostic.client.DiagnosticServiceFactory;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.FailoverPriority;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.test_support.util.ConfigRepositoryGenerator;
import org.terracotta.dynamic_config.test_support.util.NodeOutputRule;
import org.terracotta.dynamic_config.test_support.util.PropertyResolver;
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
import java.time.Duration;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.nio.file.Files.walkFileTree;
import static java.util.function.Function.identity;
import static java.util.stream.IntStream.rangeClosed;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.terracotta.angela.client.config.custom.CustomConfigurationContext.customConfigurationContext;
import static org.terracotta.angela.common.AngelaProperties.DISTRIBUTION;
import static org.terracotta.angela.common.TerracottaServerState.STARTED_AS_ACTIVE;
import static org.terracotta.angela.common.TerracottaServerState.STARTED_AS_PASSIVE;
import static org.terracotta.angela.common.distribution.Distribution.distribution;
import static org.terracotta.angela.common.dynamic_cluster.Stripe.stripe;
import static org.terracotta.angela.common.provider.DynamicConfigManager.dynamicCluster;
import static org.terracotta.angela.common.tcconfig.TerracottaServer.server;
import static org.terracotta.angela.common.topology.LicenseType.TERRACOTTA_OS;
import static org.terracotta.angela.common.topology.PackageType.KIT;
import static org.terracotta.angela.common.topology.Version.version;
import static org.terracotta.common.struct.Tuple2.tuple2;
import static org.terracotta.dynamic_config.test_support.util.AngelaMatchers.successful;

public class DynamicConfigIT {
  private static Logger LOGGER = LoggerFactory.getLogger(DynamicConfigIT.class);

  @Rule public TmpDir tmpDir = new TmpDir();
  @Rule public PortLockingRule ports;
  @Rule public Timeout timeoutRule;

  protected long timeout;

  protected ClusterFactory clusterFactory;
  protected Tsa tsa;

  private int stripes;
  private boolean autoStart;
  private int nodesPerStripe;
  private boolean autoActivate;
  private Map<String, TerracottaServer> nodes = new ConcurrentHashMap<>();
  private ClusterDefinition clusterDef;
  protected FailoverPriority failoverPriority = FailoverPriority.availability();

  public DynamicConfigIT() {
    this(Duration.ofSeconds(60));
  }

  public DynamicConfigIT(Duration testTimeout) {
    this.timeout = testTimeout.toMillis();
    this.timeoutRule = Timeout.millis(testTimeout.toMillis());
    this.clusterDef = getClass().getAnnotation(ClusterDefinition.class);
    this.stripes = clusterDef.stripes();
    this.autoStart = clusterDef.autoStart();
    this.autoActivate = clusterDef.autoActivate();
    this.nodesPerStripe = clusterDef.nodesPerStripe();
    this.ports = new PortLockingRule(2 * this.stripes * this.nodesPerStripe);
  }

  @Before
  public void before() {
    this.clusterFactory = new ClusterFactory(getClass().getSimpleName(), createConfigContext(clusterDef.stripes(), clusterDef.nodesPerStripe()));
    this.tsa = clusterFactory.tsa();
    if (autoStart) {
      startNodes();
      if (autoActivate) {
        tsa.attachAll();
        tsa.activateAll();
      }
    }
  }

  @After
  public void after() throws IOException {
    try {
      clusterFactory.close();
    } catch (IOException | RuntimeException e) {
      LOGGER.error("Close error: " + e.getMessage(), e);
      throw e;
    }
  }

  protected void startNodes() {
    for (int stripeId = 1; stripeId <= stripes; stripeId++) {
      for (int nodeId = 1; nodeId <= nodesPerStripe; nodeId++) {
        startNode(stripeId, nodeId);
      }
    }
  }

  protected void startNode(int stripeId, int nodeId) {
    startNode(getNode(stripeId, nodeId));
  }

  protected void startNode(int stripeId, int nodeId, String... cli) {
    startNode(getNode(stripeId, nodeId), cli);
  }

  protected void startNode(TerracottaServer node, String... cli) {
    tsa.start(node, cli);
  }

  protected void stopNode(int stripeId, int nodeId) {
    tsa.stop(getNode(stripeId, nodeId));
  }

  protected TerracottaServer getNode(int stripeId, int nodeId) {
    String key = combine(stripeId, nodeId);
    TerracottaServer server = nodes.get(key);
    if (server == null) {
      throw new IllegalArgumentException("No server for node " + key);
    }
    return server;
  }

  protected int getNodePort(int stripeId, int nodeId) {
    //1-1 => 0 and 1
    //1-2 => 2 and 3
    //1-3 => 4 and 5
    //2-1 => 6 and 7
    //2-2 => 8 and 9
    //2-3 => 10 and 11
    return ports.getPorts()[2 * (nodeId - 1) + 2 * nodesPerStripe * (stripeId - 1)];
  }

  protected int getNodeGroupPort(int stripeId, int nodeId) {
    return getNodePort(stripeId, nodeId) + 1;
  }

  protected OptionalInt findActive(int stripeId) {
    return IntStream.rangeClosed(stripeId, nodesPerStripe)
        .filter(nodeId -> tsa.getState(getNode(stripeId, nodeId)) == STARTED_AS_ACTIVE)
        .findFirst();
  }

  protected int[] findPassives(int stripeId) {
    return IntStream.rangeClosed(stripeId, nodesPerStripe)
        .filter(nodeId -> tsa.getState(getNode(stripeId, nodeId)) == STARTED_AS_PASSIVE)
        .toArray();
  }

  protected int getNodePort() {
    return getNodePort(1, 1);
  }

  protected int getNodeGroupPort() {
    return getNodePort(1, 1) + 1;
  }

  protected Path getBaseDir() {
    return tmpDir.getRoot();
  }

  protected InetSocketAddress getNodeAddress() {
    return InetSocketAddress.createUnresolved("localhost", getNodePort());
  }

  protected Path copyConfigProperty(String configFile) {
    Path src;
    try {
      src = Paths.get(getClass().getResource(configFile).toURI());
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }

    Path dest = getBaseDir().resolve(src.getFileName());
    Properties loaded = new Properties();
    try {
      try (Reader reader = new InputStreamReader(Files.newInputStream(src), StandardCharsets.UTF_8)) {
        loaded.load(reader);
      }
      Properties variables = variables();
      Properties resolved = new PropertyResolver(variables).resolveAll(loaded);
      if (isWindows()) {
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
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return dest;
  }

  protected String licensePath() {
    try {
      return licenseUrl() == null ? null : Paths.get(licenseUrl().toURI()).toString();
    } catch (URISyntaxException e) {
      throw new IllegalStateException(e);
    }
  }

  private URL licenseUrl() {
    return getClass().getResource("/license.xml");
  }

  protected Path getNodeRepositoryDir() {
    return getNodeRepositoryDir(1, 1);
  }

  protected Path getNodeRepositoryDir(int stripeId, int nodeId) {
    return getBaseDir().resolve("repository").resolve("stripe" + stripeId).resolve("node-" + nodeId);
  }

  protected void waitUntil(ConfigToolExecutionResult result, Matcher<? super ConfigToolExecutionResult> matcher) {
    waitUntil(() -> result, matcher, timeout);
  }

  protected void waitUntil(NodeOutputRule.NodeLog result, Matcher<? super NodeOutputRule.NodeLog> matcher) {
    waitUntil(() -> result, matcher, timeout);
  }

  protected <T> void waitUntil(Callable<T> callable, Matcher<? super T> matcher) {
    waitUntil(callable, matcher, timeout);
  }

  protected <T> void waitUntil(Callable<T> callable, Matcher<? super T> matcher, long timeout) {
    Awaitility.await()
        // do not use iterative because it slows down the whole test suite considerably, especially in case of a failing process causing a timeout
        .pollInterval(Duration.ofMillis(500))
        .atMost(timeout, TimeUnit.SECONDS)
        .until(callable, matcher);
  }

  protected void waitForActive(int stripeId) {
    waitUntil(() -> findActive(stripeId).isPresent(), is(true));
  }

  protected void waitForSomePassives(int stripeId) {
    waitUntil(() -> findPassives(stripeId).length, is(greaterThan(0)));
  }

  protected Path generateNodeRepositoryDir(int stripeId, int nodeId, Consumer<ConfigRepositoryGenerator> fn) throws Exception {
    Path nodeRepositoryDir = getNodeRepositoryDir(stripeId, nodeId);
    Path repositoriesDir = getBaseDir().resolve("repositories");
    ConfigRepositoryGenerator clusterGenerator = new ConfigRepositoryGenerator(repositoriesDir, ports.getPorts());
    LOGGER.debug("Generating cluster node repositories into: {}", repositoriesDir);
    fn.accept(clusterGenerator);
    copyDirectory(repositoriesDir.resolve("stripe-" + stripeId).resolve("node-" + nodeId), nodeRepositoryDir);
    LOGGER.debug("Created node repository into: {}", nodeRepositoryDir);
    return nodeRepositoryDir;
  }

  protected Cluster getUpcomingCluster(int stripeId, int nodeId) throws Exception {
    return getUpcomingCluster("localhost", getNodePort(stripeId, nodeId));
  }

  protected Cluster getUpcomingCluster(String host, int port) throws Exception {
    try (DiagnosticService diagnosticService = DiagnosticServiceFactory.fetch(
        InetSocketAddress.createUnresolved(host, port),
        getClass().getSimpleName(),
        Duration.ofSeconds(30),
        Duration.ofSeconds(30),
        null)) {
      return diagnosticService.getProxy(TopologyService.class).getUpcomingNodeContext().getCluster();
    }
  }

  protected Cluster getRuntimeCluster(int stripeId, int nodeId) throws Exception {
    return getUpcomingCluster("localhost", getNodePort(stripeId, nodeId));
  }

  protected Cluster getRuntimeCluster(String host, int port) throws Exception {
    try (DiagnosticService diagnosticService = DiagnosticServiceFactory.fetch(
        InetSocketAddress.createUnresolved(host, port),
        getClass().getSimpleName(),
        Duration.ofSeconds(30),
        Duration.ofSeconds(30),
        null)) {
      return diagnosticService.getProxy(TopologyService.class).getRuntimeNodeContext().getCluster();
    }
  }

  protected void withTopologyService(int stripeId, int nodeId, Consumer<TopologyService> consumer) throws Exception {
    withTopologyService("localhost", getNodePort(stripeId, nodeId), consumer);
  }

  protected void withTopologyService(String host, int port, Consumer<TopologyService> consumer) throws Exception {
    try (DiagnosticService diagnosticService = DiagnosticServiceFactory.fetch(
        InetSocketAddress.createUnresolved(host, port),
        getClass().getSimpleName(),
        Duration.ofSeconds(30),
        Duration.ofSeconds(30),
        null)) {
      consumer.accept(diagnosticService.getProxy(TopologyService.class));
    }
  }

  protected ConfigToolExecutionResult activateCluster() {
    return activateCluster("tc-cluster");
  }

  protected ConfigToolExecutionResult activateCluster(String name) {
    String licensePath = licensePath();
    ConfigToolExecutionResult result;
    if (licensePath == null) {
      result = configToolInvocation("activate", "-s", "localhost:" + getNodePort(), "-n", name);
    } else {
      result = configToolInvocation("activate", "-s", "localhost:" + getNodePort(), "-n", name, "-l", licensePath);
    }
    assertThat(result, is(successful()));
    return result;
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

  protected TerracottaServer createNode(int stripeId, int nodesId) {
    String uniqueId = combine(stripeId, nodesId);
    return server("node-" + uniqueId, "localhost")
        .tsaPort(getNodePort(stripeId, nodesId))
        .tsaGroupPort(getNodeGroupPort(stripeId, nodesId))
        .configRepo("terracotta" + uniqueId + "/repository")
        .logs("terracotta" + uniqueId + "/logs")
        .dataDir("main:terracotta" + uniqueId + "/data-dir")
        .offheap("main:512MB,foo:1GB")
        .metaData("terracotta" + uniqueId + "/metadata")
        .failoverPriority(failoverPriority.toString());
  }

  protected String combine(int stripeId, int nodesId) {
    return stripeId + "-" + nodesId;
  }

  protected CustomConfigurationContext createConfigContext(int stripeCount, int nodesPerStripe) {
    Stripe[] stripes = new Stripe[stripeCount];
    for (int stripeIndex = 0; stripeIndex < stripeCount; stripeIndex++) {
      TerracottaServer[] servers = new TerracottaServer[nodesPerStripe];
      for (int serverIndex = 0; serverIndex < nodesPerStripe; serverIndex++) {
        String key = combine(stripeIndex + 1, serverIndex + 1);
        TerracottaServer node = createNode(stripeIndex + 1, serverIndex + 1);
        nodes.put(key, node);
        servers[serverIndex] = node;
      }
      stripes[stripeIndex] = stripe(servers);
    }

    return customConfigurationContext()
        .tsa(tsa -> {
          CustomTsaConfigurationContext topology = tsa
              .clusterName("tc-cluster")
              .topology(new Topology(getDistribution(), dynamicCluster(stripes)));
          if (licenseUrl() != null) {
            topology.license(new License(licenseUrl()));
          }
        });
  }

  protected Distribution getDistribution() {
    return distribution(version(DISTRIBUTION.getValue()), KIT, TERRACOTTA_OS);
  }

  protected ConfigToolExecutionResult configToolInvocation(String... cli) {
    return tsa.configTool(getNode(1, 1)).executeCommand(cli);
  }

  private static boolean isWindows() {
    return System.getProperty("os.name").toLowerCase().startsWith("windows");
  }
}