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
import org.junit.Rule;
import org.junit.rules.Timeout;
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionFactory;
import org.terracotta.connection.ConnectionPropertyNames;
import org.terracotta.management.entity.nms.agent.client.NmsAgentEntityClientService;
import org.terracotta.management.entity.nms.agent.server.NmsAgentEntityServerService;
import org.terracotta.management.entity.nms.NmsConfig;
import org.terracotta.management.entity.nms.client.DefaultNmsService;
import org.terracotta.management.entity.nms.client.NmsEntity;
import org.terracotta.management.entity.nms.client.NmsEntityClientService;
import org.terracotta.management.entity.nms.client.NmsEntityFactory;
import org.terracotta.management.entity.nms.client.NmsService;
import org.terracotta.management.entity.nms.server.NmsEntityServerService;
import org.terracotta.management.entity.sample.client.CacheEntityClientService;
import org.terracotta.management.entity.sample.client.CacheFactory;
import org.terracotta.management.entity.sample.server.CacheEntityServerService;
import org.terracotta.management.model.capabilities.context.CapabilityContext;
import org.terracotta.offheapresource.OffHeapResourcesProvider;
import org.terracotta.offheapresource.config.MemoryUnit;
import org.terracotta.offheapresource.config.OffheapResourcesType;
import org.terracotta.offheapresource.config.ResourceType;
import org.terracotta.passthrough.PassthroughClusterControl;
import org.terracotta.passthrough.PassthroughTestHelpers;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Mathieu Carbou
 */
public abstract class AbstractTest {

  private final ObjectMapper mapper = new ObjectMapper();
  protected final PassthroughClusterControl stripeControl;

  private Connection managementConnection;

  protected final List<CacheFactory> webappNodes = new ArrayList<>();
  protected final Map<String, List<Cache>> caches = new HashMap<>();
  protected NmsService nmsService;

  @Rule
  public Timeout timeout = Timeout.seconds(60);

  protected AbstractTest() {
    this(0);
  }

  protected AbstractTest(int nPassives) {
    mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
    mapper.addMixIn(CapabilityContext.class, CapabilityContextMixin.class);

    stripeControl = PassthroughTestHelpers.createMultiServerStripe("stripe-1", nPassives + 1, server -> {
      server.registerClientEntityService(new CacheEntityClientService());
      server.registerServerEntityService(new CacheEntityServerService());

      server.registerClientEntityService(new NmsAgentEntityClientService());
      server.registerServerEntityService(new NmsAgentEntityServerService());

      server.registerClientEntityService(new NmsEntityClientService());
      server.registerServerEntityService(new NmsEntityServerService());

      OffheapResourcesType resources = new OffheapResourcesType();
      ResourceType resource = new ResourceType();
      resource.setName("primary-server-resource");
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
    CacheFactory cacheFactory = new CacheFactory(URI.create("passthrough://stripe-1:9510/pet-clinic"));
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
    properties.setProperty(ConnectionPropertyNames.CONNECTION_TIMEOUT, "5000");
    this.managementConnection = ConnectionFactory.connect(URI.create("passthrough://stripe-1:9510/"), properties);

    // create a NMS Entity
    NmsEntityFactory nmsEntityFactory = new NmsEntityFactory(managementConnection, getClass().getSimpleName());
    NmsEntity nmsEntity = nmsEntityFactory.retrieveOrCreate(new NmsConfig());
    this.nmsService = new DefaultNmsService(nmsEntity);
    this.nmsService.setOperationTimeout(10, TimeUnit.SECONDS);
  }

}
