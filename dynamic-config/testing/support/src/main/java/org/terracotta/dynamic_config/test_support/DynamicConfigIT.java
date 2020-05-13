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
import org.junit.rules.RuleChain;
import org.junit.rules.Timeout;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.angela.client.config.ConfigurationContext;
import org.terracotta.angela.client.config.TsaConfigurationContext;
import org.terracotta.angela.client.filesystem.RemoteFolder;
import org.terracotta.angela.client.support.junit.AngelaRule;
import org.terracotta.angela.client.support.junit.NodeOutputRule;
import org.terracotta.angela.common.ConfigToolExecutionResult;
import org.terracotta.angela.common.TerracottaCommandLineEnvironment;
import org.terracotta.angela.common.distribution.Distribution;
import org.terracotta.angela.common.dynamic_cluster.Stripe;
import org.terracotta.angela.common.tcconfig.License;
import org.terracotta.angela.common.tcconfig.TerracottaServer;
import org.terracotta.angela.common.topology.Topology;
import org.terracotta.common.struct.Measure;
import org.terracotta.common.struct.TimeUnit;
import org.terracotta.diagnostic.client.DiagnosticService;
import org.terracotta.diagnostic.client.DiagnosticServiceFactory;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.FailoverPriority;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.test_support.util.ConfigurationGenerator;
import org.terracotta.dynamic_config.test_support.util.PropertyResolver;
import org.terracotta.testing.ExtendedTestRule;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;
import java.util.Properties;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.stream.IntStream.rangeClosed;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.terracotta.angela.client.config.custom.CustomConfigurationContext.customConfigurationContext;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;
import static org.terracotta.angela.common.AngelaProperties.DISTRIBUTION;
import static org.terracotta.angela.common.TerracottaServerState.STARTED_AS_ACTIVE;
import static org.terracotta.angela.common.TerracottaServerState.STARTED_AS_PASSIVE;
import static org.terracotta.angela.common.TerracottaServerState.STARTED_IN_DIAGNOSTIC_MODE;
import static org.terracotta.angela.common.TerracottaServerState.STOPPED;
import static org.terracotta.angela.common.distribution.Distribution.distribution;
import static org.terracotta.angela.common.dynamic_cluster.Stripe.stripe;
import static org.terracotta.angela.common.provider.DynamicConfigManager.dynamicCluster;
import static org.terracotta.angela.common.tcconfig.TerracottaServer.server;
import static org.terracotta.angela.common.topology.LicenseType.TERRACOTTA_OS;
import static org.terracotta.angela.common.topology.PackageType.KIT;
import static org.terracotta.angela.common.topology.Version.version;
import static org.terracotta.utilities.io.Files.ExtendedOption.RECURSIVE;
import static org.terracotta.utilities.test.matchers.Eventually.within;

public class DynamicConfigIT {
  protected static final boolean WIN = System.getProperty("os.name").toLowerCase().startsWith("windows");

  private static final Logger LOGGER = LoggerFactory.getLogger(DynamicConfigIT.class);
  private static final Duration DEFAULT_TEST_TIMEOUT = Duration.ofMinutes(2);
  private static final Duration CONN_TIMEOUT = Duration.ofSeconds(30);
  private static final Duration ASSERT_TIMEOUT = Duration.ofMinutes(1);

  protected final TmpDir tmpDir;
  protected final AngelaRule angela;
  protected final long timeout;

  @Rule public RuleChain rules;

  public DynamicConfigIT() {
    this(DEFAULT_TEST_TIMEOUT);
  }

  public DynamicConfigIT(Duration testTimeout) {
    this(testTimeout, Paths.get(System.getProperty("user.dir"), "target", "test-data"));
  }

