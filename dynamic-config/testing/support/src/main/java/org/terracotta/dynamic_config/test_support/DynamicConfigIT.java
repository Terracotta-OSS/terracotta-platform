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
import org.junit.Rule;
import org.junit.rules.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.angela.client.config.ConfigurationContext;
import org.terracotta.angela.common.ConfigToolExecutionResult;
import org.terracotta.angela.common.distribution.Distribution;
import org.terracotta.angela.common.dynamic_cluster.Stripe;
import org.terracotta.angela.common.tcconfig.License;
import org.terracotta.angela.common.tcconfig.TerracottaServer;
import org.terracotta.angela.common.topology.Topology;
import org.terracotta.diagnostic.client.DiagnosticService;
import org.terracotta.diagnostic.client.DiagnosticServiceFactory;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.FailoverPriority;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.test_support.angela.AngelaRule;
import org.terracotta.dynamic_config.test_support.angela.NodeOutputRule;
import org.terracotta.dynamic_config.test_support.util.ConfigRepositoryGenerator;
import org.terracotta.dynamic_config.test_support.util.PropertyResolver;
import org.terracotta.port_locking.LockingPortChooser;
import org.terracotta.port_locking.LockingPortChoosers;
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
import java.util.OptionalInt;
import java.util.Properties;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Supplier;

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

  protected static boolean WIN = System.getProperty("os.name").toLowerCase().startsWith("windows");

  @Rule public TmpDir tmpDir = new TmpDir(Paths.get(System.getProperty("user.dir"), "target"), false);
  @Rule public AngelaRule angela;
  @Rule public Timeout timeoutRule;

  private final LockingPortChooser lockingPortChooser = LockingPortChoosers.getFileLockingPortChooser();

  private int stripes;
  private boolean autoStart;
  private int nodesPerStripe;
  private boolean autoActivate;

  protected long timeout;
  protected FailoverPriority failoverPriority = FailoverPriority.availability();

  public DynamicConfigIT() {
    this(Duration.ofSeconds(120));
  }

  public DynamicConfigIT(Duration testTimeout) {
    // cluster definition
    ClusterDefinition clusterDef = getClass().getAnnotation(ClusterDefinition.class);
    this.stripes = clusterDef.stripes();
    this.autoStart = clusterDef.autoStart();
    this.autoActivate = clusterDef.autoActivate();
    this.nodesPerStripe = clusterDef.nodesPerStripe();

    // timeout
    this.timeout = testTimeout.toMillis();

    // rules
    this.timeoutRule = Timeout.millis(testTimeout.toMillis());
    this.angela = new AngelaRule(lockingPortChooser, createConfigurationContext(stripes, nodesPerStripe), autoStart, autoActivate);
  }

  // =========================================
  // tmp dir
  // =========================================

  protected Path getBaseDir() {
    return tmpDir.getRoot();
  }

  // =========================================
  // angela calls
  // =========================================

  protected void startNode(int stripeId, int nodeId) {
    angela.startNode(stripeId, nodeId);
  }

  protected void startNode(int stripeId, int nodeId, String... cli) {
    angela.startNode(stripeId, nodeId, cli);
  }

  protected void startNode(TerracottaServer node, String... cli) {
    angela.tsa().start(node, cli);
  }

  protected void stopNode(int stripeId, int nodeId) {
    angela.tsa().stop(getNode(stripeId, nodeId));
  }

  protected TerracottaServer getNode(int stripeId, int nodeId) {
    return angela.getNode(stripeId, nodeId);
  }

  protected int getNodePort(int stripeId, int nodeId) {
    return angela.getNodePort(stripeId, nodeId);
  }

  protected int getNodeGroupPort(int stripeId, int nodeId) {
    return angela.getNodePort(stripeId, nodeId);
  }

  protected OptionalInt findActive(int stripeId) {
    return angela.findActive(stripeId);
  }

  protected int[] findPassives(int stripeId) {
    return angela.findPassives(stripeId);
  }

  protected int getNodePort() {
    return getNodePort(1, 1);
  }

  protected int getNodeGroupPort() {
    return getNodePort(1, 1) + 1;
  }

  protected InetSocketAddress getNodeAddress(int stripeId, int nodeId) {
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
    return angela.tsa().configTool(getNode(1, 1)).executeCommand(cli);
  }

  // =========================================
  // node and topology construction
  // =========================================

  protected ConfigurationContext createConfigurationContext(int stripes, int nodesPerStripe) {
    return customConfigurationContext().tsa(tsa -> tsa
        .clusterName("tc-cluster")
        .license(getLicenceUrl() == null ? null : new License(getLicenceUrl()))
        .topology(new Topology(
            getDistribution(),
            dynamicCluster(
                rangeClosed(1, stripes)
                    .mapToObj(stripeId -> stripe(rangeClosed(1, nodesPerStripe)
                        .mapToObj(nodeId -> createNode(stripeId, nodeId))
                        .toArray(TerracottaServer[]::new)))
                    .toArray(Stripe[]::new)))));
  }

  protected TerracottaServer createNode(int stripeId, int nodeId) {
    String symbolicName = "node-" + stripeId + "-" + nodeId;
    return server(symbolicName, "localhost")
        .configRepo(getNodePath(stripeId, nodeId).resolve("repository").toString())
        .logs(getNodePath(stripeId, nodeId).resolve("logs").toString())
        .dataDir("main:" + getNodePath(stripeId, nodeId).resolve("data-dir").toString())
        .offheap("main:512MB,foo:1GB")
        .metaData(getNodePath(stripeId, nodeId).resolve("metadata").toString())
        .failoverPriority(failoverPriority.toString());
  }

  protected Distribution getDistribution() {
    return distribution(version(DISTRIBUTION.getValue()), KIT, TERRACOTTA_OS);
  }

  protected String getNodeName(int stripeId, int nodeId) {
    return "node-" + stripeId + "-" + nodeId;
  }

  protected Path getNodePath(int stripeId, int nodeId) {
    return Paths.get(getNodeName(stripeId, nodeId));
  }

  protected Path getNodeRepositoryDir(int stripeId, int nodeId) {
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
      Properties variables = generateProperties();
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
    Path nodeRepositoryDir = getNodeRepositoryDir(stripeId, nodeId);
    Path repositoriesDir = getBaseDir().resolve("generated-repositories");
    ConfigRepositoryGenerator clusterGenerator = new ConfigRepositoryGenerator(repositoriesDir, new ConfigRepositoryGenerator.PortSupplier() {
      @Override
      public int getNodePort(int stripeId, int nodeId) {
        return 0;
      }

      @Override
      public int getNodeGroupPort(int stripeId, int nodeId) {
        return 0;
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

  private Properties generateProperties() {
    return rangeClosed(1, stripes).mapToObj(stripeId -> rangeClosed(1, nodesPerStripe).mapToObj(nodeId -> tuple2(stripeId, nodeId))).flatMap(identity()).reduce(new Properties(), (props, tuple) -> {
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

  // =========================================
  // assertions
  // =========================================

  protected void waitUntil(ConfigToolExecutionResult result, Matcher<? super ConfigToolExecutionResult> matcher) throws TimeoutException {
    waitUntil(() -> result, matcher, timeout);
  }

  protected void waitUntil(NodeOutputRule.NodeLog result, Matcher<? super NodeOutputRule.NodeLog> matcher) throws TimeoutException {
    waitUntil(() -> result, matcher, timeout);
  }

  protected <T> void waitUntil(Supplier<T> callable, Matcher<? super T> matcher) throws TimeoutException {
    waitUntil(callable, matcher, timeout);
  }

  protected <T> void waitUntil(Supplier<T> callable, Matcher<? super T> matcher, long timeout) throws TimeoutException {
    assertThatEventually(callable, matcher)
        .threadDumpOnTimeout()
        .within(Duration.ofSeconds(timeout));
  }

  protected void waitForActive(int stripeId) throws TimeoutException {
    waitUntil(() -> findActive(stripeId).isPresent(), is(true));
  }

  protected void waitForActive(int stripeId, int nodeId) throws TimeoutException {
    waitUntil(() -> angela.tsa().getState(getNode(stripeId, nodeId)), is(equalTo(STARTED_AS_ACTIVE)));
  }

  protected void waitForPassive(int stripeId, int nodeId) throws TimeoutException {
    waitUntil(() -> angela.tsa().getState(getNode(stripeId, nodeId)), is(equalTo(STARTED_AS_PASSIVE)));
  }

  protected void waitForDiagnostic(int stripeId, int nodeId) throws TimeoutException {
    waitUntil(() -> angela.tsa().getState(getNode(stripeId, nodeId)), is(equalTo(STARTED_IN_DIAGNOSTIC_MODE)));
  }

  protected void waitForPassives(int stripeId) throws TimeoutException {
    waitUntil(() -> findPassives(stripeId).length, is(equalTo(nodesPerStripe - 1)));
  }

  protected void waitForNPassives(int stripeId, int count) throws TimeoutException {
    waitUntil(() -> findPassives(stripeId).length, is(equalTo(count)));
  }

  protected Cluster getUpcomingCluster(int stripeId, int nodeId) throws Exception {
    return getUpcomingCluster("localhost", getNodePort(stripeId, nodeId));
  }

  // =========================================
  // information retrieval
  // =========================================

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
}