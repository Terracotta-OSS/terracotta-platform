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
package org.terracotta.dynamic_config.server.configuration.service;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.util.AbstractMap.SimpleEntry;
import static java.util.Arrays.asList;
import java.util.Collection;
import static java.util.Collections.singletonList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.IntStream.range;
import java.util.stream.Stream;

import javax.management.MBeanServerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeThat;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.terracotta.common.struct.Measure;
import org.terracotta.common.struct.MemoryUnit;
import org.terracotta.diagnostic.model.LogicalServerState;
import org.terracotta.dynamic_config.api.json.DynamicConfigJsonModule;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Configuration;
import org.terracotta.dynamic_config.api.model.FailoverPriority;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Setting;
import static org.terracotta.dynamic_config.api.model.Setting.FAILOVER_PRIORITY;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_HOSTNAME;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_NAME;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_PORT;
import static org.terracotta.dynamic_config.api.model.Setting.OFFHEAP_RESOURCES;
import org.terracotta.dynamic_config.api.model.Version;
import org.terracotta.dynamic_config.api.model.nomad.Applicability;
import org.terracotta.dynamic_config.api.model.nomad.ClusterActivationNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.SettingNomadChange;
import org.terracotta.dynamic_config.api.server.ConfigChangeHandler;
import org.terracotta.dynamic_config.api.server.ConfigChangeHandlerManager;
import org.terracotta.dynamic_config.api.server.DynamicConfigNomadServer;
import org.terracotta.dynamic_config.api.server.InvalidConfigChangeException;
import org.terracotta.dynamic_config.api.server.LicenseParserDiscovery;
import org.terracotta.dynamic_config.api.server.LicenseService;
import org.terracotta.dynamic_config.api.service.ClusterFactory;
import org.terracotta.dynamic_config.api.service.ConfigurationConsistencyAnalyzer;
import org.terracotta.dynamic_config.api.service.ConfigurationConsistencyState;
import org.terracotta.dynamic_config.api.service.DynamicConfigService;
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadConfigurationManager;
import org.terracotta.dynamic_config.server.configuration.sync.DynamicConfigSyncData;
import org.terracotta.dynamic_config.server.configuration.sync.DynamicConfigurationPassiveSync;
import static org.terracotta.dynamic_config.server.configuration.sync.Require.NOTHING;
import org.terracotta.inet.HostPort;
import org.terracotta.json.DefaultJsonFactory;
import org.terracotta.json.Json;
import org.terracotta.nomad.NomadEnvironment;
import org.terracotta.nomad.client.NomadClient;
import org.terracotta.nomad.client.NomadEndpoint;
import org.terracotta.nomad.client.change.NomadChange;
import org.terracotta.nomad.client.results.LoggingResultReceiver;
import org.terracotta.nomad.client.results.NomadFailureReceiver;
import org.terracotta.nomad.client.status.MultiDiscoveryResultReceiver;
import org.terracotta.nomad.server.ChangeRequestState;
import org.terracotta.nomad.server.NomadException;
import org.terracotta.nomad.server.UncheckedNomadException;
import org.terracotta.server.Server;
import org.terracotta.server.ServerJMX;
import org.terracotta.testing.Retry;
import org.terracotta.testing.TmpDir;
import static org.terracotta.utilities.io.Files.ExtendedOption.RECURSIVE;

@RunWith(Parameterized.class)
public class DesynchronizedNomadConfigTest {

