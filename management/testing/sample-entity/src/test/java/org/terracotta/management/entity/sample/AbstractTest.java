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
package org.terracotta.management.entity.sample;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.After;
import org.junit.Before;
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionFactory;
import org.terracotta.connection.ConnectionPropertyNames;
import org.terracotta.management.entity.management.client.ManagementAgentEntityClientService;
import org.terracotta.management.entity.management.server.ManagementAgentEntityServerService;
import org.terracotta.management.entity.sample.client.CacheEntityClientService;
import org.terracotta.management.entity.sample.client.CacheFactory;
import org.terracotta.management.entity.sample.server.CacheEntityServerService;
import org.terracotta.management.entity.tms.TmsAgentConfig;
import org.terracotta.management.entity.tms.client.TmsAgentEntity;
import org.terracotta.management.entity.tms.client.TmsAgentEntityClientService;
import org.terracotta.management.entity.tms.client.TmsAgentEntityFactory;
import org.terracotta.management.entity.tms.client.TmsAgentService;
import org.terracotta.management.entity.tms.server.TmsAgentEntityServerService;
import org.terracotta.management.model.capabilities.context.CapabilityContext;
import org.terracotta.management.model.stats.ContextualStatistics;
import org.terracotta.management.registry.collect.StatisticConfiguration;
import org.terracotta.offheapresource.OffHeapResourcesProvider;
import org.terracotta.offheapresource.config.MemoryUnit;
import org.terracotta.offheapresource.config.OffheapResourcesType;
import org.terracotta.offheapresource.config.ResourceType;
import org.terracotta.passthrough.PassthroughClusterControl;
import org.terracotta.passthrough.PassthroughTestHelpers;

import java.io.File;
import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Mathieu Carbou
 */
public abstract class AbstractTest {

  private final ObjectMapper mapper = new ObjectMapper();
  protected final PassthroughClusterControl stripeControl;

  private Connection managementConnection;

  protected final List<CacheFactory> webappNodes = new ArrayList<>();
  protected final Map<String, List<Cache>> caches = new HashMap<>();
  protected TmsAgentService tmsAgentService;

  protected AbstractTest() {
    this(0);
  }

  protected AbstractTest(int nPassives) {
    mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
    mapper.addMixIn(CapabilityContext.class, CapabilityContextMixin.class);

    stripeControl = PassthroughTestHelpers.createMultiServerStripe("stripe-1", nPassives + 1, server -> {
      server.registerClientEntityService(new CacheEntityClientService());
      server.registerServerEntityService(new CacheEntityServerService());

      server.registerClientEntityService(new ManagementAgentEntityClientService());
      server.registerServerEntityService(new ManagementAgentEntityServerService());

      server.registerClientEntityService(new TmsAgentEntityClientService());
      server.registerServerEntityService(new TmsAgentEntityServerService());

      OffheapResourcesType resources = new OffheapResourcesType();
      ResourceType resource = new ResourceType();
      resource.setName("primary-resource");
      resource.setUnit(MemoryUnit.MB);
      resource.setValue(BigInteger.valueOf(32));
      resources.getResource().add(resource);
      server.registerExtendedConfiguration(new OffHeapResourcesProvider(resources));
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

  protected JsonNode readJson(String file) {
    try {
      return mapper.readTree(new File(AbstractTest.class.getResource("/" + file).toURI()));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected JsonNode toJson(Object o) {
    try {
      return mapper.readTree(mapper.writeValueAsString(o));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
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
    webappNodes.forEach(CacheFactory::close);
  }

  protected void getCaches(String name) {
    caches.put(name, webappNodes.stream().map(cacheFactory -> cacheFactory.getCache(name)).collect(Collectors.toList()));
  }

  protected void addWebappNode() throws Exception {
    StatisticConfiguration statisticConfiguration = new StatisticConfiguration()
        .setAverageWindowDuration(1, TimeUnit.MINUTES)
        .setHistorySize(100)
        .setHistoryInterval(1, TimeUnit.SECONDS)
        .setTimeToDisable(5, TimeUnit.SECONDS);
    CacheFactory cacheFactory = new CacheFactory("passthrough://stripe-1:9510/pet-clinic", statisticConfiguration);
    cacheFactory.init();
    webappNodes.add(cacheFactory);
  }

  public static abstract class CapabilityContextMixin {
    @JsonIgnore
    public abstract Collection<String> getRequiredAttributeNames();

    @JsonIgnore
    public abstract Collection<CapabilityContext.Attribute> getRequiredAttributes();
  }

  private void connectManagementClients() throws Exception {
    // connects to server
    Properties properties = new Properties();
    properties.setProperty(ConnectionPropertyNames.CONNECTION_NAME, getClass().getSimpleName());
    properties.setProperty(ConnectionPropertyNames.CONNECTION_TIMEOUT, "10000");
    this.managementConnection = ConnectionFactory.connect(URI.create("passthrough://stripe-1:9510/"), properties);

    // create a tms entity
    TmsAgentEntityFactory tmsAgentEntityFactory = new TmsAgentEntityFactory(managementConnection, getClass().getSimpleName());
    TmsAgentEntity tmsAgentEntity = tmsAgentEntityFactory.retrieveOrCreate(new TmsAgentConfig()
        .setMaximumUnreadMessages(1024 * 1024)
        .setStatisticConfiguration(new StatisticConfiguration()
            .setAverageWindowDuration(1, TimeUnit.MINUTES)
            .setHistorySize(100)
            .setHistoryInterval(1, TimeUnit.SECONDS)
            .setTimeToDisable(5, TimeUnit.SECONDS)));
    this.tmsAgentService = new TmsAgentService(tmsAgentEntity);
    this.tmsAgentService.setOperationTimeout(10, TimeUnit.SECONDS);
  }

  protected void queryAllRemoteStatsUntil(Predicate<List<? extends ContextualStatistics>> test) throws Exception {
    List<? extends ContextualStatistics> statistics;
    do {
      statistics = tmsAgentService.readMessages()
          .stream()
          .filter(message -> message.getType().equals("STATISTICS"))
          .flatMap(message -> message.unwrap(ContextualStatistics.class).stream())
          .collect(Collectors.toList());
      // PLEASE KEEP THIS ! Really useful when troubleshooting stats!
      /*if (!statistics.isEmpty()) {
        System.out.println("received at " + System.currentTimeMillis() + ":");
        statistics.stream()
            .flatMap(o -> o.getStatistics().entrySet().stream())
            .forEach(System.out::println);
      }*/
      Thread.sleep(500);
    } while (!Thread.currentThread().isInterrupted() && (statistics.isEmpty() || !test.test(statistics)));
    assertFalse(Thread.currentThread().isInterrupted());
    assertTrue(test.test(statistics));
  }

}
