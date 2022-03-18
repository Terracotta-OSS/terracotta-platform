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
package org.terracotta.management.integration.tests;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.Rule;
import org.junit.rules.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionFactory;
import org.terracotta.connection.ConnectionPropertyNames;
import org.terracotta.management.entity.nms.NmsConfig;
import org.terracotta.management.entity.nms.client.DefaultNmsService;
import org.terracotta.management.entity.nms.client.NmsEntity;
import org.terracotta.management.entity.nms.client.NmsEntityFactory;
import org.terracotta.management.entity.nms.client.NmsService;
import org.terracotta.management.entity.sample.Cache;
import org.terracotta.management.entity.sample.client.CacheFactory;
import org.terracotta.management.model.capabilities.context.CapabilityContext;
import org.terracotta.management.model.cluster.AbstractManageableNode;
import org.terracotta.management.model.cluster.ServerEntity;
import org.terracotta.management.model.notification.ContextualNotification;
import org.terracotta.management.model.stats.ContextualStatistics;
import org.terracotta.statistics.ConstantValueStatistic;
import org.terracotta.statistics.Sample;
import org.terracotta.statistics.StatisticType;
import org.terracotta.statistics.registry.Statistic;
import org.terracotta.testing.rules.Cluster;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Mathieu Carbou
 */
public abstract class AbstractTest {

  protected Logger logger = LoggerFactory.getLogger(getClass());

