/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
import org.apache.commons.lang3.ArrayUtils;
import org.hamcrest.Matcher;
import org.junit.AssumptionViolatedException;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.RuleChain;
import org.junit.rules.TestWatcher;
import org.junit.rules.Timeout;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.angela.client.config.ConfigurationContext;
import org.terracotta.angela.client.support.junit.AngelaOrchestratorRule;
import org.terracotta.angela.client.support.junit.AngelaRule;
import org.terracotta.angela.common.TerracottaCommandLineEnvironment;
import org.terracotta.angela.common.TerracottaConfigTool;
import org.terracotta.angela.common.ToolException;
import org.terracotta.angela.common.ToolExecutionResult;
import org.terracotta.angela.common.distribution.Distribution;
import org.terracotta.angela.common.distribution.RuntimeOption;
import org.terracotta.angela.common.dynamic_cluster.Stripe;
import org.terracotta.angela.common.tcconfig.License;
import org.terracotta.angela.common.tcconfig.ServerSymbolicName;
import org.terracotta.angela.common.tcconfig.TerracottaServer;
import org.terracotta.angela.common.topology.Topology;
import org.terracotta.common.struct.Measure;
import org.terracotta.common.struct.TimeUnit;
import org.terracotta.connection.ConnectionException;
import org.terracotta.diagnostic.client.DiagnosticService;
import org.terracotta.diagnostic.client.DiagnosticServiceFactory;
import org.terracotta.dynamic_config.api.json.DynamicConfigJsonModule;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.FailoverPriority;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.cli.api.output.InMemoryOutputService;
import org.terracotta.dynamic_config.cli.api.output.LoggingOutputService;
import org.terracotta.dynamic_config.cli.api.output.OutputService;
import org.terracotta.dynamic_config.cli.config_tool.ConfigTool;
import org.terracotta.dynamic_config.test_support.util.ConfigurationGenerator;
import org.terracotta.inet.HostPort;
import org.terracotta.json.DefaultJsonFactory;
import org.terracotta.json.Json;
import org.terracotta.testing.ExtendedTestRule;
import org.terracotta.testing.JavaTool;
import org.terracotta.testing.TmpDir;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.lang.System.lineSeparator;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.rangeClosed;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.terracotta.angela.client.config.custom.CustomConfigurationContext.customConfigurationContext;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;
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
import static org.terracotta.common.struct.Tuple3.tuple3;
import static org.terracotta.utilities.io.Files.ExtendedOption.RECURSIVE;

public class DynamicConfigIT {
  private static final Logger LOGGER = LoggerFactory.getLogger(DynamicConfigIT.class);

  protected static final String CLUSTER_NAME = "tc-cluster";
  protected static final AngelaOrchestratorRule angelaOrchestratorRule;

  protected final TmpDir tmpDir;
  protected final AngelaRule angela;
  protected final Json.Factory jsonFactory = new DefaultJsonFactory().withModule(new DynamicConfigJsonModule());
  protected final Json json = jsonFactory.create();

  // can be modified by subclasses to update the timeouts

  // We intentionally set a huge timeout for connection
  protected Duration connectionTimeout = Duration.ofDays(1);

  // time given for a diagnostic operation to complete
  protected Duration diagnosticOperationTimeout = Duration.ofDays(1);

  // time given for an entity operation to complete (there could be a failover in the middle)
  protected Duration entityOperationTimeout = Duration.ofDays(1);

  // time given for the nodes to restart
  protected Duration restartTimeout = Duration.ofDays(1);

  // time given for the nodes to be stopped
  protected Duration stopTimeout = Duration.ofDays(1);

  protected boolean verbose;

  @ClassRule
  public static final RuleChain classRules = RuleChain.emptyRuleChain()
      .around(angelaOrchestratorRule = new AngelaOrchestratorRule().igniteFree());

  @Rule
  public final RuleChain testRules;

  public DynamicConfigIT() {
    this(Duration.ofMinutes(3));
  }

  public DynamicConfigIT(Duration testTimeout) {
    this(testTimeout, Paths.get(System.getProperty("user.dir"), "target", "test-data"));
  }