  @Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return asList(new Object[][]{
        {"v1-config"},
        {"v1-config-migrated"},
        {"v2-concurrent-tx-rolled-back"},
        {"v2-config"},
        {"v2-config-10.7.0.0.315"},
    });
  }

  @Rule
  public TmpDir temporaryFolder = new TmpDir(Paths.get(System.getProperty("user.dir"), "build"), true);

  @Parameter
  public String rootName;

  @Test
  public void test_sync() throws NomadException {
    Path root = copy(rootName);
    try (FakeNode active = FakeNode.create(root.resolve("node1").resolve("config"));
         FakeNode passive = FakeNode.create(root.resolve("node2").resolve("config"));
         NomadClient<NodeContext> nomadClient = createNomadClient(0, active, passive)) {
      DynamicConfigSyncData syncData = active.sync.getSyncData();
      passive.sync.sync(syncData);
      runNormalChange(nomadClient);
    }
  }

  // This test is showing the issue that can happen when a node starts at the same time a nomad change is forced and prepared on the active,
  // just at the same time (or a little after) it is sending its config to the passive that is syncing.
  // A DC changes requires all nodes to be up in order to avoid such situation.
  @Test
  public void test_concurrent_sync_and_change() throws NomadException {
    Path root = copy(rootName);
    try (FakeNode active = FakeNode.create(root.resolve("node1").resolve("config"));
         FakeNode passive = FakeNode.create(root.resolve("node2").resolve("config"))) {
      // 1. active starts sending changed to passive
      DynamicConfigSyncData syncData = active.sync.getSyncData();

      // 2. at the same time it accepts a change
      try (NomadClient<NodeContext> nomadClient = createNomadClient(active)) {
        runNormalChange(nomadClient, "2GB"); // currently succeeds, but should fail
      }

      // 3. passive syncs the previously sent changes in 1
      passive.sync.sync(syncData);

      // 3. active server has preparing (and also eventually committed) the change while passive is syncing
      assertThat(active.manager.getConfiguration().get().getCluster().getOffheapResources().orDefault().get("main"), is(equalTo(Measure.of(2, MemoryUnit.GB))));

      // 4. passive server has synced the changes, but the last one is missing
      assertThat(passive.manager.getConfiguration().get().getCluster().getOffheapResources().orDefault().get("main"), is(equalTo(Measure.of(512, MemoryUnit.MB))));

      // 4. we now have a cluster with a partitioned config.
      try (NomadClient<NodeContext> nomadClient = createNomadClient(1, active, passive)) {
        runNormalChange(nomadClient, "3GB"); // should succeed, but currently fails
        fail();
      } catch (IllegalStateException e) {
        /* fails with:
        java.lang.IllegalStateException: Two-Phase commit failed with 2 messages(s):
(1) UNRECOVERABLE: Partitioned configuration on cluster. Subsets: [[host1:9410], [host2:9412]]
(2) Please run the 'diagnostic' command to diagnose the configuration state and please seek support. The cluster is inconsistent or partitioned and cannot be trivially recovered.
        */
        assertThat(e.getMessage(), containsString("UNRECOVERABLE: Partitioned configuration on cluster"));
      }

      // If this issue is caught in time, the easier way to repair it is to restart the passive.
      // But if this issue is not caught in time, and worse, the passive becomes active, then the last change is lost and the cluster might run into issues

      // Let's simulate a passive restarts (new passive sync)
      syncData = active.sync.getSyncData();
      passive.sync.sync(syncData);

      // now both servers are OK
      assertThat(active.manager.getConfiguration().get().getCluster().getOffheapResources().orDefault().get("main"), is(equalTo(Measure.of(2, MemoryUnit.GB))));
      assertThat(passive.manager.getConfiguration().get().getCluster().getOffheapResources().orDefault().get("main"), is(equalTo(Measure.of(2, MemoryUnit.GB))));

      // and a change can be made
      try (NomadClient<NodeContext> nomadClient = createNomadClient(1, active, passive)) {
        runNormalChange(nomadClient, "3GB");
      }

      assertThat(active.manager.getConfiguration().get().getCluster().getOffheapResources().orDefault().get("main"), is(equalTo(Measure.of(3, MemoryUnit.GB))));
      assertThat(passive.manager.getConfiguration().get().getCluster().getOffheapResources().orDefault().get("main"), is(equalTo(Measure.of(3, MemoryUnit.GB))));
    }
  }

  @Test
  public void test_restricted_activation() throws NomadException {
    // This test assumes that we have a stripe with 2 nodes: 1 active and  1 passive
    // but "v2-concurrent-tx-rolled-back" is a cluster with 2 stripe 1 node each.
    assumeThat(rootName, is(not(equalTo("v2-concurrent-tx-rolled-back"))));

    Path root = copy(rootName);

    // reset the node
    try (FakeNode passive = FakeNode.create(root.resolve("node2").resolve("config"))) {
      passive.reset();
    }

    try (FakeNode active = FakeNode.create(root.resolve("node1").resolve("config"));
         NomadClient<NodeContext> activeNomadClient = active.createNomadClient()) {

      runNormalChange(activeNomadClient);
      assertThat(active.nomad.getChangeHistory().size(), greaterThanOrEqualTo(2));

      // grab last topology
      Cluster lastTopology = active.nomad.discover().getLatestChange().getResult().getCluster();
      Node passiveDetails = lastTopology.getSingleStripe().get().getNodes().get(1);

      // activate the un-configured node
      try (FakeNode passive = FakeNode.create(root.resolve("node2").resolve("config"), passiveDetails);
           NomadClient<NodeContext> passiveNomadClient = passive.createNomadClient()) {
        passive.manager.getDynamicConfigService().setUpcomingCluster(lastTopology);
        passive.manager.getDynamicConfigService().activate(lastTopology, null);

        NomadFailureReceiver<NodeContext> failureRecorder = new NomadFailureReceiver<>();
        passiveNomadClient.tryApplyChange(failureRecorder, new ClusterActivationNomadChange(lastTopology));
        failureRecorder.reThrowErrors();

        assertThat(passive.nomad.getChangeHistory(), hasSize(1));
      }

      // force sync the passive to active
      try (FakeNode passive = FakeNode.create(root.resolve("node2").resolve("config"), passiveDetails)) {
        passive.sync.sync(active.sync.getSyncData());
        assertThat(passive.nomad.getChangeHistory().size(), greaterThanOrEqualTo(2)); // the changes before any upgrade are not synced
      }

      // run a normal change across the cluster
      try (FakeNode passive = FakeNode.create(root.resolve("node2").resolve("config"), passiveDetails);
           NomadClient<NodeContext> clusterNomadClient = createNomadClient(0, active, passive)) {
        NomadFailureReceiver<NodeContext> failureRecorder = new NomadFailureReceiver<>();
        clusterNomadClient.tryApplyChange(failureRecorder, SettingNomadChange.set(Applicability.cluster(), OFFHEAP_RESOURCES, "main", "3GB"));
        failureRecorder.reThrowErrors();
        assertThat(active.nomad.getChangeHistory().size(), greaterThanOrEqualTo(3));
        assertThat(active.nomad.getChangeHistory().size(), greaterThanOrEqualTo(3));
      }

      // nothing to sync next time
      try (FakeNode passive = FakeNode.create(root.resolve("node2").resolve("config"), passiveDetails)) {
        assertThat(passive.sync.sync(active.sync.getSyncData()), hasItem(NOTHING));
      }
    }
  }

  @Test
  public void test_nomad_tx_committed() {
    Path root = copy(rootName);
    try (FakeNode active = FakeNode.create(root.resolve("node1").resolve("config"));
         FakeNode passive = FakeNode.create(root.resolve("node2").resolve("config"));
         NomadClient<NodeContext> nomadClient = createNomadClient(0, active, passive)) {
      runNormalChange(nomadClient);
    }
  }

  @Test
  public void test_nomad_tx_rolled_back() {
    Path root = copy(rootName);
    try (FakeNode active = FakeNode.create(root.resolve("node1").resolve("config"));
         FakeNode passive = FakeNode.create(root.resolve("node2").resolve("config"));
         NomadClient<NodeContext> nomadClient = createNomadClient(0, active, passive)) {

      NomadFailureReceiver<NodeContext> failureRecorder = new NomadFailureReceiver<>();
      nomadClient.tryApplyChange(failureRecorder, SettingNomadChange.set(Applicability.cluster(), OFFHEAP_RESOURCES, "main", "64MB"));
      assertThat(failureRecorder.getReasons(), hasItems(
          "Prepare rejected for node " + active.getHostPort() + ". Reason: 'set offheap-resources.main=64MB': New offheap-resource size: 64MB should be larger than the old size: 512MB",
          "Prepare rejected for node " + passive.getHostPort() + ". Reason: 'set offheap-resources.main=64MB': New offheap-resource size: 64MB should be larger than the old size: 512MB"
      ));

      runNormalChange(nomadClient);
    }
  }

  @Test
  public void test_diagnostic() {
    Path root = copy(rootName);
    try (FakeNode active = FakeNode.create(root.resolve("node1").resolve("config"));
         FakeNode passive = FakeNode.create(root.resolve("node2").resolve("config"))) {
      ConfigurationConsistencyAnalyzer configurationConsistencyAnalyzer = analyzeConsistency(active, passive);
      assertThat(configurationConsistencyAnalyzer.getState(), is(equalTo(ConfigurationConsistencyState.ALL_ACCEPTING)));
    }
  }

  @Test(timeout = 20 * 1000)
  @Ignore("This test is not deterministic and can fail randomly on slow machines because the timeout can be reached before the concurrent modification")
  public void test_concurrent_prepare_leading_to_reject_and_rollback() throws InterruptedException {
    assumeThat(Runtime.getRuntime().availableProcessors(), is(greaterThanOrEqualTo(4)));
    Retry.untilInterrupted(() -> {
      try {
        org.terracotta.utilities.io.Files.deleteTree(temporaryFolder.getRoot().toAbsolutePath());
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
      runConcurrentNomadTx(4, idx -> SettingNomadChange.set(Applicability.cluster(), OFFHEAP_RESOURCES, "main", "64MB"));
    });
  }

  @Test(timeout = 20 * 1000)
  @Ignore("This test is not deterministic and can fail randomly on slow machines because the timeout can be reached before the concurrent modification")
  public void test_concurrent_prepare_leading_to_accept() throws InterruptedException {
    assumeThat(Runtime.getRuntime().availableProcessors(), is(greaterThanOrEqualTo(4)));
    Retry.untilInterrupted(() -> {
      try {
        org.terracotta.utilities.io.Files.deleteTree(temporaryFolder.getRoot().toAbsolutePath());
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
      AtomicInteger memory = new AtomicInteger();
      runConcurrentNomadTx(4, idx -> SettingNomadChange.set(Applicability.cluster(), OFFHEAP_RESOURCES, "main", (1 + memory.incrementAndGet()) + "GB"));
    });
  }

  private void runConcurrentNomadTx(int concurrency, Function<Integer, NomadChange> changeFactory) {
    Path root = copy(rootName);
    CountDownLatch start = new CountDownLatch(1);
    List<NomadFailureReceiver<NodeContext>> errorRecorders = range(0, concurrency).mapToObj(idx -> new NomadFailureReceiver<NodeContext>()).collect(toList());
    AtomicInteger errors = new AtomicInteger();

    try (FakeNode active = FakeNode.create(root.resolve("node1").resolve("config"));
         FakeNode passive = FakeNode.create(root.resolve("node2").resolve("config"))) {

      List<NomadClient<NodeContext>> nomadClients = range(0, concurrency).mapToObj(idx -> createNomadClient(idx, active, passive)).collect(toList());
      List<Thread> threads = range(0, concurrency).mapToObj(idx -> new Thread(() -> {
        try {
          start.await();
          do {
            NomadFailureReceiver<NodeContext> errorRecorder = new NomadFailureReceiver<>();
            errorRecorders.set(idx, errorRecorder);
            nomadClients.get(idx).tryApplyChange(errorRecorder, changeFactory.apply(idx));
            errors.updateAndGet(prev -> prev + errorRecorder.getCount());
            //errorRecorder.buildErrors().ifPresent(Throwable::printStackTrace);
          } while (!Thread.currentThread().isInterrupted() && errors.get() == 0);
        } catch (InterruptedException ignored) {
        }
      })).peek(Thread::start).collect(toList());

      try {
        start.countDown();
        for (Thread thread : threads) {
          try {
            thread.join();
          } catch (InterruptedException ignored) {
            thread.interrupt();
            try {
              thread.join();
            } catch (InterruptedException ignored2) {
            }
          }
          assertFalse(thread.isInterrupted());
        }

        assertFalse(Thread.currentThread().isInterrupted());
        assertThat(errors.get(), is(greaterThan(0)));
        assertThat(errorRecorders.stream().filter(r -> !r.isEmpty()).findFirst().get().getReasons(), hasItem(containsString("Another process running on")));
        assertNomadClientsHaveRolledBack(nomadClients, active, passive);

        runNormalChange(nomadClients.get(0));
      } finally {
        nomadClients.forEach(NomadClient::close);
        nomadClients.forEach(NomadClient::close);
      }
    }
  }

  private void assertNomadClientsHaveRolledBack(List<NomadClient<NodeContext>> nomadClients, FakeNode active, FakeNode passive) {
    for (NomadClient<NodeContext> nomadClient : nomadClients) {
      ConfigurationConsistencyAnalyzer configurationConsistencyAnalyzer = analyzeConsistency(active, passive);
      assertThat(configurationConsistencyAnalyzer.getState(), is(equalTo(ConfigurationConsistencyState.ALL_ACCEPTING)));

      // all tx should be rolled back
      Stream.of(active.getHostPort(), passive.getHostPort())
          .map(addr -> configurationConsistencyAnalyzer.getDiscoveryResponse(addr).get())
          .forEach(discoverResponse -> {
            assertThat(discoverResponse.getLatestChange().getState(), is(ChangeRequestState.ROLLED_BACK));
          });

      // last committed change is the same (but eventually different change uuids)
      Set<String> hash = Stream.of(active.getHostPort(), passive.getHostPort())
          .map(addr -> configurationConsistencyAnalyzer.getDiscoveryResponse(addr).get())
          .map(discoverResponse -> discoverResponse.getLatestCommittedChange().getChangeResultHash())
          .collect(Collectors.toSet());
      assertThat(hash, hasSize(1));
    }
  }

  private Path copy(String root) {
    try {
      Path src = Paths.get(getClass().getResource("/" + root).toURI());
      Path dst = temporaryFolder.getRoot().resolve(root);
      org.terracotta.utilities.io.Files.copy(src, dst, RECURSIVE);
      Files.createDirectories(dst.resolve(Paths.get("node1", "config", "license")));
      Files.createDirectories(dst.resolve(Paths.get("node2", "config", "license")));
      return dst;
    } catch (URISyntaxException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  private NomadClient<NodeContext> createNomadClient(FakeNode node) {
    List<NomadEndpoint<NodeContext>> endpoints = singletonList(node.getEndpoint());
    NomadEnvironment environment = new NomadEnvironment();
    return new NomadClient<>(endpoints, environment.getHost(), environment.getUser(), Clock.systemUTC());
  }

  private NomadClient<NodeContext> createNomadClient(int idx, FakeNode active, FakeNode passive) {
    List<NomadEndpoint<NodeContext>> endpoints = idx == 0 ?
        asList(active.getEndpoint(), passive.getEndpoint()) :
        asList(passive.getEndpoint(), active.getEndpoint());
    NomadEnvironment environment = new NomadEnvironment();
    return new NomadClient<>(endpoints, environment.getHost(), environment.getUser(), Clock.systemUTC());
  }

  private ConfigurationConsistencyAnalyzer analyzeConsistency(FakeNode active, FakeNode passive) {
    Map<HostPort, LogicalServerState> addresses = Stream.of(
        new SimpleEntry<>(active.getHostPort(), LogicalServerState.ACTIVE),
        new SimpleEntry<>(passive.getHostPort(), LogicalServerState.PASSIVE)
    ).collect(toMap(SimpleEntry::getKey, SimpleEntry::getValue));
    ConfigurationConsistencyAnalyzer configurationConsistencyAnalyzer = new ConfigurationConsistencyAnalyzer(addresses);
    try (NomadClient<NodeContext> nomadClient = createNomadClient(0, active, passive)) {
      nomadClient.tryDiscovery(new MultiDiscoveryResultReceiver<>(asList(new LoggingResultReceiver<>(), configurationConsistencyAnalyzer)));
    }
    return configurationConsistencyAnalyzer;
  }

  private static void runNormalChange(NomadClient<NodeContext> nomadClient, String offheapValue) {
    NomadFailureReceiver<NodeContext> failureRecorder = new NomadFailureReceiver<>();
    nomadClient.tryApplyChange(failureRecorder, SettingNomadChange.set(Applicability.cluster(), OFFHEAP_RESOURCES, "main", offheapValue));
    failureRecorder.reThrowErrors();
  }

  private static void runNormalChange(NomadClient<NodeContext> nomadClient) {
    runNormalChange(nomadClient, "2GB");
  }

  static class FakeNode implements Closeable {

    private final DynamicConfigNomadServer nomad;
    private final DynamicConfigurationPassiveSync sync;
    private final NomadServerManager manager;
    private final NodeContext alternateConfig;

    FakeNode(DynamicConfigNomadServer nomad, DynamicConfigurationPassiveSync sync, NomadServerManager manager, NodeContext alternateConfig) {
      this.nomad = nomad;
      this.sync = sync;
      this.manager = manager;
      this.alternateConfig = alternateConfig;
    }

    @Override
    public void close() {
      nomad.close();
    }

    HostPort getHostPort() {
      try {
        return nomad.getCurrentCommittedConfig().map(c -> c.getNode().getInternalHostPort()).orElse(alternateConfig.getNode().getInternalHostPort());
      } catch (NomadException e) {
        throw new UncheckedNomadException(e);
      }
    }

    NomadEndpoint<NodeContext> getEndpoint() {
      return new NomadEndpoint<>(getHostPort(), nomad);
    }

    NomadClient<NodeContext> createNomadClient() {
      List<NomadEndpoint<NodeContext>> endpoints = singletonList(getEndpoint());
      NomadEnvironment environment = new NomadEnvironment();
      return new NomadClient<>(endpoints, environment.getHost(), environment.getUser(), Clock.systemUTC());
    }

    void reset() throws NomadException {
      nomad.reset();
      manager.setNomad(NomadMode.RO);
    }

    /**
     * if node is configured
     */
    static FakeNode create(Path configDir) {
      return create(configDir, null);
    }

    /**
     * Simulate the wiring of real service like it is done in the configuration provider
     */
    static FakeNode create(Path configDir, Node alternateNode) {
      // initialize services
      IParameterSubstitutor parameterSubstitutor = new ParameterSubstitutor();
      ConfigChangeHandlerManager configChangeHandlerManager = new ConfigChangeHandlerManagerImpl();
      Json.Factory jsonFactory = new DefaultJsonFactory().withModule(new DynamicConfigJsonModule());
      LicenseService licenseService = new LicenseParserDiscovery(FakeNode.class.getClassLoader()).find().orElse(LicenseService.UNSUPPORTED);
      Server server = mock(Server.class);
      ServerJMX serverJMX = mock(ServerJMX.class);
      when(serverJMX.getMBeanServer()).thenReturn(MBeanServerFactory.newMBeanServer());
      when(server.getManagement()).thenReturn(serverJMX);
      NomadServerManager nomadServerManager = new NomadServerManager(parameterSubstitutor, configChangeHandlerManager, licenseService, jsonFactory, server);

      // add an off-heap change handler
      configChangeHandlerManager.set(Setting.OFFHEAP_RESOURCES, new ConfigChangeHandler() {
        @Override
        public void validate(NodeContext baseConfig, Configuration change) throws InvalidConfigChangeException {
          if (!change.hasValue()) {
            throw new InvalidConfigChangeException("Operation not supported"); //unset not supported
          }
          try {
            Measure<MemoryUnit> measure = Measure.parse(change.getValue().get(), MemoryUnit.class);
            String name = change.getKey();
            long newValue = measure.getQuantity(MemoryUnit.B);
            Measure<MemoryUnit> existing = baseConfig.getCluster().getOffheapResources().orDefault().get(name);
            if (existing != null) {
              if (newValue <= existing.getQuantity(MemoryUnit.B)) {
                throw new InvalidConfigChangeException("New offheap-resource size: " + change.getValue().get() +
                    " should be larger than the old size: " + existing);
              }
            }
            Cluster updatedCluster = baseConfig.getCluster();
            change.apply(updatedCluster);
          } catch (RuntimeException e) {
            throw new InvalidConfigChangeException(e.toString(), e);
          }
        }
      });

      // simulate some empty CLI options (except for failover)
      Map<Setting, String> cliOptions = new LinkedHashMap<>();
      cliOptions.putIfAbsent(FAILOVER_PRIORITY, FailoverPriority.availability().toString());
      if (alternateNode != null) {
        cliOptions.putIfAbsent(NODE_HOSTNAME, alternateNode.getHostname());
        cliOptions.putIfAbsent(NODE_PORT, String.valueOf(alternateNode.getPort().orDefault()));
        cliOptions.putIfAbsent(NODE_NAME, String.valueOf(alternateNode.getName()));
      }
      ClusterFactory clusterFactory = new ClusterFactory(Version.CURRENT);
      Cluster cluster = clusterFactory.create(cliOptions, parameterSubstitutor);
      NodeContext alternateConfig = new NodeContext(cluster, cluster.getSingleNode().get().getUID());

      // determine node name from the config dir
      String nodeName = NomadConfigurationManager.findNodeName(configDir, parameterSubstitutor).orElse(null);

      // initialize nomad
      nomadServerManager.reload(configDir, nodeName == null ? alternateConfig.getNode().getName() : nodeName, alternateConfig);

      // get the initialized DC services
      DynamicConfigNomadServer nomadServer = nomadServerManager.getNomadServer();
      DynamicConfigService dynamicConfigService = nomadServerManager.getDynamicConfigService();
      TopologyService topologyService = nomadServerManager.getTopologyService();

      // activate the node
      if (nodeName != null) {
        dynamicConfigService.activate(topologyService.getUpcomingNodeContext().getCluster(), dynamicConfigService.getLicenseContent().orElse(null));
      }

      // create the sync service
      DynamicConfigurationPassiveSync sync = new DynamicConfigurationPassiveSync(
          nomadServerManager.getConfiguration().orElse(null),
          nomadServer,
          dynamicConfigService,
          topologyService, () -> dynamicConfigService.getLicenseContent().orElse(null));

      return new FakeNode(nomadServer, sync, nomadServerManager, alternateConfig);
    }
  }
}
