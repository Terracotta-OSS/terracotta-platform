/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package org.terracotta.management.entity.sample;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.Timeout;
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionFactory;
import org.terracotta.connection.ConnectionPropertyNames;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.Testing;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.entity.BasicServiceConfiguration;
import org.terracotta.entity.PlatformConfiguration;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderConfiguration;
import org.terracotta.json.DefaultJsonFactory;
import org.terracotta.json.Json;
import org.terracotta.management.entity.nms.NmsConfig;
import org.terracotta.management.entity.nms.agent.client.NmsAgentEntityClientService;
import org.terracotta.management.entity.nms.agent.server.NmsAgentEntityServerService;
import org.terracotta.management.entity.nms.client.DefaultNmsService;
import org.terracotta.management.entity.nms.client.NmsEntity;
import org.terracotta.management.entity.nms.client.NmsEntityClientService;
import org.terracotta.management.entity.nms.client.NmsEntityFactory;
import org.terracotta.management.entity.nms.client.NmsService;
import org.terracotta.management.entity.nms.server.NmsEntityServerService;
import org.terracotta.management.entity.sample.client.CacheEntityClientService;
import org.terracotta.management.entity.sample.client.CacheFactory;
import org.terracotta.management.entity.sample.json.TestModule;
import org.terracotta.management.entity.sample.server.CacheEntityServerService;
import org.terracotta.offheapresource.OffHeapResourcesProvider;
import org.terracotta.passthrough.PassthroughClusterControl;
import org.terracotta.passthrough.PassthroughTestHelpers;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singleton;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.terracotta.common.struct.Measure;
import static org.terracotta.dynamic_config.api.model.Testing.N_UIDS;
import static org.terracotta.dynamic_config.api.model.Testing.newTestCluster;

/**
 * @author Mathieu Carbou
 */
public abstract class AbstractTest {

  private final Json json = new DefaultJsonFactory().withModule(new TestModule()).pretty().create();
  protected final PassthroughClusterControl stripeControl;

  private Connection managementConnection;

  protected final List<CacheFactory> webappNodes = new ArrayList<>();
  protected final Map<String, List<Cache>> caches = new HashMap<>();
  protected NmsService nmsService;

  @Rule
  public Timeout timeout = Timeout.seconds(90);

  protected AbstractTest() {
    this(0);
  }

  protected AbstractTest(int nPassives) {
    TopologyServiceServiceProvider topologyServiceServiceProvider = new TopologyServiceServiceProvider();

    stripeControl = PassthroughTestHelpers.createMultiServerStripe("stripe-1", nPassives + 1, server -> {
      server.registerClientEntityService(new CacheEntityClientService());
      server.registerServerEntityService(new CacheEntityServerService());

      server.registerClientEntityService(new NmsAgentEntityClientService());
      server.registerServerEntityService(new NmsAgentEntityServerService());

      server.registerClientEntityService(new NmsEntityClientService());
      server.registerServerEntityService(new NmsEntityServerService());

      Map<String, Measure<org.terracotta.common.struct.MemoryUnit>> resources = new HashMap<>();
      resources.put("primary-server-resource", Measure.of(32, org.terracotta.common.struct.MemoryUnit.MB));
      server.registerExtendedConfiguration(new OffHeapResourcesProvider(resources));

      server.registerExtendedConfiguration(topologyServiceServiceProvider.getService(0, new BasicServiceConfiguration<>(TopologyService.class)));
      server.registerServiceProvider(topologyServiceServiceProvider, null);
    });

    try {
      stripeControl.waitForActive();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    // only keep 1 active running by default
    for (int i = 0; i < nPassives; i++) {
      stripeControl.terminateOnePassive();
    }
  }

  @Before
  public void setUp() throws Exception {
    connectManagementClients();

    addWebappNode();
    addWebappNode();

    getCaches("pets");
    getCaches("clients");
  }

  @After
  public void tearDown() throws Exception {
    closeNodes();
    if (managementConnection != null) {
      managementConnection.close();
    }
    stripeControl.tearDown();
  }

  protected String read(String file) {
    try {
      if (!file.startsWith("/")) {
        file = "/" + file;
      }
      return new String(Files.readAllBytes(Paths.get(getClass().getResource(file).toURI())), UTF_8).replace("\r", "");
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
  protected String toJson(Object o) {
    return json.toString(o);
  }

  protected int size(int nodeIdx, String cacheName) {
    return caches.get(cacheName).get(nodeIdx).size();
  }

  protected String get(int nodeIdx, String cacheName, String key) {
    return caches.get(cacheName).get(nodeIdx).get(key);
  }

  protected void put(int nodeIdx, String cacheName, String key, String value) {
    caches.get(cacheName).get(nodeIdx).put(key, value);
  }

  protected void remove(int nodeIdx, String cacheName, String key) {
    caches.get(cacheName).get(nodeIdx).remove(key);
  }

  protected void closeNodes() {
    webappNodes.forEach(cacheFactory -> {
      try {
        cacheFactory.getConnection().close();
      } catch (IOException ignored) {
      }
    });
  }

  protected void getCaches(String name) {
    caches.put(name, webappNodes.stream().map(cacheFactory -> cacheFactory.getCache(name)).collect(Collectors.toList()));
  }

  protected void addWebappNode() throws Exception {
    CacheFactory cacheFactory = new CacheFactory(nextInstanceId(), URI.create("passthrough://stripe-1:9510"), "pet-clinic");
    cacheFactory.init();
    webappNodes.add(cacheFactory);
  }

  protected final String nextInstanceId() {
    return "instance-" + webappNodes.size();
  }

  private void connectManagementClients() throws Exception {
    // connects to server
    Properties properties = new Properties();
    properties.setProperty(ConnectionPropertyNames.CONNECTION_NAME, getClass().getSimpleName());
    properties.setProperty(ConnectionPropertyNames.CONNECTION_TIMEOUT, "5000");
    this.managementConnection = ConnectionFactory.connect(URI.create("passthrough://stripe-1:9510/"), properties);

    // create a NMS Entity
    NmsEntityFactory nmsEntityFactory = new NmsEntityFactory(managementConnection, getClass().getSimpleName());
    NmsEntity nmsEntity = nmsEntityFactory.retrieveOrCreate(new NmsConfig());
    this.nmsService = new DefaultNmsService(nmsEntity);
    this.nmsService.setOperationTimeout(10, TimeUnit.SECONDS);
  }

  public static class TopologyServiceServiceProvider implements ServiceProvider {

    NodeContext topology = new NodeContext(newTestCluster("my-cluster", new Stripe()
        .setName("stripe[0]")
        .addNode(Testing.newTestNode("bar", "localhost"))), N_UIDS[1]);
    TopologyService topologyService = mock(TopologyService.class);

    public TopologyServiceServiceProvider() {
      when(topologyService.getRuntimeNodeContext()).thenReturn(topology);
    }

    @Override
    public boolean initialize(ServiceProviderConfiguration configuration, PlatformConfiguration platformConfiguration) {
      return true;
    }

    @Override
    public <T> T getService(long consumerID, ServiceConfiguration<T> configuration) {
      if (configuration.getServiceType() == TopologyService.class) {
        return configuration.getServiceType().cast(topologyService);
      }
      return null;
    }

    @Override
    public Collection<Class<?>> getProvidedServiceTypes() {
      return singleton(TopologyService.class);
    }

    @Override
    public void prepareForSynchronization() {
    }
  }
}