  public DynamicConfigIT(Duration testTimeout, Path parentTmpDir) {
    ClusterDefinition clusterDef = getClass().getAnnotation(ClusterDefinition.class);
    this.testRules = RuleChain.emptyRuleChain()
        .around(tmpDir = new TmpDir(parentTmpDir, false))
        .around(angela = new AngelaRule(angelaOrchestratorRule::getAngelaOrchestrator, null, false, false) {
          ConfigurationContext oldConfiguration;

          @Override
          public void startNode(int stripeId, int nodeId) {
            // let the subclasses control the node startup
            DynamicConfigIT.this.startNode(stripeId, nodeId);
          }

          @Override
          protected void before(Description description) {
            InlineServers inline = description.getAnnotation(InlineServers.class);
            if (inline != null) {
              oldConfiguration = configure(createConfigurationContext(clusterDef.stripes(), clusterDef.nodesPerStripe(), clusterDef.netDisruptionEnabled(), inline.value()));
            } else {
              configure(createConfigurationContext(clusterDef.stripes(), clusterDef.nodesPerStripe(), clusterDef.netDisruptionEnabled(), clusterDef.inlineServers()));
            }
            super.before(description);
          }

          @Override
          protected void after(Description description) {
            super.after(description);
            if (oldConfiguration != null) {
              configure(oldConfiguration);
              oldConfiguration = null;
            }
          }
        })
        .around(Timeout.builder()
            .withLookingForStuckThread(true)
            .withTimeout(testTimeout.toMillis(), MILLISECONDS)
            .build())
        .around(new ExtendedTestRule() {
          @Override
          protected void before(Description description) {
            // wait for server startup if auto-activated
            if (clusterDef.autoStart()) {
              int stripes = angela.getStripeCount();
              for (int x = 1; x <= stripes; x++) {
                int nodes = angela.getStripe(x).size();
                for (int n = 1; n <= nodes; n++) {
                  startNode(x, n);
                }
              }
              if (clusterDef.autoActivate()) {
                attachAll();
                activateCluster();
              }
            }
          }
        })
        .around(new TestWatcher() {
          @Override
          protected void succeeded(Description description) {
            LOGGER.info("[SUCCESS] {}", description);
          }

          @Override
          protected void failed(Throwable e, Description description) {
            LOGGER.info("[FAILED] {}", description);
            // take some thread dumps and memory dumps if the test has failed
            // to check if this is a timeout, use: if(throwable instanceof MultipleFailureException || throwable instanceof TestTimedOutException) {...}
            {
              // thread dumps
              Path threadDumpOutput = parentTmpDir.resolve("thread-dumps")
                  .resolve(description.getTestClass().getSimpleName())
                  .resolve(description.getMethodName() == null ? "_class_" : description.getMethodName());
              LOGGER.info("Taking thread dumps after timeout of test: {} into: {}", description, threadDumpOutput);
              JavaTool.threadDumps(threadDumpOutput, Duration.ofSeconds(15));
            }
            {
              // memory dumps on test failures
              //Path memoryDumpOutput = target.resolve("memory-dumps").resolve(description.toString());
              //LOGGER.info("Taking memory dumps after timeout of test: {} into: {}", description, memoryDumpOutput);
              //JavaTool.threadDumps(memoryDumpOutput, Duration.ofSeconds(20));
            }
          }

          @Override
          protected void starting(Description description) {
            LOGGER.info("[STARTING] {}", description);
          }

          @Override
          protected void skipped(AssumptionViolatedException e, Description description) {
            LOGGER.info("[SKIPPED] {}", description);
          }
        });
  }

  // timeouts

  protected Duration getConnectionTimeout() {
    return connectionTimeout;
  }

  protected Duration getDiagnosticOperationTimeout() {
    return diagnosticOperationTimeout;
  }

  public Duration getEntityOperationTimeout() {
    return entityOperationTimeout;
  }

  public Duration getRestartTimeout() {
    return restartTimeout;
  }

  public Duration getStopTimeout() {
    return stopTimeout;
  }

  public boolean isVerbose() {
    return verbose;
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
    angela.startNode(node, cli);
  }

  protected final void startNode(TerracottaServer node, Map<String, String> env, String... cli) {
    angela.startNode(node, env, cli);
  }

