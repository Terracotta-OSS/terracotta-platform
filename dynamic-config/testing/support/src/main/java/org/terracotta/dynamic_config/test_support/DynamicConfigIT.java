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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.apache.commons.lang3.ArrayUtils;
import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.rules.RuleChain;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.angela.client.config.ConfigurationContext;
import org.terracotta.angela.client.filesystem.RemoteFolder;
import org.terracotta.angela.client.support.junit.AngelaRule;
import org.terracotta.angela.common.TerracottaCommandLineEnvironment;
import org.terracotta.angela.common.TerracottaConfigTool;
import org.terracotta.angela.common.ToolExecutionResult;
import org.terracotta.angela.common.distribution.Distribution;
import org.terracotta.angela.common.dynamic_cluster.Stripe;
import org.terracotta.angela.common.tcconfig.License;
import org.terracotta.angela.common.tcconfig.TerracottaServer;
import org.terracotta.angela.common.topology.Topology;
import org.terracotta.common.struct.Measure;
import org.terracotta.common.struct.TimeUnit;
import org.terracotta.connection.ConnectionException;
import org.terracotta.diagnostic.client.DiagnosticService;
import org.terracotta.diagnostic.client.DiagnosticServiceFactory;
import org.terracotta.dynamic_config.api.json.DynamicConfigApiJsonModule;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.FailoverPriority;
import org.terracotta.dynamic_config.api.model.RawPath;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.cli.api.output.ConsoleOutputService;
import org.terracotta.dynamic_config.cli.api.output.InMemoryOutputService;
import org.terracotta.dynamic_config.cli.api.output.OutputService;
import org.terracotta.dynamic_config.cli.config_tool.ConfigTool;
import org.terracotta.dynamic_config.test_support.util.ConfigurationGenerator;
import org.terracotta.dynamic_config.test_support.util.PropertyResolver;
import org.terracotta.json.ObjectMapperFactory;
import org.terracotta.testing.ExtendedTestRule;
import org.terracotta.testing.Timeout;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.lang.System.lineSeparator;
import static java.util.Collections.emptyList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.IntStream.rangeClosed;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import org.junit.AfterClass;
import static org.junit.Assert.assertThat;
import org.junit.BeforeClass;
import org.terracotta.angela.client.ClusterAgent;
import static org.terracotta.angela.client.config.custom.CustomConfigurationContext.customConfigurationContext;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;
import static org.terracotta.angela.common.AngelaProperties.DISTRIBUTION;
import static org.terracotta.angela.common.TerracottaServerState.STARTED_AS_ACTIVE;
import static org.terracotta.angela.common.TerracottaServerState.STARTED_AS_PASSIVE;
import static org.terracotta.angela.common.TerracottaServerState.STARTED_IN_DIAGNOSTIC_MODE;
import static org.terracotta.angela.common.TerracottaServerState.STOPPED;
import static org.terracotta.angela.common.distribution.Distribution.distribution;
import org.terracotta.angela.common.distribution.RuntimeOption;
import static org.terracotta.angela.common.dynamic_cluster.Stripe.stripe;
import org.terracotta.angela.common.net.DefaultPortAllocator;
import static org.terracotta.angela.common.provider.DynamicConfigManager.dynamicCluster;
import static org.terracotta.angela.common.tcconfig.TerracottaServer.server;
import static org.terracotta.angela.common.topology.LicenseType.TERRACOTTA_OS;
import static org.terracotta.angela.common.topology.PackageType.KIT;
import static org.terracotta.angela.common.topology.Version.version;
import static org.terracotta.common.struct.Tuple2.tuple2;
import static org.terracotta.utilities.io.Files.ExtendedOption.RECURSIVE;
import static org.terracotta.utilities.test.matchers.Eventually.within;

public class DynamicConfigIT {
  protected static final String CLUSTER_NAME = "tc-cluster";

  private static final Logger LOGGER = LoggerFactory.getLogger(DynamicConfigIT.class);
  private static final Duration DEFAULT_TEST_TIMEOUT = Duration.ofMinutes(2);
  private static final Duration CONN_TIMEOUT = Duration.ofSeconds(30);
  private static final Duration ASSERT_TIMEOUT = Duration.ofMinutes(1);

  protected final TmpDir tmpDir;
  protected final AngelaRule angela;
  protected final long timeout;

