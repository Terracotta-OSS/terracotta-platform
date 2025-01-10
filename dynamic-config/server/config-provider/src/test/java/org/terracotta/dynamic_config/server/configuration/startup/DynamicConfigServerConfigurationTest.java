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
package org.terracotta.dynamic_config.server.configuration.startup;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.terracotta.configuration.ServerConfiguration;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Setting;
import org.terracotta.dynamic_config.api.service.ClusterFactory;
import org.terracotta.dynamic_config.api.server.GroupPortMapper;
import org.terracotta.dynamic_config.api.server.PathResolver;

import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.terracotta.dynamic_config.api.service.IParameterSubstitutor.identity;

/**
 * @author Mathieu Carbou
 */
@RunWith(Parameterized.class)
public class DynamicConfigServerConfigurationTest {

  @Parameterized.Parameters(name = "{index}: unconfigured={0},terracotta.config.logDir.noDefault={1},cluster={3}")
  public static Collection<Object[]> data() {
    return asList(new Object[][]{
        // default config
        {
            true, // unconfigured ?
            false, // terracotta.config.logDir.noDefault
            (Supplier<Cluster>) () -> makeFromConfig(),
            (BiConsumer<Cluster, ServerConfiguration>) (cluster, configuration) -> {
              assertFalse(cluster.getSingleNode().get().getLogDir().isConfigured());
              assertNotNull(cluster.getSingleNode().get().getLogDir().orDefault());
              assertNull(configuration.getLogsLocation());
            },
        },
        {
            false, // unconfigured ?
            false, // terracotta.config.logDir.noDefault
            (Supplier<Cluster>) () -> makeFromConfig(),
            (BiConsumer<Cluster, ServerConfiguration>) (cluster, configuration) -> {
              assertFalse(cluster.getSingleNode().get().getLogDir().isConfigured());
              assertNotNull(cluster.getSingleNode().get().getLogDir().orDefault());
              assertNotNull(configuration.getLogsLocation());
            },
        },
        {
            true, // unconfigured ?
            true, // terracotta.config.logDir.noDefault
            (Supplier<Cluster>) () -> makeFromConfig(),
            (BiConsumer<Cluster, ServerConfiguration>) (cluster, configuration) -> {
              assertFalse(cluster.getSingleNode().get().getLogDir().isConfigured());
              assertNull(cluster.getSingleNode().get().getLogDir().orDefault());
              assertNull(configuration.getLogsLocation());
            },
        },
        {
            false, // unconfigured ?
            true, // terracotta.config.logDir.noDefault
            (Supplier<Cluster>) () -> makeFromConfig(),
            (BiConsumer<Cluster, ServerConfiguration>) (cluster, configuration) -> {
              assertFalse(cluster.getSingleNode().get().getLogDir().isConfigured());
              assertNull(cluster.getSingleNode().get().getLogDir().orDefault());
              assertNull(configuration.getLogsLocation());
            },
        },
        // config: log-dir=
        {
            true, // unconfigured ?
            false, // terracotta.config.logDir.noDefault
            (Supplier<Cluster>) () -> makeFromConfig("stripe.1.node.1.log-dir="),
            (BiConsumer<Cluster, ServerConfiguration>) (cluster, configuration) -> {
              assertFalse(cluster.getSingleNode().get().getLogDir().isConfigured());
              assertNotNull(cluster.getSingleNode().get().getLogDir().orDefault());
              assertNull(configuration.getLogsLocation());
            },
        },
        {
            false, // unconfigured ?
            false, // terracotta.config.logDir.noDefault
            (Supplier<Cluster>) () -> makeFromConfig("stripe.1.node.1.log-dir="),
            (BiConsumer<Cluster, ServerConfiguration>) (cluster, configuration) -> {
              assertFalse(cluster.getSingleNode().get().getLogDir().isConfigured());
              assertNotNull(cluster.getSingleNode().get().getLogDir().orDefault());
              assertNotNull(configuration.getLogsLocation());
            },
        },
        {
            true, // unconfigured ?
            true, // terracotta.config.logDir.noDefault
            (Supplier<Cluster>) () -> makeFromConfig("stripe.1.node.1.log-dir="),
            (BiConsumer<Cluster, ServerConfiguration>) (cluster, configuration) -> {
              assertFalse(cluster.getSingleNode().get().getLogDir().isConfigured());
              assertNull(cluster.getSingleNode().get().getLogDir().orDefault());
              assertNull(configuration.getLogsLocation());
            },
        },
        {
            false, // unconfigured ?
            true, // terracotta.config.logDir.noDefault
            (Supplier<Cluster>) () -> makeFromConfig("stripe.1.node.1.log-dir="),
            (BiConsumer<Cluster, ServerConfiguration>) (cluster, configuration) -> {
              assertFalse(cluster.getSingleNode().get().getLogDir().isConfigured());
              assertNull(cluster.getSingleNode().get().getLogDir().orDefault());
              assertNull(configuration.getLogsLocation());
            },
        },
        // config: log-dir=foo
        {
            true, // unconfigured ?
            false, // terracotta.config.logDir.noDefault
            (Supplier<Cluster>) () -> makeFromConfig("stripe.1.node.1.log-dir=foo"),
            (BiConsumer<Cluster, ServerConfiguration>) (cluster, configuration) -> {
              assertTrue(cluster.getSingleNode().get().getLogDir().isConfigured());
              assertNotNull(cluster.getSingleNode().get().getLogDir().orDefault());
              assertNull(configuration.getLogsLocation());
            },
        },
        {
            false, // unconfigured ?
            false, // terracotta.config.logDir.noDefault
            (Supplier<Cluster>) () -> makeFromConfig("stripe.1.node.1.log-dir=foo"),
            (BiConsumer<Cluster, ServerConfiguration>) (cluster, configuration) -> {
              assertTrue(cluster.getSingleNode().get().getLogDir().isConfigured());
              assertNotNull(cluster.getSingleNode().get().getLogDir().orDefault());
              assertNotNull(configuration.getLogsLocation());
            },
        },
        {
            true, // unconfigured ?
            true, // terracotta.config.logDir.noDefault
            (Supplier<Cluster>) () -> makeFromConfig("stripe.1.node.1.log-dir=foo"),
            (BiConsumer<Cluster, ServerConfiguration>) (cluster, configuration) -> {
              assertTrue(cluster.getSingleNode().get().getLogDir().isConfigured());
              assertNotNull(cluster.getSingleNode().get().getLogDir().orDefault());
              assertNull(configuration.getLogsLocation());
            },
        },
        {
            false, // unconfigured ?
            true, // terracotta.config.logDir.noDefault
            (Supplier<Cluster>) () -> makeFromConfig("stripe.1.node.1.log-dir=foo"),
            (BiConsumer<Cluster, ServerConfiguration>) (cluster, configuration) -> {
              assertTrue(cluster.getSingleNode().get().getLogDir().isConfigured());
              assertNotNull(cluster.getSingleNode().get().getLogDir().orDefault());
              assertNotNull(configuration.getLogsLocation());
            },
        },
        // default CLI
        {
            true, // unconfigured ?
            false, // terracotta.config.logDir.noDefault
            (Supplier<Cluster>) () -> makeFromCLI(),
            (BiConsumer<Cluster, ServerConfiguration>) (cluster, configuration) -> {
              assertFalse(cluster.getSingleNode().get().getLogDir().isConfigured());
              assertNotNull(cluster.getSingleNode().get().getLogDir().orDefault());
              assertNull(configuration.getLogsLocation());
            },
        },
        {
            false, // unconfigured ?
            false, // terracotta.config.logDir.noDefault
            (Supplier<Cluster>) () -> makeFromCLI(),
            (BiConsumer<Cluster, ServerConfiguration>) (cluster, configuration) -> {
              assertFalse(cluster.getSingleNode().get().getLogDir().isConfigured());
              assertNotNull(cluster.getSingleNode().get().getLogDir().orDefault());
              assertNotNull(configuration.getLogsLocation());
            },
        },
        {
            true, // unconfigured ?
            true, // terracotta.config.logDir.noDefault
            (Supplier<Cluster>) () -> makeFromCLI(),
            (BiConsumer<Cluster, ServerConfiguration>) (cluster, configuration) -> {
              assertFalse(cluster.getSingleNode().get().getLogDir().isConfigured());
              assertNull(cluster.getSingleNode().get().getLogDir().orDefault());
              assertNull(configuration.getLogsLocation());
            },
        },
        {
            false, // unconfigured ?
            true, // terracotta.config.logDir.noDefault
            (Supplier<Cluster>) () -> makeFromCLI(),
            (BiConsumer<Cluster, ServerConfiguration>) (cluster, configuration) -> {
              assertFalse(cluster.getSingleNode().get().getLogDir().isConfigured());
              assertNull(cluster.getSingleNode().get().getLogDir().orDefault());
              assertNull(configuration.getLogsLocation());
            },
        },
        // CLI: log-dir=
        {
            true, // unconfigured ?
            false, // terracotta.config.logDir.noDefault
            (Supplier<Cluster>) () -> makeFromCLI("log-dir="),
            (BiConsumer<Cluster, ServerConfiguration>) (cluster, configuration) -> {
              assertFalse(cluster.getSingleNode().get().getLogDir().isConfigured());
              assertNotNull(cluster.getSingleNode().get().getLogDir().orDefault());
              assertNull(configuration.getLogsLocation());
            },
        },
        {
            false, // unconfigured ?
            false, // terracotta.config.logDir.noDefault
            (Supplier<Cluster>) () -> makeFromCLI("log-dir="),
            (BiConsumer<Cluster, ServerConfiguration>) (cluster, configuration) -> {
              assertFalse(cluster.getSingleNode().get().getLogDir().isConfigured());
              assertNotNull(cluster.getSingleNode().get().getLogDir().orDefault());
              assertNotNull(configuration.getLogsLocation());
            },
        },
        {
            true, // unconfigured ?
            true, // terracotta.config.logDir.noDefault
            (Supplier<Cluster>) () -> makeFromCLI("log-dir="),
            (BiConsumer<Cluster, ServerConfiguration>) (cluster, configuration) -> {
              assertFalse(cluster.getSingleNode().get().getLogDir().isConfigured());
              assertNull(cluster.getSingleNode().get().getLogDir().orDefault());
              assertNull(configuration.getLogsLocation());
            },
        },
        {
            false, // unconfigured ?
            true, // terracotta.config.logDir.noDefault
            (Supplier<Cluster>) () -> makeFromCLI("log-dir="),
            (BiConsumer<Cluster, ServerConfiguration>) (cluster, configuration) -> {
              assertFalse(cluster.getSingleNode().get().getLogDir().isConfigured());
              assertNull(cluster.getSingleNode().get().getLogDir().orDefault());
              assertNull(configuration.getLogsLocation());
            },
        },
        // CLI: log-dir=foo
        {
            true, // unconfigured ?
            false, // terracotta.config.logDir.noDefault
            (Supplier<Cluster>) () -> makeFromCLI("log-dir=foo"),
            (BiConsumer<Cluster, ServerConfiguration>) (cluster, configuration) -> {
              assertTrue(cluster.getSingleNode().get().getLogDir().isConfigured());
              assertNotNull(cluster.getSingleNode().get().getLogDir().orDefault());
              assertNull(configuration.getLogsLocation());
            },
        },
        {
            false, // unconfigured ?
            false, // terracotta.config.logDir.noDefault
            (Supplier<Cluster>) () -> makeFromCLI("log-dir=foo"),
            (BiConsumer<Cluster, ServerConfiguration>) (cluster, configuration) -> {
              assertTrue(cluster.getSingleNode().get().getLogDir().isConfigured());
              assertNotNull(cluster.getSingleNode().get().getLogDir().orDefault());
              assertNotNull(configuration.getLogsLocation());
            },
        },
        {
            true, // unconfigured ?
            true, // terracotta.config.logDir.noDefault
            (Supplier<Cluster>) () -> makeFromCLI("log-dir=foo"),
            (BiConsumer<Cluster, ServerConfiguration>) (cluster, configuration) -> {
              assertTrue(cluster.getSingleNode().get().getLogDir().isConfigured());
              assertNotNull(cluster.getSingleNode().get().getLogDir().orDefault());
              assertNull(configuration.getLogsLocation());
            },
        },
        {
            false, // unconfigured ?
            true, // terracotta.config.logDir.noDefault
            (Supplier<Cluster>) () -> makeFromCLI("log-dir=foo"),
            (BiConsumer<Cluster, ServerConfiguration>) (cluster, configuration) -> {
              assertTrue(cluster.getSingleNode().get().getLogDir().isConfigured());
              assertNotNull(cluster.getSingleNode().get().getLogDir().orDefault());
              assertNotNull(configuration.getLogsLocation());
            },
        },
    });
  }


