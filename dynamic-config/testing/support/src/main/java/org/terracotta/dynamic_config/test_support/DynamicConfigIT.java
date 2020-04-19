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

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.RuleChain;
import org.junit.rules.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.angela.client.ClusterFactory;
import org.terracotta.angela.client.Tsa;
import org.terracotta.angela.client.config.ConfigurationContext;
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
import org.terracotta.dynamic_config.test_support.angela.NodeOutputRule;
import org.terracotta.dynamic_config.test_support.util.ConfigRepositoryGenerator;
import org.terracotta.dynamic_config.test_support.util.PropertyResolver;
import org.terracotta.testing.PortLockingRule;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.nio.file.Files.walkFileTree;
import static java.util.function.Function.identity;
import static java.util.stream.IntStream.rangeClosed;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.terracotta.angela.client.config.custom.CustomConfigurationContext.customConfigurationContext;
import static org.terracotta.angela.common.AngelaProperties.DISTRIBUTION;
import static org.terracotta.angela.common.TerracottaServerState.STARTED_AS_ACTIVE;
import static org.terracotta.angela.common.TerracottaServerState.STARTED_AS_PASSIVE;
import static org.terracotta.angela.common.TerracottaServerState.STARTED_IN_DIAGNOSTIC_MODE;
import static org.terracotta.angela.common.distribution.Distribution.distribution;
import static org.terracotta.angela.common.dynamic_cluster.Stripe.stripe;
import static org.terracotta.angela.common.provider.DynamicConfigManager.dynamicCluster;
import static org.terracotta.angela.common.tcconfig.TerracottaServer.server;
import static org.terracotta.angela.common.topology.LicenseType.TERRACOTTA_OS;
import static org.terracotta.angela.common.topology.PackageType.KIT;
import static org.terracotta.angela.common.topology.Version.version;
import static org.terracotta.common.struct.Tuple2.tuple2;
import static org.terracotta.dynamic_config.test_support.angela.AngelaMatchers.successful;
import static org.terracotta.utilities.test.WaitForAssert.assertThatEventually;

public class DynamicConfigIT {
  private static final Logger LOGGER = LoggerFactory.getLogger(DynamicConfigIT.class);

  protected static final boolean WIN = System.getProperty("os.name").toLowerCase().startsWith("windows");

  @Rule public RuleChain rules;

  protected final TmpDir tmpDir;
  protected final PortLockingRule ports;
  protected final long timeout;

  private final Map<String, TerracottaServer> nodes = new ConcurrentHashMap<>();

  protected ClusterFactory clusterFactory;
  protected Tsa tsa;

  private int stripes;
  private boolean autoStart;
  private int nodesPerStripe;
  private boolean autoActivate;

  private ClusterDefinition clusterDef;

  public DynamicConfigIT() {
    this(Duration.ofSeconds(120));
  }

  public DynamicConfigIT(Duration testTimeout) {
    this(testTimeout, Paths.get(System.getProperty("user.dir"), "target"));
  }

  public DynamicConfigIT(Duration testTimeout, Path parentTmpDir) {
    this.timeout = testTimeout.toMillis();
    this.clusterDef = getClass().getAnnotation(ClusterDefinition.class);
    this.stripes = clusterDef.stripes();
    this.autoStart = clusterDef.autoStart();
    this.autoActivate = clusterDef.autoActivate();
    this.nodesPerStripe = clusterDef.nodesPerStripe();
    // this rule ensures that the timeout rule is surrounded by any other rules
    // so that if a test times out, the other rules can correctly close
    this.rules = RuleChain.emptyRuleChain()
        .around(tmpDir = new TmpDir(parentTmpDir, false))
        .around(ports = new PortLockingRule(2 * this.stripes * this.nodesPerStripe))
        .around(Timeout.millis(testTimeout.toMillis()));
  }

  @Before
  // keep throws Exception to support extend the class
  public void before() throws Exception {
    this.clusterFactory = new ClusterFactory(getClass().getSimpleName(), createConfigurationContext(clusterDef.stripes(), clusterDef.nodesPerStripe()));
    this.tsa = clusterFactory.tsa();
    if (autoStart) {
      startNodes();
      if (autoActivate) {
        tsa.attachAll();
        tsa.activateAll();
        for (int stripeId = 1; stripeId <= stripes; stripeId++) {
          waitForActive(stripeId);
          waitForPassives(stripeId);
        }
      }
    }
  }