  private final ObjectMapper mapper = new ObjectMapper().configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);

  private Connection managementConnection;
  protected Cluster cluster;

  protected final List<CacheFactory> webappNodes = new ArrayList<>();
  protected final Map<String, List<Cache>> caches = new HashMap<>();
  protected NmsService nmsService;

  @Rule
  public Timeout timeout = Timeout.seconds(120);

  protected final void commonSetUp(Cluster cluster) throws Exception {
    this.cluster = cluster;

    mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
    mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    mapper.addMixIn(CapabilityContext.class, CapabilityContextMixin.class);
    mapper.addMixIn(Statistic.class, StatisticMixin.class);
    mapper.addMixIn(ContextualStatistics.class, ContextualStatisticsMixin.class);
    mapper.addMixIn(ConstantValueStatistic.class, ConstantValueStatisticMixin.class);

    connectManagementClient(cluster.getConnectionURI());

    addWebappNode(cluster.getConnectionURI(), "pet-clinic");
    addWebappNode(cluster.getConnectionURI(), "pet-clinic");

    getCaches("pets");
    getCaches("clients");
  }

  protected final void commonTearDown() throws Exception {
    closeNodes();
    if (managementConnection != null) {
      managementConnection.close();
    }
    if (cluster != null) {
      cluster.getClusterControl().terminateAllServers();
    }
  }

  protected JsonNode readJson(String file) {
    try {
      return mapper.readTree(new File(AbstractTest.class.getResource("/" + file).toURI()));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected JsonNode readJsonStr(String json) {
    try {
      return mapper.readTree(json);
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

  protected void destroyCaches(String name) {
    caches.remove(name);
    webappNodes.forEach(cacheFactory -> cacheFactory.destroyCache(name));
  }

  protected void addWebappNode(URI uri, String path) throws Exception {
    CacheFactory cacheFactory = new CacheFactory(uri, path);
    cacheFactory.init();
    webappNodes.add(cacheFactory);
  }

  private void connectManagementClient(URI uri) throws Exception {
    // connects to server
    Properties properties = new Properties();
    properties.setProperty(ConnectionPropertyNames.CONNECTION_NAME, getClass().getSimpleName());
    properties.setProperty(ConnectionPropertyNames.CONNECTION_TIMEOUT, "20000");
    this.managementConnection = ConnectionFactory.connect(uri, properties);

    // create a NMS Entity
    NmsEntityFactory nmsEntityFactory = new NmsEntityFactory(managementConnection, getClass().getSimpleName());
    NmsEntity nmsEntity = nmsEntityFactory.retrieveOrCreate(new NmsConfig());
    this.nmsService = new DefaultNmsService(nmsEntity);
    this.nmsService.setOperationTimeout(60, TimeUnit.SECONDS);
  }

  protected void queryAllRemoteStatsUntil(Predicate<List<? extends ContextualStatistics>> test) throws InterruptedException {
    List<? extends ContextualStatistics> statistics;
    do {
      statistics = nmsService.readMessages()
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

  protected JsonNode removeRandomValues(JsonNode currentTopo) {
    return readJsonStr(removeRandomValues(currentTopo.toString()));
  }

  protected String removeRandomValues(String currentTopo) {
    // removes all random values
    return currentTopo
        .replaceAll("\"(hostName)\":\"[^\"]*\"", "\"$1\":\"<hostname>\"")
        .replaceAll("\"hostAddress\":[^,]*", "\"hostAddress\":\"127\\.0\\.0\\.1\"")
        .replaceAll("\"bindPort\":[0-9]+", "\"bindPort\":0")
        .replaceAll("\"groupPort\":[0-9]+", "\"groupPort\":0")
        .replaceAll("\"port\":[0-9]+", "\"port\":0")
        .replaceAll("\"activateTime\":[0-9]+", "\"activateTime\":0")
        .replaceAll("\"availableAtTime\":[0-9]+", "\"availableAtTime\":0")
        .replaceAll("\"OffHeapResource:AllocatedMemory\":[0-9]+", "\"OffHeapResource:AllocatedMemory\":0")
        .replaceAll("\"time\":[0-9]+", "\"time\":0")
        .replaceAll("\"startTime\":[0-9]+", "\"startTime\":0")
        .replaceAll("\"timestamp\":[0-9]+", "\"timestamp\":0")
        .replaceAll("\"upTimeSec\":[0-9]+", "\"upTimeSec\":0")
        .replaceAll("\"id\":\"[0-9]+@[^:]*:([^:]*):[^\"]*\",\"pid\":[0-9]+", "\"id\":\"0@127.0.0.1:$1:<uuid>\",\"pid\":0")
        .replaceAll("\"alias\":\"[0-9]+@[^:]*:([^:]*):[^\"]*\",", "\"alias\":\"0@127.0.0.1:$1:<uuid>\",")
        .replaceAll("\"buildId\":\"[^\"]*\"", "\"buildId\":\"Build ID\"")
        .replaceAll("\"version\":\"[^\"]*\"", "\"version\":\"<version>\"")
        .replaceAll("\"clientId\":\"[0-9]+@[^:]*:([^:]*):[^\"]*\"", "\"clientId\":\"0@127.0.0.1:$1:<uuid>\"")
        .replaceAll("\"logicalConnectionUid\":\"[^\"]*\"", "\"logicalConnectionUid\":\"<uuid>\"")
        .replaceAll("\"id\":\"[^\"]+:([\\w\\[\\]]+):[^\"]+:[^\"]+:[^\"]+\",\"logicalConnectionUid\":\"[^\"]*\"", "\"id\":\"<uuid>:$1:testServer0:127.0.0.1:0\",\"logicalConnectionUid\":\"<uuid>\"")
        .replaceAll("\"vmId\":\"[^\"]*\"", "\"vmId\":\"0@127.0.0.1\"")
        .replaceAll("-2", "")
        .replaceAll("testServer1", "testServer0")
        .replaceAll("\"(clientReportedAddress)\":\"[^\"]*\"", "\"$1\":\"<$1>\"");
  }

  protected void triggerServerStatComputation() throws Exception {
    triggerServerStatComputation(1, TimeUnit.SECONDS);
  }

  protected void triggerServerStatComputation(long interval, TimeUnit unit) throws Exception {
    triggerServerStatComputation(nmsService, getClass().getSimpleName(), interval, unit);
  }

  @SuppressWarnings("rawtypes")
  protected void triggerServerStatComputation(NmsService nmsService, String entityName, long interval, TimeUnit unit) throws Exception {
    logger.info("triggerServerStatComputation({}, {}, {})", entityName, interval, unit);
    // trigger stats computation and wait for all stats to have been computed at least once
    org.terracotta.management.model.cluster.Cluster topology = nmsService.readTopology();
    logger.trace("topology:\n{}", toJson(topology.toMap()));
    CompletableFuture.allOf(topology
        .serverEntityStream()
        .filter(e -> e.getType().equals(NmsConfig.ENTITY_TYPE) && e.getName().equals(entityName))
        .filter(AbstractManageableNode::isManageable)
        .map(ServerEntity::getContext)
        .map(context -> {
          try {
            return nmsService.startStatisticCollector(context, interval, unit).asCompletionStage();
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        })
        .map(CompletionStage::toCompletableFuture)
        .toArray(CompletableFuture[]::new)).get();
  }

  protected void triggerClientStatComputation() throws Exception {
    triggerClientStatComputation(1, TimeUnit.SECONDS);
  }

  @SuppressWarnings("rawtypes")
  protected void triggerClientStatComputation(long interval, TimeUnit unit) throws Exception {
    logger.info("triggerClientStatComputation({}, {})", interval, unit);
    // trigger stats computation and wait for all stats to have been computed at least once
    org.terracotta.management.model.cluster.Cluster topology = nmsService.readTopology();
    logger.trace("topology:\n{}", toJson(topology.toMap()));
    CompletableFuture.allOf(topology
        .clientStream()
        .filter(client -> client.getName().equals("pet-clinic"))
        .filter(AbstractManageableNode::isManageable)
        .map(client -> client.getContext().with("appName", "pet-clinic"))
        .map(context -> {
          try {
            return nmsService.startStatisticCollector(context, interval, unit).asCompletionStage();
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        })
        .map(CompletionStage::toCompletableFuture)
        .toArray(CompletableFuture[]::new)).get();
  }

  protected List<ContextualNotification> waitForAllNotifications(String... notificationTypes) throws InterruptedException {
    List<String> waitingFor = new ArrayList<>(Arrays.asList(notificationTypes));
    try {
      return nmsService.waitForMessage(message -> {
        if (message.getType().equals("NOTIFICATION")) {
          for (ContextualNotification notification : message.unwrap(ContextualNotification.class)) {
            waitingFor.remove(notification.getType());
          }
        }
        return waitingFor.isEmpty();
      }).stream()
          .filter(message -> message.getType().equals("NOTIFICATION"))
          .flatMap(message -> message.unwrap(ContextualNotification.class).stream())
          .collect(Collectors.toList());
    } catch (InterruptedException e) {
      System.err.println("STILL WAITING FOR: " + waitingFor);
      throw e;
    }
  }

  public static abstract class CapabilityContextMixin {
    @JsonIgnore
    public abstract Collection<String> getRequiredAttributeNames();

    @JsonIgnore
    public abstract Collection<CapabilityContext.Attribute> getRequiredAttributes();
  }

  public static abstract class StatisticMixin<T extends Serializable> {
    @JsonIgnore
    public abstract boolean isEmpty();

    @JsonIgnore
    public abstract Optional<T> getLatestSampleValue();

    @JsonIgnore
    public abstract Optional<Sample<T>> getLatestSample();
  }

  public static abstract class ContextualStatisticsMixin {
    @JsonIgnore
    public abstract int size();

    @JsonIgnore
    public abstract boolean isEmpty();

    @JsonIgnore
    public abstract Map<String, ? extends Serializable> getLatestSampleValues();

    @JsonIgnore
    public abstract Map<String, Sample<? extends Serializable>> getLatestSamples();
  }

  public static abstract class ConstantValueStatisticMixin<T> {
    @JsonProperty
    public abstract T value();

    @JsonProperty
    public abstract StatisticType type();
  }

}
