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
package org.terracotta.management.entity.sample.client;

import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionException;
import org.terracotta.connection.ConnectionFactory;
import org.terracotta.connection.ConnectionPropertyNames;
import org.terracotta.connection.entity.EntityRef;
import org.terracotta.exception.EntityNotFoundException;
import org.terracotta.exception.EntityNotProvidedException;
import org.terracotta.exception.PermanentEntityException;
import org.terracotta.management.entity.sample.Cache;
import org.terracotta.management.entity.sample.client.management.Management;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.context.ContextContainer;
import org.terracotta.management.registry.CapabilityManagementSupport;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Mathieu Carbou
 */
public class CacheFactory implements Closeable {

  private final URI uri;
  private final String path;
  private final ConcurrentMap<String, ClientCache> caches = new ConcurrentHashMap<>();
  private final Management management;

  private Connection connection;
  private CacheEntityFactory cacheEntityFactory;

  public CacheFactory(String instanceId, URI u, String path) {
    this.uri = u;
    this.path = path;
    this.management = new Management(instanceId, new ContextContainer("appName", path));
  }

  public CapabilityManagementSupport getManagementRegistry() {
    return management.getManagementRegistry();
  }

  public Context getRootContext() {return management.getRootContext();}

  public void init() throws ConnectionException {
    init(null);
  }

  public void init(String uuid) throws ConnectionException {
    // connects to server
    Properties properties = new Properties();
    properties.setProperty(ConnectionPropertyNames.CONNECTION_NAME, path);
    properties.setProperty(ConnectionPropertyNames.CONNECTION_TIMEOUT, "20000");
    if(uuid != null) {
      properties.setProperty(ConnectionPropertyNames.CONNECTION_UUID, uuid);
    }
    this.connection = ConnectionFactory.connect(uri, properties);
    this.cacheEntityFactory = new CacheEntityFactory(connection);
    this.management.init(connection);
  }

  public Cache getCache(String name) {
    return caches.computeIfAbsent(name, s -> {
      String entityName = path + "/" + name;
      CacheEntity cacheEntity = cacheEntityFactory.retrieveOrCreate(entityName, entityName);
      ClientCache clientCache = new ClientCache(name, cacheEntity);

      // add a cache to the local registry for stat and calls and send a notification
      management.clientCacheCreated(clientCache);

      return clientCache;
    });
  }

  public void destroyCache(String name) {
    ClientCache clientCache = caches.remove(name);
    if (clientCache != null) {
      clientCache.close();
    }
    try {
      EntityRef<CacheEntity, String, Object> ref = connection.getEntityRef(CacheEntity.class, 1, path + "/" + name);
      ref.destroy();
    } catch (EntityNotProvidedException | PermanentEntityException | EntityNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public Connection getConnection() {
    return connection;
  }

  @Override
  public void close() {
    management.close();
    try {
      connection.close();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

}