  public DynamicConfigIT(Duration testTimeout, Path parentTmpDir) {
    ClusterDefinition clusterDef = getClass().getAnnotation(ClusterDefinition.class);
    this.timeout = testTimeout.toMillis();
    this.rules = RuleChain.emptyRuleChain()
        .around(tmpDir = new TmpDir(parentTmpDir, false))
        .around(angela = new AngelaRule(createConfigurationContext(clusterDef.stripes(), clusterDef.nodesPerStripe()), clusterDef.autoStart(), clusterDef.autoActivate()) {
          @Override
          public void startNode(int stripeId, int nodeId) {
            // let the subclasses control the node startup
            DynamicConfigIT.this.startNode(stripeId, nodeId);
          }
        })
        .around(Timeout.millis(testTimeout.toMillis()))
        .around(new ExtendedTestRule() {
          @Override
          protected void before(Description description) throws Throwable {
            // upload tc logging config, but ONLY IF EXISTS !
            URL tcLoggingConfig = this.getClass().getResource("/tc-logback.xml");
            if (tcLoggingConfig != null) {
              List<TerracottaServer> servers = angela.tsa().getTsaConfigurationContext().getTopology().getServers();
              for (TerracottaServer s : servers) {
                try {
                  RemoteFolder folder = angela.tsa().browse(s, "");
                  folder.upload("logback-test.xml", tcLoggingConfig);
                } catch (IOException exp) {
                  LOGGER.warn("unable to upload logback configuration", exp);
                }
              }
            }
            // wait for server startup if auto-activated
            if (clusterDef.autoStart() && clusterDef.autoActivate()) {
              for (int stripeId = 1; stripeId <= clusterDef.stripes(); stripeId++) {
                waitForActive(stripeId);
                waitForPassives(stripeId);
              }
            }
          }
        });
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

  // can be overridden
  protected void startNode(int stripeId, int nodeId) {
    angela.startNode(angela.getNode(stripeId, nodeId));
  }

  protected final void startNode(int stripeId, int nodeId, String... cli) {
    angela.startNode(stripeId, nodeId, cli);
  }

  protected final void startNode(TerracottaServer node, String... cli) {
    angela.tsa().start(node, cli);
  }

  protected final void stopNode(int stripeId, int nodeId) {
    angela.tsa().stop(getNode(stripeId, nodeId));
  }

  protected final TerracottaServer getNode(int stripeId, int nodeId) {
    return angela.getNode(stripeId, nodeId);
  }

  protected final int getNodePort(int stripeId, int nodeId) {
    return angela.getNodePort(stripeId, nodeId);
  }

  protected final int getNodeGroupPort(int stripeId, int nodeId) {
    return angela.getNodeGroupPort(stripeId, nodeId);
  }

  protected final OptionalInt findActive(int stripeId) {
    return angela.findActive(stripeId);
  }

  protected final int[] findPassives(int stripeId) {
    return angela.findPassives(stripeId);
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
    List<String> enhancedCli = new ArrayList<>(cli.length);
    List<String> configToolOptions = getConfigToolOptions(cli);

    boolean addedOptions = false;
    String timeout = Measure.of(getConnectionTimeout().getSeconds(), TimeUnit.SECONDS).toString();
    if (!configToolOptions.contains("-t")) {
      // Add the option if it wasn't already passed in the `cli` parameter as a config tool option
      enhancedCli.add("-t");
      enhancedCli.add(timeout);
      addedOptions = true;
    }

    if (!configToolOptions.contains("-r")) {
      // Add the option if it wasn't already passed in the `cli` parameter as a config tool option
      enhancedCli.add("-r");
      enhancedCli.add(timeout);
      addedOptions = true;
    }

    String[] cmd;
    if (addedOptions) {
      enhancedCli.addAll(Arrays.asList(cli));
      cmd = enhancedCli.toArray(new String[0]);
    } else {
      cmd = cli;
    }
    return angela.tsa().configTool(getNode(1, 1)).executeCommand(cmd);
  }

  private List<String> getConfigToolOptions(String[] cli) {
    List<String> configToolOptions = new ArrayList<>(cli.length);
    for (int i = 0; i < cli.length; i++) {
      String opt = cli[i];
      if (opt.startsWith("-")) {
        configToolOptions.add(opt);
        i++;
      } else {
        break;
      }
    }
    return configToolOptions;
  }

  // =========================================
  // node and topology construction
  // =========================================

  protected ConfigurationContext createConfigurationContext(int stripes, int nodesPerStripe) {
    return customConfigurationContext().tsa(tsa -> tsa
        .clusterName("tc-cluster")
        .license(getLicenceUrl() == null ? null : new License(getLicenceUrl()))
        .terracottaCommandLineEnvironment(TerracottaCommandLineEnvironment.DEFAULT.withJavaOpts("-Xms32m -Xmx256m"))
        .terracottaCommandLineEnvironment(TsaConfigurationContext.TerracottaCommandLineEnvironmentKeys.CONFIG_TOOL,
            TerracottaCommandLineEnvironment.DEFAULT.withJavaOpts("-Xms8m -Xmx128m"))
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
    // note: node port and group port will be automatically set
    return server(symbolicName, "localhost")
        .configRepo(getNodePath(stripeId, nodeId).resolve("config").toString())
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

  protected final Path getNodeConfigDir(int stripeId, int nodeId) {
    return getNodePath(stripeId, nodeId).resolve("config");
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

  protected Path generateNodeConfigDir(int stripeId, int nodeId, Consumer<ConfigurationGenerator> fn) throws Exception {
    Path nodeConfigurationDir = getBaseDir().resolve(getNodeConfigDir(stripeId, nodeId));
    Path configDirs = getBaseDir().resolve("generated-configs");
    ConfigurationGenerator clusterGenerator = new ConfigurationGenerator(configDirs, new ConfigurationGenerator.PortSupplier() {
      @Override
      public int getNodePort(int stripeId, int nodeId) {
        return angela.getNodePort(stripeId, nodeId);
      }

      @Override
      public int getNodeGroupPort(int stripeId, int nodeId) {
        return angela.getNodeGroupPort(stripeId, nodeId);
      }
    });
    LOGGER.debug("Generating cluster node configuration directories into: {}", configDirs);
    fn.accept(clusterGenerator);
    org.terracotta.utilities.io.Files.copy(configDirs.resolve("stripe-" + stripeId).resolve("node-" + nodeId), nodeConfigurationDir, RECURSIVE);
    LOGGER.debug("Created node configuration directory into: {}", nodeConfigurationDir);
    return nodeConfigurationDir;
  }

  private Properties generateProperties() {
    Properties props = new Properties();
    rangeClosed(1, angela.getStripeCount()).forEach(stripeId ->
        rangeClosed(1, angela.getNodeCount(stripeId)).forEach(nodeId -> {
          props.setProperty(("PORT-" + stripeId + "-" + nodeId), String.valueOf(angela.getNodePort(stripeId, nodeId)));
          props.setProperty(("GROUP-PORT-" + stripeId + "-" + nodeId), String.valueOf(angela.getNodeGroupPort(stripeId, nodeId)));
        }));
    return props;
  }

  // =========================================
  // assertions
  // =========================================

  protected final void waitUntil(ConfigToolExecutionResult result, Matcher<ConfigToolExecutionResult> matcher) {
    waitUntil(() -> result, matcher, getAssertTimeout());
  }

  protected final void waitUntil(NodeOutputRule.NodeLog result, Matcher<NodeOutputRule.NodeLog> matcher) {
    waitUntil(() -> result, matcher, getAssertTimeout());
  }

  protected final <T> void waitUntil(Supplier<T> callable, Matcher<T> matcher) {
    waitUntil(callable, matcher, getAssertTimeout());
  }

  protected final <T> void waitUntil(Supplier<T> callable, Matcher<T> matcher, Duration timeout) {
    assertThat(callable, within(timeout).matches(matcher));
  }

  protected final void waitForActive(int stripeId) {
    waitUntil(() -> findActive(stripeId).isPresent(), is(true));
  }

  protected final void waitForActive(int stripeId, int nodeId) {
    waitUntil(() -> angela.tsa().getState(getNode(stripeId, nodeId)), is(equalTo(STARTED_AS_ACTIVE)));
  }

  protected final void waitForPassive(int stripeId, int nodeId) {
    waitUntil(() -> angela.tsa().getState(getNode(stripeId, nodeId)), is(equalTo(STARTED_AS_PASSIVE)));
  }

  protected final void waitForDiagnostic(int stripeId, int nodeId) {
    waitUntil(() -> angela.tsa().getState(getNode(stripeId, nodeId)), is(equalTo(STARTED_IN_DIAGNOSTIC_MODE)));
  }

  protected final void waitForStopped(int stripeId, int nodeId) {
    waitUntil(() -> angela.tsa().getState(getNode(stripeId, nodeId)), is(equalTo(STOPPED)));
  }

  protected final void waitForPassives(int stripeId) {
    int expectedPassiveCount = angela.getNodeCount(stripeId) - 1;
    waitUntil(() -> findPassives(stripeId).length, is(equalTo(expectedPassiveCount)));
  }

  protected final void waitForNPassives(int stripeId, int count) {
    waitUntil(() -> findPassives(stripeId).length, is(equalTo(count)));
  }

  protected final Cluster getUpcomingCluster(int stripeId, int nodeId) throws Exception {
    return getUpcomingCluster("localhost", getNodePort(stripeId, nodeId));
  }

  // =========================================
  // information retrieval
  // =========================================

  protected final Cluster getUpcomingCluster(String host, int port) throws Exception {
    return usingTopologyService(host, port, topologyService -> topologyService.getUpcomingNodeContext().getCluster());
  }

  protected final Cluster getRuntimeCluster(int stripeId, int nodeId) throws Exception {
    return getUpcomingCluster("localhost", getNodePort(stripeId, nodeId));
  }

  protected final Cluster getRuntimeCluster(String host, int port) throws Exception {
    return usingTopologyService(host, port, topologyService -> topologyService.getRuntimeNodeContext().getCluster());
  }

  protected final void withTopologyService(int stripeId, int nodeId, Consumer<TopologyService> consumer) throws Exception {
    withTopologyService("localhost", getNodePort(stripeId, nodeId), consumer);
  }

  protected final void withTopologyService(String host, int port, Consumer<TopologyService> consumer) throws Exception {
    usingTopologyService(host, port, topologyService -> {
      consumer.accept(topologyService);
      return null;
    });
  }

  protected final <T> T usingTopologyService(int stripeId, int nodeId, Function<TopologyService, T> fn) throws Exception {
    return usingTopologyService("localhost", getNodePort(stripeId, nodeId), fn);
  }

  protected final <T> T usingTopologyService(String host, int port, Function<TopologyService, T> fn) throws Exception {
    try (DiagnosticService diagnosticService = DiagnosticServiceFactory.fetch(
        InetSocketAddress.createUnresolved(host, port),
        getClass().getSimpleName(),
        getConnectionTimeout(),
        getConnectionTimeout(),
        null)) {
      return fn.apply(diagnosticService.getProxy(TopologyService.class));
    }
  }

  protected Duration getConnectionTimeout() {
    return CONN_TIMEOUT;
  }

  protected Duration getAssertTimeout() {
    return ASSERT_TIMEOUT;
  }
}