  protected final void stopNode(int stripeId, int nodeId) {
    angela.stopNode(stripeId, nodeId);
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

  protected final int getNodePort() {
    return getNodePort(1, 1);
  }

  protected final InetSocketAddress getNodeAddress(int stripeId, int nodeId) {
    return angela.getNodeAddress(stripeId, nodeId);
  }

  protected final HostPort getNodeHostPort(int stripeId, int nodeId) {
    return HostPort.create(getNodeAddress(stripeId, nodeId));
  }

  protected String getDefaultHostname(int stripeId, int nodeId) {
    return "localhost";
  }

  protected ToolExecutionResult activateCluster() {
    return activateCluster(CLUSTER_NAME);
  }

  protected void attachAll() {
    int stripes = angela.getStripeCount();
    for (int stripeId = 1; stripeId <= stripes; stripeId++) {
      List<TerracottaServer> stripe = angela.getStripe(stripeId);
      if (stripe.size() > 1) {
        // Attach all servers in a stripe to form individual stripes
        for (int nodeId = 2; nodeId <= stripe.size(); nodeId++) {
          List<String> command = new ArrayList<>();
          command.add("attach");
          command.add("-t");
          command.add("node");
          command.add("-d");
          command.add(getNodeHostPort(stripeId, 1).toString());
          command.add("-s");
          command.add(getNodeHostPort(stripeId, nodeId).toString());

          ToolExecutionResult result = configTool(command.toArray(new String[0]));
          if (result.getExitStatus() != 0) {
            throw new ToolException("attach failed", String.join(". ", result.getOutput()), result.getExitStatus());
          }
        }
      }
    }

    if (stripes > 1) {
      for (int i = 2; i <= stripes; i++) {
        // Attach all stripes together to form the cluster
        List<String> command = new ArrayList<>();
        command.add("attach");
        command.add("-t");
        command.add("stripe");
        command.add("-d");
        command.add(getNode(1, 1).getHostPort());

        List<TerracottaServer> stripe = angela.getStripe(i);
        command.add("-s");
        command.add(stripe.get(0).getHostPort());

        ToolExecutionResult result = configTool(command.toArray(new String[0]));
        if (result.getExitStatus() != 0) {
          throw new RuntimeException("ConfigTool::executeCommand with command parameters failed with: " + result);
        }
      }
    }
  }

  protected ToolExecutionResult activateCluster(String name) {
    return activateStripe(name, 1);
  }

  protected ToolExecutionResult activateStripe(String name, int stripe) {
    Path licensePath = getLicensePath();
    ToolExecutionResult result = licensePath == null ?
        configTool("activate", "-s", "localhost:" + getNodePort(stripe, 1), "-n", name) :
        configTool("activate", "-s", "localhost:" + getNodePort(stripe, 1), "-n", name, "-l", licensePath.toString());
    assertThat(result, is(successful()));
    return result;
  }

  protected ToolExecutionResult configTool(String... cli) {
    String command = null;
    List<String> globalOpts = new ArrayList<>(0);
    List<String> commandOpts = new ArrayList<>(0);
    boolean useDeprecatedCommands = false;

    // parse the current command into parts
    for (String opt : cli) {
      useDeprecatedCommands |= opt.startsWith("--") || opt.startsWith("-") && opt.length() <= 3;
      if (opt.startsWith("-")) {
        if (command == null) {
          globalOpts.add(opt);
        } else {
          commandOpts.add(opt);
        }
      } else if (command == null) {
        command = opt;
      }
    }

    // prevent any timeouts to be set through the CLI - we wil add them
    Stream.of(
        tuple3(globalOpts, asList("-verbose", "-v", "--verbose"), "Verbose mode must be controlled by the field #verbose"),
        tuple3(globalOpts, asList("-connect-timeout", "-connection-timeout", "-t", "--connection-timeout"), "Connection timeout must be controlled by the field #connectionTimeout"),
        tuple3(globalOpts, asList("-request-timeout", "-r", "--request-timeout"), "Diagnostic operation timeout must be controlled by the field #diagnosticOperationTimeout"),
        tuple3(globalOpts, asList("-entity-request-timeout", "-er", "--entity-request-timeout"), "Entity operation timeout must be controlled by the field #entityOperationTimeout"),
        tuple3(commandOpts, asList("-restart-wait-time", "-W"), "Restart timeout must be controlled by the field #restartTimeout"),
        tuple3(commandOpts, asList("-stop-wait-time", "-W"), "Stop timeout must be controlled by the field #stopTimeout")
    ).forEach(tuple -> {
      if (tuple.t1.stream().anyMatch(tuple.t2::contains)) {
        throw new IllegalArgumentException(tuple.t3);
      }
    });

    List<String> newCli = new ArrayList<>(cli.length + 8);

    // verbose
    if (isVerbose()) {
      newCli.add(useDeprecatedCommands ? "-v" : "-verbose");
    }

    // conn. timeout
    newCli.add(useDeprecatedCommands ? "-t" : "-connect-timeout");
    newCli.add(Measure.of(getConnectionTimeout().getSeconds(), TimeUnit.SECONDS).toString());

    // diag req. timeout
    newCli.add(useDeprecatedCommands ? "-r" : "-request-timeout");
    newCli.add(Measure.of(getDiagnosticOperationTimeout().getSeconds(), TimeUnit.SECONDS).toString());

    // entity call timeout
    newCli.add(useDeprecatedCommands ? "-er" : "-entity-request-timeout");
    newCli.add(Measure.of(getEntityOperationTimeout().getSeconds(), TimeUnit.SECONDS).toString());

    // user input
    newCli.addAll(asList(cli));

    if (asList("activate", "attach", "set", "unset").contains(command)) {
      // restart timeout
      newCli.add(useDeprecatedCommands ? "-W" : "-restart-wait-time");
      newCli.add(Measure.of(getRestartTimeout().getSeconds(), TimeUnit.SECONDS).toString());
    } else if ("detach".equals(command)) {
      newCli.add(useDeprecatedCommands ? "-W" : "-stop-wait-time");
      newCli.add(Measure.of(getStopTimeout().getSeconds(), TimeUnit.SECONDS).toString());
    }

    // inline config-tool launched from within the test classpath.
    // faster and easier to debug, but does not test the kit packaging
    {
      LOGGER.info("config-tool {}", String.join(" ", newCli));
      InlineToolExecutionResult result = new InlineToolExecutionResult();
      OutputService out = new LoggingOutputService().then(result);
      try {
        new ConfigTool(out).run(newCli.toArray(new String[0]));
        result.setExitStatus(0);
      } catch (Exception e) {
        result.setExitStatus(1);
        out.warn("Error: {}", e.getMessage(), e);
      }
      return result;
    }
  }

  // =========================================
  // node and topology construction
  // =========================================

  protected ConfigurationContext createConfigurationContext(int stripes, int nodesPerStripe, boolean netDisruptionEnabled, boolean inlineServers) {
    return customConfigurationContext()
        .tsa(tsa -> tsa
            .clusterName(CLUSTER_NAME)
            .license(getLicenceUrl() == null ? null : new License(getLicenceUrl()))
            .terracottaCommandLineEnvironment(TerracottaCommandLineEnvironment.DEFAULT.withJavaOpts("-XX:+UseG1GC", "-Xmx1g", "-Dlogback.configurationFile=logback-test.xml", "-Dlogback.debug=true"))
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
            .commandLineEnv(TerracottaCommandLineEnvironment.DEFAULT.withJavaOpts("-Xms8m", "-Xmx128m"))
            .configTool(TerracottaConfigTool.configTool("config-tool")));
  }

  protected TerracottaServer createNode(int stripeId, int nodeId) {
    return server(getNodeName(stripeId, nodeId), getDefaultHostname(stripeId, nodeId))
        .configRepo("config")
        .logs("logs")
        .dataDir("main:data-dir")
        .offheap("main:512MB,foo:1GB")
        .metaData("metadata")
        .failoverPriority(Optional.ofNullable(getFailoverPriority()).map(Objects::toString).orElse(null))
        .clientReconnectWindow("10s") // the default client reconnect window of 2min can cause some tests to timeout
        .clusterName(CLUSTER_NAME);
  }

  protected FailoverPriority getFailoverPriority() {
    ClusterDefinition clusterDef = getClass().getAnnotation(ClusterDefinition.class);
    if (clusterDef == null) {
      return FailoverPriority.availability();
    }
    if (clusterDef.failoverPriority().isEmpty()) {
      return null;
    }
    return FailoverPriority.valueOf(clusterDef.failoverPriority());
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
    try {
      Path src = Paths.get(getClass().getResource(configFile).toURI());
      Path dest = getBaseDir().resolve(src.getFileName());
      Files.createDirectories(getBaseDir());

      // construct a stream of key-value mappings for the ports and group ports
      // then use these mappings to replace placeholders in the read file
      String updated = rangeClosed(1, angela.getStripeCount()).boxed()
          .flatMap(stripeId -> rangeClosed(1, angela.getNodeCount(stripeId)).boxed()
              .flatMap(nodeId -> Stream.of(
                  new AbstractMap.SimpleEntry<>("${PORT-" + stripeId + "-" + nodeId + "}", angela.getNodePort(stripeId, nodeId)),
                  new AbstractMap.SimpleEntry<>("${GROUP-PORT-" + stripeId + "-" + nodeId + "}", angela.getNodeGroupPort(stripeId, nodeId)))))
          .reduce(
              new String(Files.readAllBytes(src), StandardCharsets.UTF_8),
              (text, placeholder) -> text.replace(placeholder.getKey(), String.valueOf(placeholder.getValue())),
              (s, s2) -> {
                throw new UnsupportedOperationException();
              });
      Files.write(dest, updated.getBytes(StandardCharsets.UTF_8));
      return dest;
    } catch (URISyntaxException | IOException e) {
      throw new RuntimeException(e);
    }
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

  // =========================================
  // assertions
  // =========================================

  protected final void waitUntilServerStdOut(TerracottaServer server, String matcher) {
    angela.waitUntilServerStdOut(server, matcher);
  }

  protected final void assertThatServerStdOut(TerracottaServer server, String matcher) {
    angela.assertThatServerStdOut(server, matcher);
  }

  protected final void assertThatServerStdOut(TerracottaServer server, Matcher<String> matcher) {
    angela.assertThatServerStdOut(server, matcher);
  }

  protected final void waitUntilServerLogs(TerracottaServer server, String matcher) {
    angela.waitUntilServerLogs(server, matcher);
  }

  protected final <T> void waitUntil(Supplier<T> callable, Matcher<T> matcher) {
    angela.waitUntil(callable, matcher);
  }

  protected final int waitForActive(int stripeId) {
    return angela.waitForActive(stripeId);
  }

  protected final void waitForActive(int stripeId, int nodeId) {
    angela.waitForActive(stripeId, nodeId);
  }

  protected final void waitForPassive(int stripeId, int nodeId) {
    angela.waitForPassive(stripeId, nodeId);
  }

  protected final void waitForDiagnostic(int stripeId, int nodeId) {
    angela.waitForDiagnostic(stripeId, nodeId);
  }

  protected final void waitForStopped(int stripeId, int nodeId) {
    angela.waitForStopped(stripeId, nodeId);
  }

  protected final int[] waitForPassives(int stripeId) {
    return angela.waitForPassives(stripeId);
  }

  protected final int[] waitForNPassives(int stripeId, int count) {
    return angela.waitForNPassives(stripeId, count);
  }

  // =========================================
  // information retrieval
  // =========================================

  protected final Cluster getUpcomingCluster(int stripeId, int nodeId) {
    return getUpcomingCluster(getNode(stripeId, nodeId).getHostName(), getNodePort(stripeId, nodeId));
  }

  protected final Cluster getUpcomingCluster(String host, int port) {
    return usingTopologyService(host, port, topologyService -> topologyService.getUpcomingNodeContext().getCluster());
  }

  protected final Cluster getRuntimeCluster(int stripeId, int nodeId) {
    return getUpcomingCluster(getNode(stripeId, nodeId).getHostName(), getNodePort(stripeId, nodeId));
  }

  protected final Cluster getRuntimeCluster(String host, int port) {
    return usingTopologyService(host, port, topologyService -> topologyService.getRuntimeNodeContext().getCluster());
  }

  protected final void withTopologyService(int stripeId, int nodeId, Consumer<TopologyService> consumer) {
    withTopologyService(getNode(stripeId, nodeId).getHostName(), getNodePort(stripeId, nodeId), consumer);
  }

  protected final void withTopologyService(String host, int port, Consumer<TopologyService> consumer) {
    usingTopologyService(host, port, topologyService -> {
      consumer.accept(topologyService);
      return null;
    });
  }

  protected final <T> T usingTopologyService(int stripeId, int nodeId, Function<TopologyService, T> fn) {
    return usingTopologyService(getNode(stripeId, nodeId).getHostName(), getNodePort(stripeId, nodeId), fn);
  }

  protected final <T> T usingTopologyService(String host, int port, Function<TopologyService, T> fn) {
    return usingDiagnosticService(host, port, diagnosticService -> fn.apply(diagnosticService.getProxy(TopologyService.class)));
  }

  protected final <T> T usingDiagnosticService(int stripeId, int nodeId, Function<DiagnosticService, T> fn) {
    return usingDiagnosticService(getNode(stripeId, nodeId).getHostName(), getNodePort(stripeId, nodeId), fn);
  }

  protected final <T> T usingDiagnosticService(String host, int port, Function<DiagnosticService, T> fn) {
    try (DiagnosticService diagnosticService = DiagnosticServiceFactory.fetch(
        InetSocketAddress.createUnresolved(host, port),
        getClass().getSimpleName(),
        getConnectionTimeout(),
        getDiagnosticOperationTimeout(),
        null,
        jsonFactory)) {
      return fn.apply(diagnosticService);
    } catch (ConnectionException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  protected void setServerDisruptionLinks(Map<Integer, Integer> stripeServer) {
    stripeServer.forEach(this::setServerToServerDisruptionLinks);
  }

  protected void setClientServerDisruptionLinks(Map<Integer, Integer> stripeServerNumMap) {
    for (Map.Entry<Integer, Integer> entry : stripeServerNumMap.entrySet()) {
      int stripeId = entry.getKey();
      int serverList = entry.getValue();
      for (int i = 1; i <= serverList; ++i) {
        TerracottaServer terracottaServer = getNode(stripeId, i);
        setClientToServerDisruptionLinks(terracottaServer);
      }
    }
  }

  public void setClientToServerDisruptionLinks(TerracottaServer terracottaServer) {
    // Disabling client redirection from passive to current active.
    List<String> arguments = new ArrayList<>();
    String property = "stripe.1.node.1.tc-properties." + "l2.l1redirect.enabled=false";
    arguments.add("set");
    arguments.add("-s");
    arguments.add(terracottaServer.getHostPort());
    arguments.add("-c");
    arguments.add(property);
    ToolExecutionResult executionResult = configTool(arguments.toArray(new String[0]));
    if (executionResult.getExitStatus() != 0) {
      throw new RuntimeException("ConfigTool::executeCommand with command parameters failed with: " + executionResult);
    }

    // Creating disruption links for client to server disruption
    Map<ServerSymbolicName, Integer> proxyMap = angela.tsa().updateToProxiedPorts();
    int proxyPort = proxyMap.get(terracottaServer.getServerSymbolicName());
    String publicHostName = "stripe.1.node.1.public-hostname=" + terracottaServer.getHostName();
    String publicPort = "stripe.1.node.1.public-port=" + proxyPort;

    List<String> args = new ArrayList<>();
    args.add("set");
    args.add("-s");
    args.add(terracottaServer.getHostPort());
    args.add("-c");
    args.add(publicHostName);
    args.add("-c");
    args.add(publicPort);

    executionResult = configTool(args.toArray(new String[0]));
    if (executionResult.getExitStatus() != 0) {
      throw new RuntimeException("ConfigTool::executeCommand with command parameters failed with: " + executionResult);
    }
  }

  public void setServerToServerDisruptionLinks(int stripeId, int size) {
    List<TerracottaServer> stripeServerList = angela.tsa().getTsaConfigurationContext()
        .getTopology()
        .getStripes()
        .get(stripeId - 1);
    for (int j = 0; j < size; ++j) {
      TerracottaServer server = stripeServerList.get(j);
      Map<ServerSymbolicName, Integer> proxyGroupPortMapping = angela.tsa().getProxyGroupPortsForServer(server);
      int nodeId = j + 1;
      StringBuilder propertyBuilder = new StringBuilder();
      propertyBuilder.append("stripe.")
          .append(stripeId)
          .append(".node.")
          .append(nodeId)
          .append(".tc-properties.test-proxy-group-port=");
      propertyBuilder.append("\"");
      for (Map.Entry<ServerSymbolicName, Integer> entry : proxyGroupPortMapping.entrySet()) {
        propertyBuilder.append(entry.getKey().getSymbolicName());
        propertyBuilder.append("->");
        propertyBuilder.append(entry.getValue());
        propertyBuilder.append("#");
      }
      propertyBuilder.deleteCharAt(propertyBuilder.lastIndexOf("#"));
      propertyBuilder.append("\"");

      ToolExecutionResult executionResult = configTool("set", "-s", server.getHostPort(), "-c", propertyBuilder.toString());
      if (executionResult.getExitStatus() != 0) {
        throw new RuntimeException("ConfigTool::executeCommand with command parameters failed with: " + executionResult);
      }
    }
  }

  @SuppressFBWarnings("REC_CATCH_EXCEPTION")
  private boolean isServerBlocked(TerracottaServer server) {
    try (DiagnosticService diagnosticService = DiagnosticServiceFactory.fetch(
        InetSocketAddress.createUnresolved(server.getHostName(), server.getTsaPort()),
        getClass().getSimpleName(),
        getConnectionTimeout(),
        getDiagnosticOperationTimeout(),
        null,
        jsonFactory)) {
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
      return delegate.lines().collect(toList());
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