  @After
  // keep throws Exception to support extend the class
  public void after() throws Exception {
    try {
      clusterFactory.close();
    } catch (IOException | RuntimeException e) {
      LOGGER.error("Close error: " + e.getMessage(), e);
      throw e;
    }
  }

  // =========================================
  // tmp dir
  // =========================================

  protected final Path getBaseDir() {
    return tmpDir.getRoot();
  }

  // =========================================
  // angela calls
  // =========================================

  protected void startNodes() {
    for (int stripeId = 1; stripeId <= stripes; stripeId++) {
      for (int nodeId = 1; nodeId <= nodesPerStripe; nodeId++) {
        startNode(stripeId, nodeId);
      }
    }
  }

  // can be overridden
  protected void startNode(int stripeId, int nodeId) {
    startNode(getNode(stripeId, nodeId));
  }

  protected final void startNode(int stripeId, int nodeId, String... cli) {
    startNode(getNode(stripeId, nodeId), cli);
  }

  protected final void startNode(TerracottaServer node, String... cli) {
    tsa.start(node, cli);
  }

  protected final void stopNode(int stripeId, int nodeId) {
    tsa.stop(getNode(stripeId, nodeId));
  }

  protected final TerracottaServer getNode(int stripeId, int nodeId) {
    String key = combine(stripeId, nodeId);
    TerracottaServer server = nodes.get(key);
    if (server == null) {
      throw new IllegalArgumentException("No server for node " + key);
    }
    return server;
  }

  protected final int getNodePort(int stripeId, int nodeId) {
    if (nodeId > nodesPerStripe) {
      throw new IllegalArgumentException("Invalid node ID: " + nodeId + ". Stripes have maximum of " + nodesPerStripe + " nodes.");
    }
    if (nodeId < 1) {
      throw new IllegalArgumentException("Invalid node ID: " + nodeId);
    }
    if (stripeId > stripes) {
      throw new IllegalArgumentException("Invalid stripe ID: " + stripeId + ". There are " + stripes + " stripe(s).");
    }
    if (stripeId < 1) {
      throw new IllegalArgumentException("Invalid stripe ID: " + nodeId);
    }
    //1-1 => 0 and 1
    //1-2 => 2 and 3
    //1-3 => 4 and 5
    //2-1 => 6 and 7
    //2-2 => 8 and 9
    //2-3 => 10 and 11
    return ports.getPorts()[2 * (nodeId - 1) + 2 * nodesPerStripe * (stripeId - 1)];
  }

  protected final int getNodeGroupPort(int stripeId, int nodeId) {
    return getNodePort(stripeId, nodeId) + 1;
  }

  protected final OptionalInt findActive(int stripeId) {
    return IntStream.rangeClosed(1, nodesPerStripe)
        .filter(nodeId -> tsa.getState(getNode(stripeId, nodeId)) == STARTED_AS_ACTIVE)
        .findFirst();
  }

  protected final int[] findPassives(int stripeId) {
    return IntStream.rangeClosed(1, nodesPerStripe)
        .filter(nodeId -> tsa.getState(getNode(stripeId, nodeId)) == STARTED_AS_PASSIVE)
        .toArray();
  }

  protected final int getNodePort() {
    return getNodePort(1, 1);
  }

  protected final InetSocketAddress getNodeAddress(int stripeId, int nodeId) {
    return InetSocketAddress.createUnresolved("localhost", getNodePort(stripeId, nodeId));
  }

  protected ConfigToolExecutionResult activateCluster() throws TimeoutException {
    return activateCluster("tc-cluster");
  }

  protected ConfigToolExecutionResult activateCluster(String name) throws TimeoutException {
    Path licensePath = getLicensePath();
    ConfigToolExecutionResult result = licensePath == null ?
        configToolInvocation("activate", "-s", "localhost:" + getNodePort(), "-n", name) :
        configToolInvocation("activate", "-s", "localhost:" + getNodePort(), "-n", name, "-l", licensePath.toString());
    assertThat(result, is(successful()));
    waitForActive(1);
    return result;
  }

  protected ConfigToolExecutionResult configToolInvocation(String... cli) {
    return tsa.configTool(getNode(1, 1)).executeCommand(cli);
  }

  // =========================================
  // node and topology construction
  // =========================================

