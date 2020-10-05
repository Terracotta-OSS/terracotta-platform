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
package org.terracotta.dynamic_config.server.configuration.service;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.terracotta.common.struct.Measure;
import org.terracotta.common.struct.MemoryUnit;
import org.terracotta.diagnostic.model.LogicalServerState;
import org.terracotta.dynamic_config.api.json.DynamicConfigApiJsonModule;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Configuration;
import org.terracotta.dynamic_config.api.model.FailoverPriority;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Setting;
import org.terracotta.dynamic_config.api.model.Version;
import org.terracotta.dynamic_config.api.model.nomad.Applicability;
import org.terracotta.dynamic_config.api.model.nomad.SettingNomadChange;
import org.terracotta.dynamic_config.api.service.ClusterFactory;
import org.terracotta.dynamic_config.api.service.ConsistencyAnalyzer;
import org.terracotta.dynamic_config.api.service.DynamicConfigService;
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.server.api.ConfigChangeHandler;
import org.terracotta.dynamic_config.server.api.ConfigChangeHandlerManager;
import org.terracotta.dynamic_config.server.api.DynamicConfigNomadServer;
import org.terracotta.dynamic_config.server.api.InvalidConfigChangeException;
import org.terracotta.dynamic_config.server.api.LicenseParserDiscovery;
import org.terracotta.dynamic_config.server.api.LicenseService;
import org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadConfigurationManager;
import org.terracotta.dynamic_config.server.configuration.sync.DynamicConfigSyncData;
import org.terracotta.dynamic_config.server.configuration.sync.DynamicConfigurationPassiveSync;
import org.terracotta.json.ObjectMapperFactory;
import org.terracotta.nomad.NomadEnvironment;
import org.terracotta.nomad.client.NomadClient;
import org.terracotta.nomad.client.NomadEndpoint;
import org.terracotta.nomad.client.results.LoggingResultReceiver;
import org.terracotta.nomad.client.results.NomadFailureReceiver;
import org.terracotta.nomad.client.status.MultiDiscoveryResultReceiver;
import org.terracotta.nomad.server.NomadException;
import org.terracotta.server.Server;
import org.terracotta.testing.TmpDir;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.IntStream.range;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.terracotta.dynamic_config.api.model.Setting.FAILOVER_PRIORITY;
import static org.terracotta.dynamic_config.api.model.Setting.OFFHEAP_RESOURCES;
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
    });
  }

  @Rule
  public TmpDir temporaryFolder = new TmpDir(Paths.get(System.getProperty("user.dir"), "target"), false);

  @Parameter
  public String rootName;

  @Test
  public void test_sync() throws NomadException {
    Path root = copy(rootName);
    try (FakeNode active = FakeNode.create(root.resolve("node1").resolve("config")); FakeNode passive = FakeNode.create(root.resolve("node2").resolve("config"))) {
      DynamicConfigSyncData syncData = active.sync.getSyncData();
      passive.sync.sync(syncData);
    }
  }

  @Test
  public void test_nomad_tx() {
    Path root = copy(rootName);
    try (FakeNode active = FakeNode.create(root.resolve("node1").resolve("config"));
         FakeNode passive = FakeNode.create(root.resolve("node2").resolve("config"));
         NomadClient<NodeContext> nomadClient = createNomadClient(0, active, passive)) {
      NomadFailureReceiver<NodeContext> failureRecorder = new NomadFailureReceiver<>();
      nomadClient.tryApplyChange(failureRecorder, SettingNomadChange.set(Applicability.cluster(), OFFHEAP_RESOURCES, "main", "1GB"));
      failureRecorder.reThrowErrors();
    }
  }

  @Test
  public void test_diagnostic() {
    Path root = copy(rootName);
    try (FakeNode active = FakeNode.create(root.resolve("node1").resolve("config"));
         FakeNode passive = FakeNode.create(root.resolve("node2").resolve("config"));
         NomadClient<NodeContext> nomadClient = createNomadClient(0, active, passive)) {
      ConsistencyAnalyzer consistencyAnalyzer = analyzeConsistency(nomadClient);
      assertThat(consistencyAnalyzer.getGlobalState(), is(equalTo(ConsistencyAnalyzer.GlobalState.ACCEPTING)));
    }
  }

  @Test(timeout = 2 * 60 * 1000)
  public void test_concurrent_prepare_leading_to_reject_and_rollback() {
    final int concurrency = 2;

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
            nomadClients.get(idx).tryApplyChange(errorRecorders.get(idx), SettingNomadChange.set(Applicability.cluster(), OFFHEAP_RESOURCES, "main", "64MB"));
            errors.updateAndGet(prev -> prev + errorRecorders.get(idx).getCount());
          }
          while (!Thread.currentThread().isInterrupted() && errors.get() == 0);
        } catch (InterruptedException ignored) {
        }
      })).peek(Thread::start).collect(toList());

      try {
        start.countDown();
        for (Thread thread : threads) {
          thread.join();
        }

        assertThat(errors.get(), is(greaterThan(0)));
        assertThat(errorRecorders.stream().filter(r -> !r.isEmpty()).findFirst().get().getReasons(), hasItem(containsString("Another process running on")));

        for (NomadClient<NodeContext> nomadClient : nomadClients) {
          ConsistencyAnalyzer consistencyAnalyzer = analyzeConsistency(nomadClient);
          assertThat(consistencyAnalyzer.getGlobalState(), is(equalTo(ConsistencyAnalyzer.GlobalState.ACCEPTING)));
        }
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      } finally {
        nomadClients.forEach(NomadClient::close);
        nomadClients.forEach(NomadClient::close);
      }
    }
  }

  @Test(timeout = 2 * 60 * 1000)
  public void test_concurrent_prepare_leading_to_accept() {
    final int concurrency = 2;

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
            nomadClients.get(idx).tryApplyChange(errorRecorders.get(idx), SettingNomadChange.set(Applicability.cluster(), OFFHEAP_RESOURCES, "main", (1 + idx) + "GB"));
            errors.updateAndGet(prev -> prev + errorRecorders.get(idx).getCount());
          }
          while (!Thread.currentThread().isInterrupted() && errors.get() == 0);
        } catch (InterruptedException ignored) {
        }
      })).peek(Thread::start).collect(toList());

      try {
        start.countDown();
        for (Thread thread : threads) {
          thread.join();
        }

        assertThat(errors.get(), is(greaterThan(0)));
        assertThat(errorRecorders.stream().filter(r -> !r.isEmpty()).findFirst().get().getReasons(), hasItem(containsString("Another process running on")));

        for (NomadClient<NodeContext> nomadClient : nomadClients) {
          ConsistencyAnalyzer consistencyAnalyzer = analyzeConsistency(nomadClient);
          assertThat(consistencyAnalyzer.getGlobalState(), is(equalTo(ConsistencyAnalyzer.GlobalState.ACCEPTING)));
        }
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      } finally {
        nomadClients.forEach(NomadClient::close);
        nomadClients.forEach(NomadClient::close);
      }
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

  private NomadClient<NodeContext> createNomadClient(int idx, FakeNode active, FakeNode passive) {
    List<NomadEndpoint<NodeContext>> endpoints = idx == 0 ? Arrays.asList(
        new NomadEndpoint<>(InetSocketAddress.createUnresolved("host1", 9410), active.nomadServer),
        new NomadEndpoint<>(InetSocketAddress.createUnresolved("host2", 9412), passive.nomadServer)
    ) : Arrays.asList(
        new NomadEndpoint<>(InetSocketAddress.createUnresolved("host2", 9412), passive.nomadServer),
        new NomadEndpoint<>(InetSocketAddress.createUnresolved("host1", 9410), active.nomadServer)
    );
    NomadEnvironment environment = new NomadEnvironment();
    return new NomadClient<>(endpoints, environment.getHost(), environment.getUser(), Clock.systemUTC());
  }

  private static ConsistencyAnalyzer analyzeConsistency(NomadClient<NodeContext> nomadClient) {
    Map<InetSocketAddress, LogicalServerState> addresses = Stream.of(
        new SimpleEntry<>(InetSocketAddress.createUnresolved("host1", 9410), LogicalServerState.ACTIVE),
        new SimpleEntry<>(InetSocketAddress.createUnresolved("host2", 9412), LogicalServerState.PASSIVE)
    ).collect(toMap(SimpleEntry::getKey, SimpleEntry::getValue));
    ConsistencyAnalyzer consistencyAnalyzer = new ConsistencyAnalyzer(addresses);
    nomadClient.tryDiscovery(new MultiDiscoveryResultReceiver<>(asList(new LoggingResultReceiver<>(), consistencyAnalyzer)));
    return consistencyAnalyzer;
  }

  static class FakeNode implements Closeable {

    private final DynamicConfigNomadServer nomadServer;
    private final DynamicConfigurationPassiveSync sync;

    FakeNode(DynamicConfigNomadServer nomadServer, DynamicConfigurationPassiveSync sync) {
      this.nomadServer = nomadServer;
      this.sync = sync;
    }

    @Override
    public void close() {
      nomadServer.close();
    }

    /**
     * Simulate the wiring of real service like it is done in the configuration provider
     */
    static FakeNode create(Path configDir) {
      // initialize services
      IParameterSubstitutor parameterSubstitutor = new ParameterSubstitutor();
      ConfigChangeHandlerManager configChangeHandlerManager = new ConfigChangeHandlerManagerImpl();
      ObjectMapperFactory objectMapperFactory = new ObjectMapperFactory().withModule(new DynamicConfigApiJsonModule());
      LicenseService licenseService = new LicenseParserDiscovery(FakeNode.class.getClassLoader()).find().orElseGet(LicenseService::unsupported);
      NomadServerManager nomadServerManager = new NomadServerManager(parameterSubstitutor, configChangeHandlerManager, licenseService, objectMapperFactory, mock(Server.class));

      // add an off-heap change handler
      configChangeHandlerManager.set(Setting.OFFHEAP_RESOURCES, new ConfigChangeHandler() {
        @Override
        public void validate(NodeContext baseConfig, Configuration change) throws InvalidConfigChangeException {
          if (!change.getValue().isPresent()) {
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
      ClusterFactory clusterFactory = new ClusterFactory(Version.CURRENT);
      Cluster cluster = clusterFactory.create(cliOptions, parameterSubstitutor);
      NodeContext alternate = new NodeContext(cluster, cluster.getSingleNode().get().getUID());

      // determine node name from the config dir
      String nodeName = NomadConfigurationManager.findNodeName(configDir, parameterSubstitutor).get();

      // initialize nomad
      nomadServerManager.init(configDir, nodeName, alternate);

      // get the initialized DC services
      DynamicConfigNomadServer nomadServer = nomadServerManager.getNomadServer();
      DynamicConfigService dynamicConfigService = nomadServerManager.getDynamicConfigService();
      TopologyService topologyService = nomadServerManager.getTopologyService();

      // activate the node
      dynamicConfigService.activate(topologyService.getUpcomingNodeContext().getCluster(), dynamicConfigService.getLicenseContent().orElse(null));

      // create the sync service
      DynamicConfigurationPassiveSync sync = new DynamicConfigurationPassiveSync(
          nomadServerManager.getConfiguration().orElse(null),
          nomadServer,
          dynamicConfigService,
          topologyService, () -> dynamicConfigService.getLicenseContent().orElse(null));

      return new FakeNode(nomadServer, sync);
    }
  }
}