  @Parameter(0)
  public boolean unconfigured;
  @Parameter(1)
  public boolean sysprop;
  @Parameter(2)
  public Supplier<Cluster> clusterSupplier;
  @Parameter(3)
  public BiConsumer<Cluster, ServerConfiguration> assertions;

  private final PathResolver pathResolver = new PathResolver(Paths.get(""));

  @Test
  public void test_getLogsLocation_default() {
    try {
      System.setProperty("terracotta.config.logDir.noDefault", String.valueOf(sysprop));

      Cluster cluster = clusterSupplier.get();
      Node node = cluster.getSingleNode().get();
      DynamicConfigServerConfiguration configuration = new DynamicConfigServerConfiguration(node, () -> new NodeContext(cluster, node.getUID()), identity(), mock(GroupPortMapper.class), pathResolver, unconfigured);

      assertions.accept(cluster, configuration);
    } finally {
      System.clearProperty("terracotta.config.logDir.noDefault");
    }
  }

  private static Cluster makeFromConfig(String... lines) {
    Properties cfg = Stream.concat(
            Stream.of("failover-priority=availability", "stripe.1.node.1.hostname=localhost", "stripe.1.node.1.name=node-1-1"),
            Stream.of(lines))
        .map(s -> s.split("="))
        .reduce(new Properties(), (configFile, kv) -> {
          configFile.setProperty(kv[0], kv.length == 1 ? "" : kv[1]);
          return configFile;
        }, (cfg1, cfg2) -> {
          throw new UnsupportedOperationException();
        });
    cfg.setProperty("failover-priority", "availability");
    return new ClusterFactory().create(cfg);
  }

  private static Cluster makeFromCLI(String... args) {
    Map<Setting, String> cli = Stream.concat(
            Stream.of("failover-priority=availability", "hostname=localhost", "name=node-1-1"),
            Stream.of(args))
        .map(s -> s.split("="))
        .reduce(new HashMap<>(), (map, kv) -> {
          map.put(Setting.fromName(kv[0]), kv.length == 1 ? "" : kv[1]);
          return map;
        }, (cli1, cli2) -> {
          throw new UnsupportedOperationException();
        });
    return new ClusterFactory().create(cli, identity());
  }
}