  protected ConfigurationContext createConfigurationContext(int stripeCount, int nodesPerStripe) {
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
    return customConfigurationContext().tsa(tsa -> tsa
        .clusterName("tc-cluster")
        .license(getLicenceUrl() == null ? null : new License(getLicenceUrl()))
        .topology(new Topology(
            getDistribution(),
            dynamicCluster(stripes))));
  }

  protected TerracottaServer createNode(int stripeId, int nodeId) {
    String uniqueId = combine(stripeId, nodeId);
    return server("node-" + uniqueId, "localhost")
        .tsaPort(getNodePort(stripeId, nodeId))
        .tsaGroupPort(getNodeGroupPort(stripeId, nodeId))
        .configRepo(getNodePath(stripeId, nodeId).resolve("repository").toString())
        .logs(getNodePath(stripeId, nodeId).resolve("logs").toString())
        .dataDir("main:" + getNodePath(stripeId, nodeId).resolve("data-dir").toString())
        .offheap("main:512MB,foo:1GB")
        .metaData(getNodePath(stripeId, nodeId).resolve("metadata").toString())
        .failoverPriority(getFailoverPriority().toString());
  }

  protected FailoverPriority getFailoverPriority() {
    return FailoverPriority.availability();
  }

  protected Distribution getDistribution() {
    return distribution(version(DISTRIBUTION.getValue()), KIT, TERRACOTTA_OS);
  }

  protected final String getNodeName(int stripeId, int nodeId) {
    return "node-" + stripeId + "-" + nodeId;
  }

  protected final Path getNodePath(int stripeId, int nodeId) {
    return Paths.get(getNodeName(stripeId, nodeId));
  }

  protected final Path getNodeRepositoryDir(int stripeId, int nodeId) {
    return getNodePath(stripeId, nodeId).resolve("repository");
  }

  protected Path getLicensePath() {
    try {
      return getLicenceUrl() == null ? null : Paths.get(getLicenceUrl().toURI());
    } catch (URISyntaxException e) {
      throw new IllegalStateException(e);
    }
  }

  private URL getLicenceUrl() {
    return getClass().getResource("/license.xml");
  }