  protected final ObjectMapperFactory objectMapperFactory = new ObjectMapperFactory().withModule(new DynamicConfigApiJsonModule());

  private static ClusterAgent localAgent;
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
        .around(angela = new AngelaRule(localAgent, createConfigurationContext(clusterDef.stripes(), clusterDef.nodesPerStripe(), clusterDef.netDisruptionEnabled(), clusterDef.inlineServers()), clusterDef.autoStart(), clusterDef.autoActivate()) {
          ConfigurationContext oldConfiguration;

          @Override
          public void startNode(int stripeId, int nodeId) {
            // let the subclasses control the node startup
            DynamicConfigIT.this.startNode(stripeId, nodeId);
          }

          @Override
          protected void before(Description description) throws Throwable {
            InlineServers inline = description.getAnnotation(InlineServers.class);
            if (inline != null) {
              oldConfiguration = configure(createConfigurationContext(clusterDef.stripes(), clusterDef.nodesPerStripe(), clusterDef.netDisruptionEnabled(), inline.value()));
            }
            super.before(description);
          }

          @Override
          protected void after(Description description) throws Throwable {
            super.after(description);
            if (oldConfiguration != null) {
              configure(oldConfiguration);
              oldConfiguration = null;
            }
          }
        })
        .around(Timeout.builder()
            .withLookingForStuckThread(true)
            .withThreadDump(Paths.get(System.getProperty("user.dir")).resolve("target").resolve("thread-dumps"))
            .withTimeout(testTimeout.toMillis(), MILLISECONDS)
            .build())
        .around(new ExtendedTestRule() {
          @Override
          protected void before(Description description) {
            InlineServers inline = description.getAnnotation(InlineServers.class);
            String baseLogging = "tc-logback.xml";
            if (inline != null && !inline.value()) {
              baseLogging = "tc-logback-console.xml";
            }
            String extraLogging = "logback-ext-test.xml";
            ExtraLogging extra = description.getAnnotation(ExtraLogging.class);
            if (extra != null) {
              extraLogging = extra.value();
            }
            // upload tc logging config, but ONLY IF EXISTS !
            Stream.of(tuple2(baseLogging, "logback-test.xml"), tuple2(extraLogging, "logback-ext-test.xml"))
                .map(loggingConfig -> tuple2(getClass().getResource("/" + loggingConfig.t1), loggingConfig.t2))
                .filter(tuple -> tuple.t1 != null)
                .forEach(loggingConfig -> {
                  angela.tsa().getTsaConfigurationContext().getTopology().getServers().forEach(s -> {
                    try {
                      RemoteFolder folder = angela.tsa().browse(s, "");
                      folder.upload(loggingConfig.t2, loggingConfig.t1);
                    } catch (IOException exp) {
                      LOGGER.warn("unable to upload logback configuration", exp);
                    }
                  });
                });
                angela.tsa().getTsaConfigurationContext().getTopology().getServers().forEach(s -> {
                  try {
                    RemoteFolder folder = angela.tsa().browse(s, "");
                    Properties props = new Properties();
                    props.setProperty("serverWorkingDir", folder.getAbsoluteName());
                    props.setProperty("serverId", s.getServerSymbolicName().getSymbolicName());
                    props.setProperty("test.displayName", description.getDisplayName());
                    props.setProperty("test.className", description.getClassName());
                    props.setProperty("test.methodName", description.getMethodName());
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    props.store(bos, "logging properties");
                    bos.close();
                    folder.upload("logbackVars.properties", new ByteArrayInputStream(bos.toByteArray()));
                  } catch (IOException exp) {
                    LOGGER.warn("unable to upload logback configuration", exp);
                  }
                });
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

  @BeforeClass
  public static void setupTestServers() {
    System.setProperty("com.tc.server.entity.processor.threads", "4");
    System.setProperty("com.tc.l2.tccom.workerthreads", "4");
    System.setProperty("com.tc.l2.seda.stage.stall.warning", "1000");
    System.setProperty("IGNITE_UPDATE_NOTIFIER", "false");
    localAgent = new ClusterAgent(true, new DefaultPortAllocator());
  }

  @AfterClass
  public static void teardownTestServers() {
    try {
      localAgent.close();
    } catch (IOException io) {
      LOGGER.error("io error", io);
    }
  }

  // =========================================
  // tmp dir
  // =========================================

  protected final Path getBaseDir() {
    return tmpDir.getRoot();
  }

  protected final Path getServerHome() {
    return getServerHome(getNode(1, 1));
  }

  protected final Path getServerHome(TerracottaServer server) {
    return Paths.get(angela.tsa().browse(server, "").getAbsoluteName());
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
    startNode(node, Collections.emptyMap(), cli);
  }

  protected final void startNode(TerracottaServer node, Map<String, String> env, String... cli) {
    angela.tsa().start(node, env, cli);
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

  protected ToolExecutionResult activateCluster() {
    return activateCluster(CLUSTER_NAME);
  }

  protected ToolExecutionResult activateCluster(String name) {
    Path licensePath = getLicensePath();
    ToolExecutionResult result = licensePath == null ?
        configTool("activate", "-s", "localhost:" + getNodePort(), "-n", name) :
        configTool("activate", "-s", "localhost:" + getNodePort(), "-n", name, "-l", licensePath.toString());
    assertThat(result, is(successful()));
    waitForActive(1);
    return result;
  }

  protected ToolExecutionResult configTool(String... cli) {
    return configTool(Collections.emptyMap(), cli);
  }

  protected ToolExecutionResult configTool(Map<String, String> env, String... cli) {
    List<String> enhancedCli = new ArrayList<>(cli.length);
    List<String> configToolOptions = getConfigToolOptions(cli);

    boolean addedOptions = false;
    String timeout = Measure.of(getConnectionTimeout().getSeconds(), TimeUnit.SECONDS).toString();
    if (!configToolOptions.contains("-t") && !configToolOptions.contains("-connection-timeout")) {
      // Add the option if it wasn't already passed in the `cli` parameter as a config tool option
      enhancedCli.add("-t");
      enhancedCli.add(timeout);
      addedOptions = true;
    }

    if (!configToolOptions.contains("-r") && !configToolOptions.contains("-request-timeout")) {
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

    // config-tool launched through angela from the kit in
    // its own process. Slower and harder to debug.
//    {
//      return angela.configTool().executeCommand(env, cmd);
//    }

    // inline config-tool launched from within the test classpath.
    // faster and easier to debug, but does not test the kit packaging
    {
      LOGGER.info("config-tool {}", String.join(" ", cmd));
      InlineToolExecutionResult result = new InlineToolExecutionResult();
      OutputService out = new ConsoleOutputService().then(result);
      try {
        new ConfigTool(out).run(cmd);
        result.setExitStatus(0);
      } catch (Exception e) {
        result.setExitStatus(1);
        out.warn("Error: {}", e.getMessage());
      }
      return result;
    }
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

  protected final ConfigurationContext createConfigurationContext(int stripes, int nodesPerStripe, boolean netDisruptionEnabled, boolean inlineServers) {
    return customConfigurationContext()
        .tsa(tsa -> tsa
            .clusterName(CLUSTER_NAME)
            .license(getLicenceUrl() == null ? null : new License(getLicenceUrl()))
            .terracottaCommandLineEnvironment(TerracottaCommandLineEnvironment.DEFAULT.withJavaOpts("-Xms32m -Xmx256m"))
            .topology(new Topology(
                getDistribution(inlineServers),
                netDisruptionEnabled,
                dynamicCluster(
                    rangeClosed(1, stripes)
                        .mapToObj(stripeId -> stripe(rangeClosed(1, nodesPerStripe)
                            .mapToObj(nodeId -> createNode(stripeId, nodeId))
                            .toArray(TerracottaServer[]::new)))
                        .toArray(Stripe[]::new)))))
        .configTool(context -> context
            .distribution(getDistribution())
            .license(getLicenceUrl() == null ? null : new License(getLicenceUrl()))
            .commandLineEnv(TerracottaCommandLineEnvironment.DEFAULT.withJavaOpts("-Xms8m -Xmx128m"))
            .configTool(TerracottaConfigTool.configTool("config-tool", "localhost")));
  }

  protected TerracottaServer createNode(int stripeId, int nodeId) {
    return server(getNodeName(stripeId, nodeId), "localhost")
        .configRepo("config")
        .logs("logs")
        .dataDir("main:data-dir")
        .offheap("main:512MB,foo:1GB")
        .metaData("metadata")
        .failoverPriority(getFailoverPriority().toString())
        .clientReconnectWindow("10s") // the default client reconnect window of 2min can cause some tests to timeout
        .clusterName(CLUSTER_NAME);
  }

  protected FailoverPriority getFailoverPriority() {
    return FailoverPriority.availability();
  }

  protected Distribution getDistribution() {
    return distribution(version(DISTRIBUTION.getValue()), KIT, TERRACOTTA_OS);
  }

  protected Distribution getDistribution(boolean inline) {
    if (inline) {
      return distribution(version(DISTRIBUTION.getValue()), KIT, TERRACOTTA_OS, RuntimeOption.INLINE_SERVERS);
    } else {
      return distribution(version(DISTRIBUTION.getValue()), KIT, TERRACOTTA_OS);
    }
  }

  protected String getNodeName(int stripeId, int nodeId) {
    return "node-" + stripeId + "-" + nodeId;
  }

  protected final RawPath getNodeConfigDir(int stripeId, int nodeId) {
    return RawPath.valueOf("config");
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
    TerracottaServer server = getNode(stripeId, nodeId);
    Path nodeWorkingDir = Paths.get(angela.tsa().browse(server, "").getAbsoluteName());
    Path nodeConfigurationDir = nodeWorkingDir.resolve("config");
    Files.createDirectories(nodeWorkingDir);
    Path configDirs = getBaseDir().resolve("generated-configs");
    ConfigurationGenerator clusterGenerator = new ConfigurationGenerator(configDirs, nodeWorkingDir, new ConfigurationGenerator.PortSupplier() {
      @Override
      public int getNodePort(int stripeId, int nodeId) {
        return angela.getNodePort(stripeId, nodeId);
      }

      @Override
      public int getNodeGroupPort(int stripeId, int nodeId) {
        return angela.getNodeGroupPort(stripeId, nodeId);
      }
    });
    LOGGER.debug("Generating cluster node configuration directories into: {}", nodeConfigurationDir);
    fn.accept(clusterGenerator);
    org.terracotta.utilities.io.Files.copy(configDirs.resolve("stripe-" + stripeId).resolve("node-" + stripeId + "-" + nodeId), nodeConfigurationDir, RECURSIVE);
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

  protected final void waitUntil(ToolExecutionResult result, Matcher<ToolExecutionResult> matcher) {
    waitUntil(() -> result, matcher, getAssertTimeout());
  }

  protected final void waitUntilServerLogs(TerracottaServer server, String matcher) {
    assertThat(()->serverStdOut(server), within(getAssertTimeout()).matches(hasItem(containsString(matcher))));
  }

  protected final void assertThatServerLogs(TerracottaServer server, String matcher) {
    assertThat(serverStdOut(server), hasItem(containsString(matcher)));
  }

  protected final void assertThatServerLogs(TerracottaServer server, Matcher<String> matcher) {
    assertThat(serverStdOut(server), hasItem(matcher));
  }

  private List<String> serverStdOut(TerracottaServer server) {
    try {
      return Files.readAllLines(getServerHome(server).resolve("stdout.txt"));
    } catch (IOException io) {
      return Collections.emptyList();
    }
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

  protected final Cluster getUpcomingCluster(int stripeId, int nodeId) {
    return getUpcomingCluster("localhost", getNodePort(stripeId, nodeId));
  }

  // =========================================
  // information retrieval
  // =========================================

  protected final Cluster getUpcomingCluster(String host, int port) {
    return usingTopologyService(host, port, topologyService -> topologyService.getUpcomingNodeContext().getCluster());
  }

  protected final Cluster getRuntimeCluster(int stripeId, int nodeId) {
    return getUpcomingCluster("localhost", getNodePort(stripeId, nodeId));
  }

  protected final Cluster getRuntimeCluster(String host, int port) {
    return usingTopologyService(host, port, topologyService -> topologyService.getRuntimeNodeContext().getCluster());
  }

  protected final void withTopologyService(int stripeId, int nodeId, Consumer<TopologyService> consumer) {
    withTopologyService("localhost", getNodePort(stripeId, nodeId), consumer);
  }

  protected final void withTopologyService(String host, int port, Consumer<TopologyService> consumer) {
    usingTopologyService(host, port, topologyService -> {
      consumer.accept(topologyService);
      return null;
    });
  }

  protected final <T> T usingTopologyService(int stripeId, int nodeId, Function<TopologyService, T> fn) {
    return usingTopologyService("localhost", getNodePort(stripeId, nodeId), fn);
  }

  protected final <T> T usingTopologyService(String host, int port, Function<TopologyService, T> fn) {
    return usingDiagnosticService(host, port, diagnosticService -> fn.apply(diagnosticService.getProxy(TopologyService.class)));
  }

  protected final <T> T usingDiagnosticService(int stripeId, int nodeId, Function<DiagnosticService, T> fn) {
    return usingDiagnosticService("localhost", getNodePort(stripeId, nodeId), fn);
  }

  protected final <T> T usingDiagnosticService(String host, int port, Function<DiagnosticService, T> fn) {
    try (DiagnosticService diagnosticService = DiagnosticServiceFactory.fetch(
        InetSocketAddress.createUnresolved(host, port),
        getClass().getSimpleName(),
        getConnectionTimeout(),
        getConnectionTimeout(),
        null,
        objectMapperFactory)) {
      return fn.apply(diagnosticService);
    } catch (ConnectionException e) {
      throw new RuntimeException(e);
    }
  }

  protected Duration getConnectionTimeout() {
    return CONN_TIMEOUT;
  }

  protected Duration getAssertTimeout() {
    return ASSERT_TIMEOUT;
  }

  protected void setServerDisruptionLinks(Map<Integer, Integer> stripeServer) {
    stripeServer.forEach((k, v) -> angela.configTool().setServerToServerDisruptionLinks(k, v));
  }

  protected void setClientServerDisruptionLinks(Map<Integer, Integer> stripeServerNumMap) {
    for (Map.Entry<Integer, Integer> entry : stripeServerNumMap.entrySet()) {
      int stripeId = entry.getKey();
      int serverList = entry.getValue();
      for (int i = 1; i <= serverList; ++i) {
        TerracottaServer terracottaServer = getNode(stripeId, i);
        angela.configTool().setClientToServerDisruptionLinks(terracottaServer);
      }
    }
  }

  @SuppressFBWarnings("REC_CATCH_EXCEPTION")
  private boolean isServerBlocked(TerracottaServer server) {
    try (DiagnosticService diagnosticService = DiagnosticServiceFactory.fetch(
        InetSocketAddress.createUnresolved(server.getHostName(), server.getTsaPort()),
        getClass().getSimpleName(),
        getConnectionTimeout(),
        getConnectionTimeout(),
        null,
        objectMapperFactory)) {
        return diagnosticService.isBlocked();
    } catch (Exception e) {
      return false;
    }
  }

  protected void waitForServerBlocked(TerracottaServer server) {
    waitUntil(() -> isServerBlocked(server), is(true));
  }

  protected TerracottaServer waitForNewActive(TerracottaServer... servers) {
    waitUntil(() -> Arrays.stream(servers).anyMatch(server -> angela.tsa().getState(server) == STARTED_AS_ACTIVE), is(true));
    TerracottaServer active = Arrays.stream(servers)
        .filter(server -> angela.tsa().getState(server) == STARTED_AS_ACTIVE)
        .findFirst()
        .get();
    TerracottaServer[] passives = ArrayUtils.removeElements(servers, active);
    if (passives.length == 0) {
      return active;
    }
    waitUntil(() -> Arrays.stream(passives).allMatch(server -> angela.tsa().getState(server) == STARTED_AS_PASSIVE), is(true));
    return active;
  }

  public static class InlineToolExecutionResult extends ToolExecutionResult implements OutputService {

    private final InMemoryOutputService delegate = new InMemoryOutputService();
    private int exitStatus = -1;

    public InlineToolExecutionResult() {
      super(-1, emptyList());
    }

    public void setExitStatus(int exitStatus) {
      this.exitStatus = exitStatus;
    }

    @Override
    public int getExitStatus() {
      return exitStatus;
    }

    @Override
    public List<String> getOutput() {
      return delegate.getOutput();
    }

    @Override
    public String toString() {
      return String.join(lineSeparator(), getOutput());
    }

    @Override
    public void out(String format, Object... args) {
      delegate.out(format, args);
    }

    @Override
    public void info(String format, Object... args) {
      delegate.info(format, args);
    }

    @Override
    public void warn(String format, Object... args) {
      delegate.warn(format, args);
    }

    @Override
    public void close() {
      delegate.close();
    }
  }
}
