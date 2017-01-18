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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.Rule;
import org.junit.rules.Timeout;
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionFactory;
import org.terracotta.connection.ConnectionPropertyNames;
import org.terracotta.management.entity.sample.Cache;
import org.terracotta.management.entity.sample.client.CacheFactory;
import org.terracotta.management.entity.tms.TmsAgentConfig;
import org.terracotta.management.entity.tms.client.TmsAgentEntity;
import org.terracotta.management.entity.tms.client.TmsAgentEntityFactory;
import org.terracotta.management.entity.tms.client.SingleStripeTmsAgentService;
import org.terracotta.management.entity.tms.client.TmsAgentService;
import org.terracotta.management.model.capabilities.context.CapabilityContext;
import org.terracotta.management.model.cluster.ServerEntity;
import org.terracotta.management.model.stats.ContextualStatistics;
import org.terracotta.testing.rules.Cluster;

import java.io.File;
import java.io.IOException;
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

  private final ObjectMapper mapper = new ObjectMapper().configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);

  private Connection managementConnection;
  protected Cluster cluster;

  protected final List<CacheFactory> webappNodes = new ArrayList<>();
  protected final Map<String, List<Cache>> caches = new HashMap<>();
  protected TmsAgentService tmsAgentService;

  @Rule
  public Timeout timeout = Timeout.seconds(60);

  protected final void commonSetUp(Cluster cluster) throws Exception {
    this.cluster = cluster;

    mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
    mapper.addMixIn(CapabilityContext.class, CapabilityContextMixin.class);

    connectManagementClient(cluster.getConnectionURI());

    addWebappNode(cluster.getConnectionURI().resolve("/pet-clinic"));
    addWebappNode(cluster.getConnectionURI().resolve("/pet-clinic"));

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

  protected void addWebappNode(URI uri) throws Exception {
    CacheFactory cacheFactory = new CacheFactory(uri);
    cacheFactory.init();
    webappNodes.add(cacheFactory);
  }

  public static abstract class CapabilityContextMixin {
    @JsonIgnore
    public abstract Collection<String> getRequiredAttributeNames();

    @JsonIgnore
    public abstract Collection<CapabilityContext.Attribute> getRequiredAttributes();
  }

  private void connectManagementClient(URI uri) throws Exception {
    // connects to server
    Properties properties = new Properties();
    properties.setProperty(ConnectionPropertyNames.CONNECTION_NAME, getClass().getSimpleName());
    properties.setProperty(ConnectionPropertyNames.CONNECTION_TIMEOUT, "5000");
    this.managementConnection = ConnectionFactory.connect(uri, properties);

    // create a tms entity
    TmsAgentEntityFactory tmsAgentEntityFactory = new TmsAgentEntityFactory(managementConnection, getClass().getSimpleName());
    TmsAgentEntity tmsAgentEntity = tmsAgentEntityFactory.retrieveOrCreate(new TmsAgentConfig()
        .setMaximumUnreadMessages(1024 * 1024));
    this.tmsAgentService = new SingleStripeTmsAgentService(tmsAgentEntity);
    this.tmsAgentService.setOperationTimeout(60, TimeUnit.SECONDS);
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

  protected String removeRandomValues(String currentTopo) {
    // removes all random values
    return currentTopo
        .replaceAll("\"(hostName)\":\"[^\"]*\"", "\"$1\":\"<hostname>\"")
        .replaceAll("\"hostAddress\":[^,]*", "\"hostAddress\":\"127\\.0\\.0\\.1\"")
        .replaceAll("\"bindPort\":[0-9]+", "\"bindPort\":0")
        .replaceAll("\"groupPort\":[0-9]+", "\"groupPort\":0")
        .replaceAll("\"port\":[0-9]+", "\"port\":0")
        .replaceAll("\"activateTime\":[0-9]+", "\"activateTime\":0")
        .replaceAll("\"time\":[0-9]+", "\"time\":0")
        .replaceAll("\"startTime\":[0-9]+", "\"startTime\":0")
        .replaceAll("\"upTimeSec\":[0-9]+", "\"upTimeSec\":0")
        .replaceAll("\"id\":\"[0-9]+@[^:]*:([^:]*):[^\"]*\",\"pid\":[0-9]+", "\"id\":\"0@127.0.0.1:$1:<uuid>\",\"pid\":0")
        .replaceAll("\"buildId\":\"[^\"]*\"", "\"buildId\":\"Build ID\"")
        .replaceAll("\"version\":\"[^\"]*\"", "\"version\":\"<version>\"")
        .replaceAll("\"clientId\":\"[0-9]+@[^:]*:([^:]*):[^\"]*\"", "\"clientId\":\"0@127.0.0.1:$1:<uuid>\"")
        .replaceAll("\"logicalConnectionUid\":\"[^\"]*\"", "\"logicalConnectionUid\":\"<uuid>\"")
        .replaceAll("\"id\":\"[^\"]*\",\"logicalConnectionUid\":\"[^\"]*\"", "\"id\":\"<uuid>:SINGLE:testServer0:127.0.0.1:0\",\"logicalConnectionUid\":\"<uuid>\"")
        .replaceAll("\"vmId\":\"[^\"]*\"", "\"vmId\":\"0@127.0.0.1\"")
        .replaceAll("testServer1", "testServer0");
  }

  protected void triggerServerStatComputation() throws Exception {
    // trigger stats computation and wait for all stats to have been computed at least once
    tmsAgentService.readTopology().serverStream().forEach(server -> {
      ServerEntity serverEntity = server
          .serverEntityStream()
          .filter(e -> e.getType().equals(TmsAgentConfig.ENTITY_TYPE))
          .findFirst()
          .get();

      try {
        tmsAgentService.startStatisticCollector(
            serverEntity.getContext(),
            1, TimeUnit.SECONDS
        ).waitForReturn();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

}