  // =========================================
  // config repo generation
  // =========================================

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
      if (WIN) {
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

  protected Path generateNodeRepositoryDir(int stripeId, int nodeId, Consumer<ConfigRepositoryGenerator> fn) throws Exception {
    Path nodeRepositoryDir = getBaseDir().resolve(getNodeRepositoryDir(stripeId, nodeId));
    Path repositoriesDir = getBaseDir().resolve("generated-repositories");
    ConfigRepositoryGenerator clusterGenerator = new ConfigRepositoryGenerator(repositoriesDir, new ConfigRepositoryGenerator.PortSupplier() {
      @Override
      public int getNodePort(int stripeId, int nodeId) {
        return DynamicConfigIT.this.getNodePort(stripeId, nodeId);
      }

      @Override
      public int getNodeGroupPort(int stripeId, int nodeId) {
        return DynamicConfigIT.this.getNodeGroupPort(stripeId, nodeId);
      }
    });
    LOGGER.debug("Generating cluster node repositories into: {}", repositoriesDir);
    fn.accept(clusterGenerator);
    copyDirectory(repositoriesDir.resolve("stripe-" + stripeId).resolve("node-" + nodeId), nodeRepositoryDir);
    LOGGER.debug("Created node repository into: {}", nodeRepositoryDir);
    return nodeRepositoryDir;
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

  private Properties variables() {
    return nodeDefinitions().reduce(new Properties(), (props, tuple) -> {
      int stripeId = tuple.t1;
      int nodeId = tuple.t2;
      int nodePort = getNodePort(stripeId, nodeId);
      int groupPort = nodePort + 1;
      props.setProperty(("PORT-" + stripeId + "-" + nodeId), String.valueOf(nodePort));
      props.setProperty(("GROUP-PORT-" + stripeId + "-" + nodeId), String.valueOf(groupPort));
      return props;
    }, (p1, p2) -> {
      throw new UnsupportedOperationException();
    });
  }

  // =========================================
  // assertions
  // =========================================

  protected final void waitUntil(ConfigToolExecutionResult result, Matcher<? super ConfigToolExecutionResult> matcher) throws TimeoutException {
    waitUntil(() -> result, matcher, timeout);
  }

  protected final void waitUntil(NodeOutputRule.NodeLog result, Matcher<? super NodeOutputRule.NodeLog> matcher) throws TimeoutException {
    waitUntil(() -> result, matcher, timeout);
  }

  protected final <T> void waitUntil(Supplier<T> callable, Matcher<? super T> matcher) throws TimeoutException {
    waitUntil(callable, matcher, timeout);
  }

  protected final <T> void waitUntil(Supplier<T> callable, Matcher<? super T> matcher, long timeout) throws TimeoutException {
    assertThatEventually(callable, matcher)
        .threadDumpOnTimeout()
        .within(Duration.ofSeconds(timeout));
  }

  protected final void waitForActive(int stripeId) throws TimeoutException {
    waitUntil(() -> findActive(stripeId).isPresent(), is(true));
  }

  protected final void waitForActive(int stripeId, int nodeId) throws TimeoutException {
    waitUntil(() -> tsa.getState(getNode(stripeId, nodeId)), is(equalTo(STARTED_AS_ACTIVE)));
  }

  protected final void waitForPassive(int stripeId, int nodeId) throws TimeoutException {
    waitUntil(() -> tsa.getState(getNode(stripeId, nodeId)), is(equalTo(STARTED_AS_PASSIVE)));
  }

  protected void waitForDiagnostic(int stripeId, int nodeId) throws TimeoutException {
    waitUntil(() -> tsa.getState(getNode(stripeId, nodeId)), is(equalTo(STARTED_IN_DIAGNOSTIC_MODE)));
  }

  protected void waitForPassives(int stripeId) throws TimeoutException {
    waitUntil(() -> findPassives(stripeId).length, is(equalTo(nodesPerStripe - 1)));
  }

  protected void waitForNPassives(int stripeId, int count) throws TimeoutException {
    waitUntil(() -> findPassives(stripeId).length, is(equalTo(count)));
  }

  protected final Cluster getUpcomingCluster(int stripeId, int nodeId) throws Exception {
    return getUpcomingCluster("localhost", getNodePort(stripeId, nodeId));
  }

  // =========================================
  // information retrieval
  // =========================================

  protected final Cluster getUpcomingCluster(String host, int port) throws Exception {
    try (DiagnosticService diagnosticService = DiagnosticServiceFactory.fetch(
        InetSocketAddress.createUnresolved(host, port),
        getClass().getSimpleName(),
        Duration.ofSeconds(30),
        Duration.ofSeconds(30),
        null)) {
      return diagnosticService.getProxy(TopologyService.class).getUpcomingNodeContext().getCluster();
    }
  }

  protected final Cluster getRuntimeCluster(int stripeId, int nodeId) throws Exception {
    return getUpcomingCluster("localhost", getNodePort(stripeId, nodeId));
  }

  protected final Cluster getRuntimeCluster(String host, int port) throws Exception {
    try (DiagnosticService diagnosticService = DiagnosticServiceFactory.fetch(
        InetSocketAddress.createUnresolved(host, port),
        getClass().getSimpleName(),
        Duration.ofSeconds(30),
        Duration.ofSeconds(30),
        null)) {
      return diagnosticService.getProxy(TopologyService.class).getRuntimeNodeContext().getCluster();
    }
  }

  protected final void withTopologyService(int stripeId, int nodeId, Consumer<TopologyService> consumer) throws Exception {
    withTopologyService("localhost", getNodePort(stripeId, nodeId), consumer);
  }

  protected final void withTopologyService(String host, int port, Consumer<TopologyService> consumer) throws Exception {
    try (DiagnosticService diagnosticService = DiagnosticServiceFactory.fetch(
        InetSocketAddress.createUnresolved(host, port),
        getClass().getSimpleName(),
        Duration.ofSeconds(30),
        Duration.ofSeconds(30),
        null)) {
      consumer.accept(diagnosticService.getProxy(TopologyService.class));
    }
  }

  private Stream<Tuple2<Integer, Integer>> nodeDefinitions() {
    return rangeClosed(1, stripes).mapToObj(stripeId -> rangeClosed(1, nodesPerStripe).mapToObj(nodeId -> tuple2(stripeId, nodeId))).flatMap(identity());
  }

  protected String combine(int stripeId, int nodesId) {
    return stripeId + "-" + nodesId;
  }
}